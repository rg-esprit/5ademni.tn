<?php

namespace App\Controller;

use App\Entity\Job;
use App\Entity\JobApplication;
use App\Entity\User;
use App\Repository\JobApplicationRepository;
use App\Repository\JobRepository;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

#[Route('/management/jobs')]
class JobManagementController extends AbstractController
{
    #[Route('/', name: 'app_management_jobs_index', methods: ['GET'])]
    public function index(JobRepository $jobRepository): Response
    {
        $user = $this->getUser();
        if (!$user instanceof User) {
            return $this->redirectToRoute('app_login');
        }

        $jobs = $jobRepository->findPostedByUser($user);
        $totalSaves = $jobRepository->countTotalSavesForUserJobs($user);

        return $this->render('job_management/index.html.twig', [
            'jobs' => $jobs,
            'total_saves' => $totalSaves,
        ]);
    }

    #[Route('/{id<\d+>}/applications', name: 'app_management_job_applications', methods: ['GET'])]
    public function jobApplications(Job $job, JobApplicationRepository $applicationRepository): Response
    {
        $user = $this->getUser();
        if (!$user instanceof User || $job->getUser() !== $user) {
            throw $this->createAccessDeniedException('You are not allowed to view these applications.');
        }

        $applications = $applicationRepository->findByJob($job);
        
        $acceptedApp = null;
        foreach ($applications as $app) {
            if ($app->getStatus() === 'ACCEPTED') {
                $acceptedApp = $app;
                break;
            }
        }

        $totalProgress = 0;
        if ($acceptedApp) {
            $totalProgress = $this->container->get('App\Repository\WorkLogRepository')->getTotalProgressForJobAndFreelancer($job, $acceptedApp->getUser());
        }

        return $this->render('job_management/applications.html.twig', [
            'job' => $job,
            'applications' => $applications,
            'accepted_app' => $acceptedApp,
            'total_progress' => $totalProgress,
        ]);
    }

    #[Route('/applications/{id<\d+>}/status', name: 'app_management_application_status', methods: ['POST'])]
    public function updateStatus(Request $request, JobApplication $application, EntityManagerInterface $entityManager): Response
    {
        $user = $this->getUser();
        $job = $application->getJob();

        if (!$user instanceof User || $job->getUser() !== $user) {
            throw $this->createAccessDeniedException('You are not allowed to manage this application.');
        }

        $status = $request->request->get('status');
        $validStatuses = ['PENDING', 'ACCEPTED', 'REJECTED', 'INTERVIEWING'];

        if ($this->isCsrfTokenValid('status' . $application->getId(), $request->request->get('_token')) && in_array($status, $validStatuses)) {
            $application->setStatus($status);
            $entityManager->flush();
            $this->addFlash('success', 'Application status updated to ' . $status . '.');
        }

        return $this->redirectToRoute('app_management_job_applications', ['id' => $job->getId()]);
    }
}
