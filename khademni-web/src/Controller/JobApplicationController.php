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

}
