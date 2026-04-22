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
use Symfony\UX\Chartjs\Builder\ChartBuilderInterface;
use Symfony\UX\Chartjs\Model\Chart;

class AdminUserController extends AbstractController
{
    #[Route('/admin/users', name: 'app_admin_users', methods: ['GET', 'POST'])]
    public function index(
        Request $request,
        UserRepository $userRepository,
        EntityManagerInterface $entityManager,
        UserPasswordHasherInterface $passwordHasher,
        BlobStorageService $blobStorageService,
        ChartBuilderInterface $chartBuilder,
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
        $totalUsers = count($allUsers);
        $adminCount = 0;
        $profileImageCount = 0;
        $balanceRangeCounts = [
            'Below 0 TND' => 0,
            '0-99 TND' => 0,
            '100-499 TND' => 0,
            '500+ TND' => 0,
        ];

        foreach ($allUsers as $user) {
            if ($user->isAdmin()) {
                ++$adminCount;
            }

            if ($user->hasProfileImage()) {
                ++$profileImageCount;
            }

            $balanceRange = match (true) {
                $user->getBalance() < 0 => 'Below 0 TND',
                $user->getBalance() < 100 => '0-99 TND',
                $user->getBalance() < 500 => '100-499 TND',
                default => '500+ TND',
            };

            ++$balanceRangeCounts[$balanceRange];
        }

        $adminRoleRows = $this->buildPercentageRows([
            'Admins' => $adminCount,
            'Non-admins' => $totalUsers - $adminCount,
        ], $totalUsers);

        $balanceDistributionRows = $this->buildPercentageRows($balanceRangeCounts, $totalUsers);

        $profileImageRows = $this->buildPercentageRows([
            'Profile image set' => $profileImageCount,
            'No profile image' => $totalUsers - $profileImageCount,
        ], $totalUsers);

        return $this->render('admin/users.html.twig', [
            'admin_user_form' => $form,
            'admin_role_chart' => $totalUsers > 0 ? $this->buildDoughnutChart($chartBuilder, $adminRoleRows, ['#0b5fff', '#dbe5f0']) : null,
            'admin_role_rows' => $adminRoleRows,
            'avatar_proxy_enabled' => $currentUser instanceof User && $blobStorageService->isConfigured() && $currentUser->hasProfileImage(),
            'balance_distribution_chart' => $totalUsers > 0 ? $this->buildDoughnutChart($chartBuilder, $balanceDistributionRows, ['#dc2626', '#8b5cf6', '#0ea5e9', '#059669']) : null,
            'balance_distribution_rows' => $balanceDistributionRows,
            'editing_user' => $editingUser,
            'listed_users' => $listedUsers,
            'profile_image_chart' => $totalUsers > 0 ? $this->buildDoughnutChart($chartBuilder, $profileImageRows, ['#7c3aed', '#e2e8f0']) : null,
            'profile_image_rows' => $profileImageRows,
            'search_query' => $searchQuery,
            'user_stats' => [
                'total' => $totalUsers,
                'admins' => $adminCount,
                'admin_percentage' => $this->calculatePercentage($adminCount, $totalUsers),
                'profile_image_percentage' => $this->calculatePercentage($profileImageCount, $totalUsers),
                'with_profile_img' => $profileImageCount,
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

    /**
     * @param array<string, int> $counts
     *
     * @return list<array{label: string, count: int, percentage: float}>
     */
    private function buildPercentageRows(array $counts, int $total): array
    {
        $rows = [];

        foreach ($counts as $label => $count) {
            $rows[] = [
                'label' => $label,
                'count' => $count,
                'percentage' => $this->calculatePercentage($count, $total),
            ];
        }

        return $rows;
    }

    /**
     * @param list<array{label: string, count: int, percentage: float}> $rows
     * @param list<string> $colors
     */
    private function buildDoughnutChart(ChartBuilderInterface $chartBuilder, array $rows, array $colors): Chart
    {
        $chart = $chartBuilder->createChart(Chart::TYPE_DOUGHNUT);

        $chart->setData([
            'labels' => array_map(static fn (array $row): string => $row['label'], $rows),
            'datasets' => [[
                'data' => array_map(static fn (array $row): float => $row['percentage'], $rows),
                'backgroundColor' => $colors,
                'borderWidth' => 0,
                'hoverOffset' => 8,
            ]],
        ]);

        $chart->setOptions([
            'cutout' => '68%',
            'maintainAspectRatio' => false,
            'plugins' => [
                'legend' => [
                    'position' => 'bottom',
                    'labels' => [
                        'boxWidth' => 10,
                        'usePointStyle' => true,
                    ],
                ],
            ],
        ]);

        return $chart;
    }

    private function calculatePercentage(int $count, int $total): float
    {
        if ($total <= 0) {
            return 0.0;
        }

        return round(($count / $total) * 100, 1);
    }
}
