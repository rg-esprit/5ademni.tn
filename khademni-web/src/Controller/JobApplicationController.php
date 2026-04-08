<?php

namespace App\Controller;

use App\Entity\Job;
use App\Entity\JobApplication;
use App\Entity\User;
use App\Form\JobApplicationType;
use App\Repository\JobApplicationRepository;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\File\Exception\FileException;
use Symfony\Component\HttpFoundation\File\UploadedFile;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;
use Symfony\Component\String\Slugger\SluggerInterface;

class JobApplicationController extends AbstractController
{
    #[Route('/jobs/{id}/apply', name: 'app_job_apply', methods: ['GET', 'POST'])]
    public function apply(Request $request, Job $job, EntityManagerInterface $entityManager, JobApplicationRepository $applicationRepository, SluggerInterface $slugger): Response
    {
        $user = $this->getUser();
        if (!$user instanceof User) {
            return $this->redirectToRoute('app_login');
        }

        // Check if user already applied
        $existingApplication = $applicationRepository->findByJobAndUser($job, $user);
        if ($existingApplication) {
            $this->addFlash('info', 'You have already applied to this job.');
            return $this->redirectToRoute('app_job_show', ['id' => $job->getId()]);
        }

        // Users shouldn't apply to their own jobs
        if ($job->getUser() === $user) {
            $this->addFlash('warning', 'You cannot apply to your own job posting.');
            return $this->redirectToRoute('app_job_show', ['id' => $job->getId()]);
        }

        $application = new JobApplication();
        $application->setJob($job);
        $application->setUser($user);

        $form = $this->createForm(JobApplicationType::class, $application);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            /** @var UploadedFile $cvFile */
            $cvFile = $form->get('cvFile')->getData();

            if ($cvFile) {
                $originalFilename = pathinfo($cvFile->getClientOriginalName(), PATHINFO_FILENAME);
                $safeFilename = $slugger->slug($originalFilename);
                $newFilename = $safeFilename.'-'.uniqid().'.'.$cvFile->guessExtension();

                try {
                    // Make sure 'cv_directory' is configured in services.yaml (e.g. public/uploads/cv)
                    $cvDirectory = $this->getParameter('kernel.project_dir') . '/public/uploads/cv';
                    if (!file_exists($cvDirectory)) {
                        mkdir($cvDirectory, 0777, true);
                    }
                    
                    $cvFile->move($cvDirectory, $newFilename);
                    $application->setCvPath('uploads/cv/' . $newFilename);
                } catch (FileException $e) {
                    $this->addFlash('error', 'There was an error uploading your CV.');
                    return $this->redirectToRoute('app_job_apply', ['id' => $job->getId()]);
                }
            }

            $entityManager->persist($application);
            $entityManager->flush();

            $this->addFlash('success', 'Your application has been submitted successfully.');
            return $this->redirectToRoute('app_job_applications_my');
        }

        return $this->render('job_application/apply.html.twig', [
            'job' => $job,
            'form' => $form->createView(),
        ]);
    }

    #[Route('/applications/{id<\d+>}', name: 'app_job_application_show', methods: ['GET'])]
    public function show(JobApplication $application): Response
    {
        $user = $this->getUser();
        if (!$user instanceof User || $application->getUser() !== $user) {
            throw $this->createAccessDeniedException('You are not allowed to view this application.');
        }

        return $this->render('job_application/show.html.twig', [
            'application' => $application,
        ]);
    }

    #[Route('/applications/{id<\d+>}/edit', name: 'app_job_application_edit', methods: ['GET', 'POST'])]
    public function edit(Request $request, JobApplication $application, EntityManagerInterface $entityManager, SluggerInterface $slugger): Response
    {
        $user = $this->getUser();
        if (!$user instanceof User || $application->getUser() !== $user) {
            throw $this->createAccessDeniedException('You are not allowed to edit this application.');
        }

        if ($application->getStatus() !== 'PENDING') {
            $this->addFlash('warning', 'Only pending applications can be edited.');
            return $this->redirectToRoute('app_job_application_show', ['id' => $application->getId()]);
        }

        $form = $this->createForm(JobApplicationType::class, $application);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            /** @var UploadedFile $cvFile */
            $cvFile = $form->get('cvFile')->getData();

            if ($cvFile) {
                // Remove old CV if exists
                if ($application->getCvPath()) {
                    $oldCvPath = $this->getParameter('kernel.project_dir') . '/public/' . $application->getCvPath();
                    if (file_exists($oldCvPath)) {
                        unlink($oldCvPath);
                    }
                }

                $originalFilename = pathinfo($cvFile->getClientOriginalName(), PATHINFO_FILENAME);
                $safeFilename = $slugger->slug($originalFilename);
                $newFilename = $safeFilename.'-'.uniqid().'.'.$cvFile->guessExtension();

                try {
                    $cvDirectory = $this->getParameter('kernel.project_dir') . '/public/uploads/cv';
                    $cvFile->move($cvDirectory, $newFilename);
                    $application->setCvPath('uploads/cv/' . $newFilename);
                } catch (FileException $e) {
                    $this->addFlash('error', 'There was an error uploading your CV.');
                }
            }

            $entityManager->flush();
            $this->addFlash('success', 'Your application has been updated successfully.');
            return $this->redirectToRoute('app_job_applications_my');
        }

        return $this->render('job_application/edit.html.twig', [
            'application' => $application,
            'form' => $form->createView(),
        ]);
    }

    #[Route('/applications/{id<\d+>}/delete', name: 'app_job_application_delete', methods: ['POST'])]
    public function delete(Request $request, JobApplication $application, EntityManagerInterface $entityManager): Response
    {
        $user = $this->getUser();
        if (!$user instanceof User || $application->getUser() !== $user) {
            throw $this->createAccessDeniedException('You are not allowed to delete this application.');
        }

        if ($application->getStatus() !== 'PENDING') {
            $this->addFlash('warning', 'Only pending applications can be deleted.');
            return $this->redirectToRoute('app_job_applications_my');
        }

        if ($this->isCsrfTokenValid('delete_application' . $application->getId(), $request->request->get('_token'))) {
            // Remove CV file if exists
            if ($application->getCvPath()) {
                $cvPath = $this->getParameter('kernel.project_dir') . '/public/' . $application->getCvPath();
                if (file_exists($cvPath)) {
                    unlink($cvPath);
                }
            }

            $entityManager->remove($application);
            $entityManager->flush();
            $this->addFlash('success', 'Your application has been deleted.');
        }

        return $this->redirectToRoute('app_job_applications_my');
    }
}
