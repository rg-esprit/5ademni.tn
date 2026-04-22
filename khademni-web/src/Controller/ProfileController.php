<?php

namespace App\Controller;

use App\Entity\User;
use App\Form\ProfileFormType;
use App\Repository\UserRepository;
use App\Service\BlobStorageService;
use App\Service\FaceBackendService;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\Form\FormError;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;
use Symfony\Component\Security\Core\Authentication\Token\Storage\TokenStorageInterface;
use Symfony\Component\Security\Core\Exception\CsrfTokenNotFoundException;
use Symfony\Component\Security\Csrf\CsrfToken;
use Symfony\Component\Security\Csrf\CsrfTokenManagerInterface;

class ProfileController extends AbstractController
{
    #[Route('/profile', name: 'app_profile', methods: ['GET', 'POST'])]
    public function show(
        Request $request,
        EntityManagerInterface $entityManager,
        UserRepository $userRepository,
        BlobStorageService $blobStorageService,
        CsrfTokenManagerInterface $csrfTokenManager,
    ): Response {
        $user = $this->getUser();

        if (!$user instanceof User) {
            return $this->redirectToRoute('app_login');
        }

        $form = $this->createForm(ProfileFormType::class, $user);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            if ($userRepository->emailExists($user->getEmail(), $user->getId())) {
                $form->get('email')->addError(new FormError('Another account already uses this email address.'));
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
                $entityManager->flush();

                $this->addFlash('success', 'Profile updated successfully.');

                return $this->redirectToRoute('app_profile');
            }
        }

        return $this->render('profile/show.html.twig', [
            'profile_form' => $form,
            'avatar_proxy_enabled' => $blobStorageService->isConfigured() && $user->hasProfileImage(),
            'delete_csrf_token' => $csrfTokenManager->getToken('delete_account')->getValue(),
            'face_enroll_csrf_token' => $csrfTokenManager->getToken('face_enroll')->getValue(),
        ]);
    }

    #[Route('/profile/delete', name: 'app_profile_delete', methods: ['POST'])]
    public function delete(
        Request $request,
        EntityManagerInterface $entityManager,
        TokenStorageInterface $tokenStorage,
    ): Response {
        $user = $this->getUser();

        if (!$user instanceof User) {
            return $this->redirectToRoute('app_login');
        }

        if (!$this->isCsrfTokenValid('delete_account', (string) $request->request->get('_token'))) {
            throw new CsrfTokenNotFoundException('Invalid delete-account token.');
        }

        $entityManager->remove($user);
        $entityManager->flush();

        $tokenStorage->setToken(null);
        $session = $request->getSession();
        if (null !== $session) {
            $session->invalidate();
        }

        return $this->redirectToRoute('app_login');
    }

    #[Route('/me/avatar', name: 'app_my_avatar', methods: ['GET'])]
    public function avatar(BlobStorageService $blobStorageService): Response
    {
        $user = $this->getUser();

        if (!$user instanceof User || !$user->hasProfileImage() || !$blobStorageService->isConfigured()) {
            throw $this->createNotFoundException();
        }

        try {
            $file = $blobStorageService->download($user->getProfileImg());
        } catch (\RuntimeException) {
            throw $this->createNotFoundException();
        }

        $response = new Response($file['content']);
        $response->headers->set('Content-Type', $file['contentType']);
        $response->headers->set('Cache-Control', 'no-store, no-cache, must-revalidate, max-age=0');
        $response->headers->set('Pragma', 'no-cache');
        $response->headers->set('Expires', '0');

        return $response;
    }

    #[Route('/face/enroll', name: 'app_face_enroll', methods: ['POST'])]
    public function faceEnroll(
        Request $request,
        FaceBackendService $faceBackendService,
        EntityManagerInterface $entityManager,
        CsrfTokenManagerInterface $csrfTokenManager,
    ): Response {
        $user = $this->getUser();

        if (!$user instanceof User) {
            return $this->redirectToRoute('app_login');
        }

        if (!$csrfTokenManager->isTokenValid(new CsrfToken('face_enroll', (string) $request->request->get('_token')))) {
            $this->addFlash('error', 'The Face ID form expired. Please try again.');

            return $this->redirectToRoute('app_profile');
        }

        $frames = array_values(array_filter($request->files->all('frames')));

        if ([] === $frames) {
            $this->addFlash('error', 'No Face ID frames were captured. Please use the camera and try again.');

            return $this->redirectToRoute('app_profile');
        }

        $result = $faceBackendService->enroll((int) $user->getId(), $frames);

        if (($result['success'] ?? false) !== true) {
            $this->addFlash('error', $result['detail'] ?? 'Face ID enrollment failed.');

            return $this->redirectToRoute('app_profile');
        }

        $entityManager->refresh($user);

        $this->addFlash('success', 'Face ID updated successfully.');

        return $this->redirectToRoute('app_profile');
    }
}
