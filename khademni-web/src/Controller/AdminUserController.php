<?php

namespace App\Controller;

use App\Entity\User;
use App\Form\AdminUserFormType;
use App\Repository\UserRepository;
use App\Service\BlobStorageService;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\Form\FormError;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\PasswordHasher\Hasher\UserPasswordHasherInterface;
use Symfony\Component\Routing\Attribute\Route;

class AdminUserController extends AbstractController
{
    #[Route('/admin/users', name: 'app_admin_users', methods: ['GET', 'POST'])]
    public function index(
        Request $request,
        UserRepository $userRepository,
        EntityManagerInterface $entityManager,
        UserPasswordHasherInterface $passwordHasher,
        BlobStorageService $blobStorageService,
    ): Response {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');

        $currentUser = $this->getUser();
        $searchQuery = trim((string) $request->query->get('q', ''));
        $editId = $request->query->getInt('edit');
        $editingUser = $editId > 0 ? $userRepository->find($editId) : null;

        if ($editId > 0 && !$editingUser instanceof User) {
            $this->addFlash('error', 'That user could not be loaded.');

            return $this->redirectToRoute('app_admin_users', array_filter(['q' => $searchQuery]));
        }

        $managedUser = $editingUser ?? (new User())->setBalance(0.0);
        $form = $this->createForm(AdminUserFormType::class, $managedUser, [
            'is_edit' => $editingUser instanceof User,
        ]);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            if ($userRepository->emailExists($managedUser->getEmail(), $editingUser?->getId())) {
                $form->get('email')->addError(new FormError('Another account already uses this email address.'));
            }

            $plainPassword = trim((string) $form->get('plainPassword')->getData());

            if (!$editingUser instanceof User && '' === $plainPassword) {
                $form->get('plainPassword')->addError(new FormError('Password is required for a new user.'));
            }

            if ('' !== $plainPassword && strlen($plainPassword) < 6) {
                $form->get('plainPassword')->addError(new FormError('Password must be at least 6 characters long.'));
            }

            if ($editingUser instanceof User && $currentUser instanceof User && $editingUser->getId() === $currentUser->getId() && !$managedUser->isAdmin()) {
                $form->get('isAdmin')->addError(new FormError('You cannot remove your own admin access from the backoffice.'));
            }

            if ($form->isValid()) {
                if ('' !== $plainPassword) {
                    $managedUser->setPassword($passwordHasher->hashPassword($managedUser, $plainPassword));
                }

                if ($form->get('clearFaceEmbedding')->getData()) {
                    $managedUser->setFaceEmbedding(null);
                }

                if (!$editingUser instanceof User) {
                    $entityManager->persist($managedUser);
                }

                $entityManager->flush();

                $this->addFlash('success', $editingUser instanceof User ? 'User updated successfully.' : 'User created successfully.');

                return $this->redirectToRoute('app_admin_users', ['q' => $managedUser->getEmail()]);
            }
        }

        $listedUsers = '' === $searchQuery
            ? $userRepository->findBy([], ['firstName' => 'ASC', 'lastName' => 'ASC', 'id' => 'ASC'])
            : $userRepository->searchAdminUsers($searchQuery);

        $allUsers = $userRepository->findAll();
        $adminCount = 0;
        $faceEnrollmentCount = 0;

        foreach ($allUsers as $user) {
            if ($user->isAdmin()) {
                ++$adminCount;
            }

            if ($user->hasFaceEnrollment()) {
                ++$faceEnrollmentCount;
            }
        }

        return $this->render('admin/users.html.twig', [
            'admin_user_form' => $form,
            'avatar_proxy_enabled' => $currentUser instanceof User && $blobStorageService->isConfigured() && $currentUser->hasProfileImage(),
            'editing_user' => $editingUser,
            'listed_users' => $listedUsers,
            'search_query' => $searchQuery,
            'user_stats' => [
                'total' => count($allUsers),
                'admins' => $adminCount,
                'face_enrolled' => $faceEnrollmentCount,
            ],
        ]);
    }

    #[Route('/admin/users/{id}/delete', name: 'app_admin_users_delete', methods: ['POST'])]
    public function delete(
        User $managedUser,
        Request $request,
        UserRepository $userRepository,
        EntityManagerInterface $entityManager,
    ): Response {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');

        if (!$this->isCsrfTokenValid('delete_admin_user_'.$managedUser->getId(), (string) $request->request->get('_token'))) {
            $this->addFlash('error', 'The delete form expired. Please try again.');

            return $this->redirectToRoute('app_admin_users', array_filter(['q' => trim((string) $request->request->get('q', ''))]));
        }

        $currentUser = $this->getUser();

        if ($currentUser instanceof User && $managedUser->getId() === $currentUser->getId()) {
            $this->addFlash('error', 'Use your profile page if you need to delete your own account.');

            return $this->redirectToRoute('app_admin_users', array_filter(['q' => trim((string) $request->request->get('q', ''))]));
        }

        if ($managedUser->isAdmin() && $userRepository->count(['isAdmin' => true]) <= 1) {
            $this->addFlash('error', 'You cannot delete the last admin account.');

            return $this->redirectToRoute('app_admin_users', array_filter(['q' => trim((string) $request->request->get('q', ''))]));
        }

        $entityManager->remove($managedUser);
        $entityManager->flush();

        $this->addFlash('success', 'User deleted successfully.');

        return $this->redirectToRoute('app_admin_users', array_filter(['q' => trim((string) $request->request->get('q', ''))]));
    }
}
