<?php

namespace App\Controller;

use App\Entity\User;
use App\Form\RegistrationFormType;
use App\Repository\UserRepository;
use App\Service\BlobStorageService;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\Form\FormError;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\PasswordHasher\Hasher\UserPasswordHasherInterface;
use Symfony\Component\Routing\Attribute\Route;

class RegistrationController extends AbstractController
{
    #[Route('/signup', name: 'app_signup', methods: ['GET', 'POST'])]
    public function signup(
        Request $request,
        EntityManagerInterface $entityManager,
        UserRepository $userRepository,
        UserPasswordHasherInterface $passwordHasher,
        BlobStorageService $blobStorageService,
    ): Response {
        if ($this->getUser()) {
            return $this->redirectToRoute('app_profile');
        }

        $user = new User();
        $form = $this->createForm(RegistrationFormType::class, $user);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            if ($userRepository->emailExists($user->getEmail())) {
                $form->get('email')->addError(new FormError('An account with this email already exists.'));
            } else {
                $imageFile = $form->get('profileImageFile')->getData();

                if (null !== $imageFile) {
                    try {
                        $user->setProfileImg($blobStorageService->upload($imageFile));
                    } catch (\RuntimeException $exception) {
                        $form->get('profileImageFile')->addError(new FormError($exception->getMessage()));
                    }
                }
            }

            if ($form->isValid()) {
                $user->setBalance(0.0);
                $user->setIsAdmin(false);
                $user->setPassword($passwordHasher->hashPassword($user, (string) $form->get('plainPassword')->getData()));

                $entityManager->persist($user);
                $entityManager->flush();

                $this->addFlash('success', 'Account created successfully. You can now sign in or use Face ID enrollment from your profile.');

                return $this->redirectToRoute('app_login');
            }
        }

        return $this->render('registration/signup.html.twig', [
            'registration_form' => $form,
            'avatar_uploads_enabled' => $blobStorageService->isConfigured(),
        ]);
    }
}
