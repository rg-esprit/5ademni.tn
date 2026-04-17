<?php

namespace App\Service;

use App\Entity\Job;
use App\Entity\JobApplication;
use App\Repository\WorkLogRepository;
use App\Service\NotificationService;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Component\Routing\Generator\UrlGeneratorInterface;

class ApplicationManager
{
    public function __construct(
        private EntityManagerInterface $entityManager,
        private WorkLogRepository $workLogRepository,
        private NotificationService $notificationService,
        private UrlGeneratorInterface $urlGenerator
    ) {}

    /**
     * Updates an application status and handles automatic workflow logic.
     */
    public function updateStatus(JobApplication $application, string $newStatus): void
    {
        $oldStatus = $application->getStatus();
        $application->setStatus($newStatus);

        $job = $application->getJob();

        if ($newStatus === JobApplication::STATUS_ACCEPTED) {
            // When an application is accepted, the job moves to IN_PROGRESS
            $job->setStatus('IN_PROGRESS');

            // Automatically reject all other pending applications for this job
            foreach ($job->getApplications() as $otherApp) {
                if ($otherApp !== $application && $otherApp->getStatus() === JobApplication::STATUS_PENDING) {
                    $otherApp->setStatus(JobApplication::STATUS_REJECTED);
                }
            }

            // Notify the freelancer their application was accepted
            $freelancer = $application->getUser();
            $this->notificationService->send(
                $freelancer,
                sprintf('🎉 Congratulations! Your application for "%s" at %s has been accepted!', $job->getTitle(), $job->getCompany()),
                'success',
                $this->urlGenerator->generate('app_job_show', ['id' => $job->getId()])
            );
        }

        if ($newStatus === JobApplication::STATUS_REJECTED) {
            // Notify the freelancer their application was rejected
            $freelancer = $application->getUser();
            $this->notificationService->send(
                $freelancer,
                sprintf('Your application for "%s" at %s was not selected this time.', $job->getTitle(), $job->getCompany()),
                'warning',
                $this->urlGenerator->generate('app_job_show', ['id' => $job->getId()])
            );
        }

        if ($newStatus === JobApplication::STATUS_COMPLETED) {
            // When the application is completed, the job is CLOSED
            $job->setStatus('CLOSED');
        }

        $this->entityManager->flush();
    }

    /**
     * Synchronizes the application and job status based on total work progress.
     */
    public function syncCompletionStatus(Job $job): void
    {
        $application = $this->entityManager->getRepository(JobApplication::class)->findAcceptedForJob($job);
        if (!$application) {
            return;
        }

        $freelancer = $application->getUser();
        $totalProgress = $this->workLogRepository->getTotalProgressForJobAndFreelancer($job, $freelancer);

        // Auto-move to IN_PROGRESS if there is research/work logged
        if ($totalProgress > 0 && $application->getStatus() === JobApplication::STATUS_ACCEPTED) {
            $application->setStatus(JobApplication::STATUS_IN_PROGRESS);
        }

        // Auto-move to COMPLETED if 100% reached
        if ($totalProgress >= 100 && $application->getStatus() !== JobApplication::STATUS_COMPLETED) {
            $this->updateStatus($application, JobApplication::STATUS_COMPLETED);
        }

        $this->entityManager->flush();
    }

    /**
     * Checks if a user can apply for a job.
     */
    public function canApply(JobApplication $application): bool
    {
        $job = $application->getJob();
        
        // Prevent new applications if job is not OPEN
        if ($job->getStatus() !== 'OPEN') {
            return false;
        }

        return true;
    }
}
