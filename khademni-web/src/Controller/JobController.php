<?php

namespace App\Controller;

use App\Entity\Job;
use App\Entity\User;
use App\Form\JobType;
use App\Repository\JobApplicationRepository;
use App\Repository\JobRepository;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

#[Route('/jobs')]
class JobController extends AbstractController
{
    #[Route('/', name: 'app_job_index', methods: ['GET'])]
    public function index(
        Request $request,
        JobRepository $jobRepository,
        JobApplicationRepository $applicationRepository,
    ): Response {
        $view     = (string) $request->query->get('view', 'all');
        $query    = (string) $request->query->get('q', '');
        $category = (string) $request->query->get('category', '');
        $location = (string) $request->query->get('location', '');
        $jobType  = (string) $request->query->get('job_type', '');
        $sort     = (string) $request->query->get('sort', 'newest');
        $minSalary= (int) $request->query->get('min_salary', 0);

        $jobs = $jobRepository->searchJobs($query, $category, $location, $jobType, $sort, $minSalary);

        $user           = $this->getUser();
        $savedJobs      = [];
        $myApplications = [];

        if ($user instanceof User) {
            $savedJobs      = $user->getSavedJobs()->toArray();
            $myApplications = $applicationRepository->findByUser($user);
        }

        return $this->render('job/index.html.twig', [
            'jobs'            => $jobs,
            'saved_jobs'      => $savedJobs,
            'my_applications' => $myApplications,
            'view'            => $view,
            'query'           => $query,
            'category'        => $category,
            'location'        => $location,
            'job_type'        => $jobType,
            'sort'            => $sort,
            'min_salary'      => $minSalary,
        ]);
    }

    #[Route('/saved', name: 'app_job_saved', methods: ['GET'])]
    public function saved(): Response
    {
        return $this->redirectToRoute('app_job_index', ['view' => 'saved']);
    }

    #[Route('/applications', name: 'app_job_applications_my', methods: ['GET'])]
    public function applications(): Response
    {
        return $this->redirectToRoute('app_job_index', ['view' => 'applications']);
    }

    #[Route('/new', name: 'app_job_new', methods: ['GET', 'POST'])]
    public function new(Request $request, EntityManagerInterface $entityManager): Response
    {
        $user = $this->getUser();
        if (!$user instanceof User) {
            return $this->redirectToRoute('app_login');
        }

        $job = new Job();
        $job->setUser($user);

        $form = $this->createForm(JobType::class, $job);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            $entityManager->persist($job);
            $entityManager->flush();

            $this->addFlash('success', 'Job posting created successfully.');

            return $this->redirectToRoute('app_management_jobs_index', [], Response::HTTP_SEE_OTHER);
        }

        return $this->render('job/form.html.twig', [
            'job'     => $job,
            'form'    => $form->createView(),
            'is_edit' => false,
        ]);
    }

    #[Route('/{id<\d+>}', name: 'app_job_show', methods: ['GET'])]
    public function show(Job $job): Response
    {
        return $this->render('job/show.html.twig', ['job' => $job]);
    }

    #[Route('/{id<\d+>}/edit', name: 'app_job_edit', methods: ['GET', 'POST'])]
    public function edit(Request $request, Job $job, EntityManagerInterface $entityManager): Response
    {
        $user = $this->getUser();
        if (!$user instanceof User || $job->getUser() !== $user) {
            throw $this->createAccessDeniedException('You are not allowed to edit this job posting.');
        }

        $form = $this->createForm(JobType::class, $job);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            $entityManager->flush();
            $this->addFlash('success', 'Job posting updated successfully.');
            return $this->redirectToRoute('app_management_jobs_index', [], Response::HTTP_SEE_OTHER);
        }

        return $this->render('job/form.html.twig', [
            'job'     => $job,
            'form'    => $form->createView(),
            'is_edit' => true,
        ]);
    }

    #[Route('/{id<\d+>}/delete', name: 'app_job_delete', methods: ['POST'])]
    public function delete(Request $request, Job $job, EntityManagerInterface $entityManager): Response
    {
        $user = $this->getUser();
        if (!$user instanceof User || $job->getUser() !== $user) {
            throw $this->createAccessDeniedException('You are not allowed to delete this job posting.');
        }

        if ($this->isCsrfTokenValid('delete' . $job->getId(), $request->request->get('_token'))) {
            $entityManager->remove($job);
            $entityManager->flush();
            $this->addFlash('success', 'Job posting deleted successfully.');
        }

        return $this->redirectToRoute('app_management_jobs_index', [], Response::HTTP_SEE_OTHER);
    }

    #[Route('/{id<\d+>}/save', name: 'app_job_save', methods: ['POST'])]
    public function saveJob(Request $request, Job $job, EntityManagerInterface $entityManager): Response
    {
        $user = $this->getUser();
        if (!$user instanceof User) {
            return $this->redirectToRoute('app_login');
        }

        $referer = $request->headers->get('referer', $this->generateUrl('app_job_index'));

        if ($this->isCsrfTokenValid('save_job_' . $job->getId(), $request->request->get('_token'))) {
            if ($user->getSavedJobs()->contains($job)) {
                $user->removeSavedJob($job);
                $this->addFlash('success', 'Job removed from your saved list.');
            } else {
                $user->addSavedJob($job);
                $this->addFlash('success', 'Job saved successfully.');
            }
            $entityManager->flush();
        }

        return $this->redirect($referer);
    }
}
