<?php

namespace App\Controller;

use App\Entity\Job;
use App\Entity\User;
use App\Entity\WorkLog;
use App\Repository\JobApplicationRepository;
use App\Repository\WorkLogRepository;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

class WorkLogController extends AbstractController
{
    #[Route('/jobs/{id}/progress', name: 'app_job_progress', methods: ['GET', 'POST'])]
    public function progress(
        Job $job,
        Request $request,
        JobApplicationRepository $applicationRepository,
        WorkLogRepository $workLogRepository,
        EntityManagerInterface $entityManager,
    ): Response {
        $user = $this->getUser();
        if (!$user instanceof User) {
            return $this->redirectToRoute('app_login');
        }

        $isOwner = ($job->getUser() === $user);

        // Find the accepted application for this freelancer (or if owner, any accepted one)
        $application = $applicationRepository->findByJobAndUser($job, $user);
        $isFreelancer = $application && $application->getStatus() === 'ACCEPTED';

        if (!$isOwner && !$isFreelancer) {
            $this->addFlash('error', 'You do not have access to view or log progress for this job.');
            return $this->redirectToRoute('app_job_index');
        }

        // For owner: find the accepted freelancer's application for display
        $acceptedApplication = $isOwner
            ? $applicationRepository->findAcceptedForJob($job)
            : $application;

        $freelancer = $acceptedApplication?->getUser();
        $totalProgress = $freelancer
            ? $workLogRepository->getTotalProgressForJobAndFreelancer($job, $freelancer)
            : 0;

        $logs  = $workLogRepository->findLogsForJob($job);
        $error = null;

        // Only the accepted freelancer can post logs
        if ($isFreelancer && $request->isMethod('POST')) {
            if (!$this->isCsrfTokenValid('work_log_' . $job->getId(), $request->request->get('_token'))) {
                $this->addFlash('error', 'Security token expired. Please try again.');
                return $this->redirectToRoute('app_job_progress', ['id' => $job->getId()]);
            }

            $progressChange = (int) $request->request->get('progress_change', 0);
            $description    = trim((string) $request->request->get('description', ''));

            if ($progressChange < 1 || $progressChange > 100) {
                $error = 'Progress must be between 1 and 100.';
            } elseif ($totalProgress + $progressChange > 100) {
                $error = sprintf(
                    'You can only add up to %d%% more (current: %d%%).',
                    100 - $totalProgress,
                    $totalProgress
                );
            } elseif ('' === $description) {
                $error = 'Please describe what was completed.';
            } else {
                $log = new WorkLog();
                $log->setJob($job)
                    ->setFreelancer($user)
                    ->setProgressChange($progressChange)
                    ->setDescription($description);

                $entityManager->persist($log);
                $entityManager->flush();

                $this->addFlash('success', sprintf('+%d%% progress logged.', $progressChange));
                return $this->redirectToRoute('app_job_progress', ['id' => $job->getId()]);
            }
        }

        return $this->render('work_log/progress.html.twig', [
            'job'             => $job,
            'application'     => $acceptedApplication,
            'logs'            => $logs,
            'total_progress'  => $totalProgress,
            'error'           => $error,
            'is_owner'        => $isOwner,
            'is_freelancer'   => $isFreelancer,
        ]);
    }
}
