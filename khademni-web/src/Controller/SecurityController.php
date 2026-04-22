<?php

namespace App\Controller;

use App\Entity\User;
use App\Form\LoginFormType;
use App\Repository\UserRepository;
use App\Service\FaceBackendService;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\Form\FormFactoryInterface;
use Symfony\Bundle\SecurityBundle\Security;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;
use Symfony\Component\Security\Csrf\CsrfToken;
use Symfony\Component\Security\Csrf\CsrfTokenManagerInterface;
use Symfony\Component\Security\Http\Authentication\AuthenticationUtils;

class SecurityController extends AbstractController
{
    #[Route('/login', name: 'app_login', methods: ['GET', 'POST'])]
    public function login(AuthenticationUtils $authenticationUtils, CsrfTokenManagerInterface $csrfTokenManager, FormFactoryInterface $formFactory): Response
    {
        if ($this->getUser()) {
            return $this->redirectToRoute('app_profile');
        }

        $loginForm = $formFactory->createNamed('', LoginFormType::class, [
            'email' => $authenticationUtils->getLastUsername(),
        ], [
            'action' => $this->generateUrl('app_login'),
        ]);

        return $this->render('security/login.html.twig', [
            'error' => $authenticationUtils->getLastAuthenticationError(),
            'face_login_csrf_token' => $csrfTokenManager->getToken('face_login')->getValue(),
            'login_form' => $loginForm->createView(),
        ]);
    }

    #[Route('/face/login', name: 'app_face_login', methods: ['POST'])]
    public function faceLogin(
        Request $request,
        FaceBackendService $faceBackendService,
        UserRepository $userRepository,
        Security $security,
        CsrfTokenManagerInterface $csrfTokenManager,
    ): Response {
        if (!$csrfTokenManager->isTokenValid(new CsrfToken('face_login', (string) $request->request->get('_token')))) {
            $this->addFlash('error', 'The Face ID form expired. Please try again.');

            return $this->redirectToRoute('app_login');
        }

        $frames = array_values(array_filter($request->files->all('frames')));

        if ([] === $frames) {
            $this->addFlash('error', 'No Face ID frames were captured. Please use the camera and try again.');

            return $this->redirectToRoute('app_login');
        }

        $result = $faceBackendService->login($frames);

        if (($result['success'] ?? false) !== true) {
            $this->addFlash('error', $result['detail'] ?? 'Face ID login failed.');

            return $this->redirectToRoute('app_login');
        }

        $userId = isset($result['user_id']) ? (int) $result['user_id'] : 0;
        $user = $userId > 0 ? $userRepository->find($userId) : null;

        if (!$user instanceof User) {
            $this->addFlash('error', 'The recognized account could not be loaded.');

            return $this->redirectToRoute('app_login');
        }

        $response = $security->login($user, 'form_login', 'main');

        return $response ?? $this->redirectToRoute('app_profile');
    }

    #[Route('/logout', name: 'app_logout', methods: ['GET'])]
    public function logout(): never
    {
        throw new \LogicException('This method is intercepted by Symfony logout.');
    }
}
