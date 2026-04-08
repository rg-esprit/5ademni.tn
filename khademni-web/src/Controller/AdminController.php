<?php

namespace App\Controller;

use App\Entity\Category;
use App\Entity\Gig;
use App\Entity\User;
use App\Repository\CategoryRepository;
use App\Repository\GigRepository;
use App\Repository\UserRepository;
use App\Service\BlobStorageService;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

#[Route('/admin')]
class AdminController extends AbstractController
{
    #[Route('/dashboard', name: 'app_admin_dashboard', methods: ['GET'])]
    public function dashboard(
        CategoryRepository $categoryRepository,
        GigRepository $gigRepository,
        UserRepository $userRepository,
        BlobStorageService $blobStorageService,
    ): Response {
        $currentUser = $this->getCurrentUser();
        if (!$currentUser instanceof User) {
            return $this->redirectToRoute('app_login');
        }

        if (!$this->isGranted('ROLE_ADMIN')) {
            $this->addFlash('error', 'You are not allowed to access the admin area.');

            return $this->redirectToRoute('app_profile');
        }

        $allGigs = $gigRepository->findAllWithCategory();
        $gigStats = $this->buildGigStats($allGigs);
        $categoryStats = $categoryRepository->getDashboardData();

        $chartData = [
            'labels' => array_map(static fn (array $row): string => (string) $row['name'], $categoryStats['rows']),
            'values' => array_map(static fn (array $row): int => (int) $row['gig_count'], $categoryStats['rows']),
            'colors' => array_map(static fn (array $row): string => (string) $row['color'], $categoryStats['rows']),
        ];

        return $this->render('admin/dashboard.html.twig', [
            'avatar_proxy_enabled' => $blobStorageService->isConfigured() && $currentUser->hasProfileImage(),
            'stats' => [
                'users' => $userRepository->count([]),
                'admins' => $userRepository->count(['isAdmin' => true]),
                'categories' => $categoryStats['total_categories'],
                'active_categories' => $categoryStats['active_categories'],
                'gigs' => $gigStats['total'],
                'active_gigs' => $gigStats['active'],
                'inactive_gigs' => $gigStats['inactive'],
                'expired_gigs' => $gigStats['expired'],
                'revenue' => $gigStats['revenue'],
            ],
            'category_rows' => array_slice($categoryStats['rows'], 0, 8),
            'recent_gigs' => array_slice($allGigs, 0, 6),
            'chart_data' => $chartData,
        ]);
    }

    #[Route('/categories', name: 'app_admin_categories', methods: ['GET', 'POST'])]
    public function categories(
        Request $request,
        CategoryRepository $categoryRepository,
        EntityManagerInterface $entityManager,
        BlobStorageService $blobStorageService,
    ): Response {
        $currentUser = $this->getCurrentUser();
        if (!$currentUser instanceof User) {
            return $this->redirectToRoute('app_login');
        }

        if (!$this->isGranted('ROLE_ADMIN')) {
            $this->addFlash('error', 'You are not allowed to access the admin area.');

            return $this->redirectToRoute('app_profile');
        }

        $form = [
            'name' => '',
            'description' => '',
            'is_active' => true,
        ];
        $errors = [];

        if ($request->isMethod('POST')) {
            if (!$this->isCsrfTokenValid('admin_category_create', (string) $request->request->get('_token'))) {
                $this->addFlash('error', 'Category form expired. Please try again.');

                return $this->redirectToRoute('app_admin_categories');
            }

            $form['name'] = trim((string) $request->request->get('name', ''));
            $form['description'] = trim((string) $request->request->get('description', ''));
            $form['is_active'] = '1' === (string) $request->request->get('is_active');

            if (mb_strlen($form['name']) < 3) {
                $errors['name'] = 'Category name must be at least 3 characters.';
            }

            if (mb_strlen($form['description']) < 10) {
                $errors['description'] = 'Category description must be at least 10 characters.';
            }

            if ($categoryRepository->nameExists($form['name'])) {
                $errors['name'] = 'A category with this name already exists.';
            }

            if ([] === $errors) {
                $category = (new Category())
                    ->setName($form['name'])
                    ->setDescription($form['description'])
                    ->setIsActive($form['is_active']);

                $entityManager->persist($category);
                $entityManager->flush();
                $this->addFlash('success', 'Category created successfully.');

                return $this->redirectToRoute('app_admin_categories');
            }
        }

        $filters = [
            'q' => trim((string) $request->query->get('q', '')),
            'status' => strtolower(trim((string) $request->query->get('status', 'all'))),
        ];

        if (!in_array($filters['status'], ['all', 'active', 'inactive'], true)) {
            $filters['status'] = 'all';
        }

        $categories = $categoryRepository->findForFilters($filters['q'], $filters['status']);
        $dashboard = $categoryRepository->getDashboardData();
        $distributionById = [];

        foreach ($dashboard['rows'] as $row) {
            $distributionById[(int) $row['id']] = $row;
        }

        return $this->render('admin/categories.html.twig', [
            'avatar_proxy_enabled' => $blobStorageService->isConfigured() && $currentUser->hasProfileImage(),
            'categories' => $categories,
            'distribution_by_id' => $distributionById,
            'dashboard' => $dashboard,
            'filters' => $filters,
            'form' => $form,
            'errors' => $errors,
        ]);
    }

    #[Route('/categories/{id}/toggle', name: 'app_admin_category_toggle', methods: ['POST'])]
    public function toggleCategory(
        Request $request,
        Category $category,
        EntityManagerInterface $entityManager,
    ): Response {
        if (!$this->isGranted('ROLE_ADMIN')) {
            return $this->redirectToRoute('app_profile');
        }

        if (!$this->isCsrfTokenValid('admin_toggle_category_'.$category->getId(), (string) $request->request->get('_token'))) {
            $this->addFlash('error', 'Category action expired. Please try again.');

            return $this->redirectToRoute('app_admin_categories');
        }

        $category->setIsActive(!$category->isActive());
        $entityManager->flush();

        $this->addFlash('success', sprintf('Category "%s" is now %s.', $category->getName(), $category->isActive() ? 'active' : 'inactive'));

        return $this->redirectToRoute('app_admin_categories');
    }

    #[Route('/categories/{id}/delete', name: 'app_admin_category_delete', methods: ['POST'])]
    public function deleteCategory(
        Request $request,
        Category $category,
        EntityManagerInterface $entityManager,
    ): Response {
        if (!$this->isGranted('ROLE_ADMIN')) {
            return $this->redirectToRoute('app_profile');
        }

        if (!$this->isCsrfTokenValid('admin_delete_category_'.$category->getId(), (string) $request->request->get('_token'))) {
            $this->addFlash('error', 'Delete action expired. Please try again.');

            return $this->redirectToRoute('app_admin_categories');
        }

        if ($category->getGigs()->count() > 0) {
            $this->addFlash('error', 'Cannot delete a category that still has gigs. Reassign or delete gigs first.');

            return $this->redirectToRoute('app_admin_categories');
        }

        $name = $category->getName();
        $entityManager->remove($category);
        $entityManager->flush();

        $this->addFlash('success', sprintf('Category "%s" deleted.', $name));

        return $this->redirectToRoute('app_admin_categories');
    }

    #[Route('/gigs', name: 'app_admin_gigs', methods: ['GET'])]
    public function gigs(
        Request $request,
        GigRepository $gigRepository,
        CategoryRepository $categoryRepository,
        UserRepository $userRepository,
        BlobStorageService $blobStorageService,
    ): Response {
        $currentUser = $this->getCurrentUser();
        if (!$currentUser instanceof User) {
            return $this->redirectToRoute('app_login');
        }

        if (!$this->isGranted('ROLE_ADMIN')) {
            $this->addFlash('error', 'You are not allowed to access the admin area.');

            return $this->redirectToRoute('app_profile');
        }

        $filters = [
            'query' => trim((string) $request->query->get('q', '')),
            'category_id' => $this->toNullableInt($request->query->get('category_id')),
            'status' => strtoupper(trim((string) $request->query->get('status', 'ALL'))),
            'min_price' => null,
            'max_price' => null,
        ];

        if (!in_array($filters['status'], ['ALL', 'ACTIVE', 'INACTIVE', 'EXPIRED'], true)) {
            $filters['status'] = 'ALL';
        }

        $allGigs = $gigRepository->findAllWithCategory();
        $gigs = $gigRepository->filterInMemory($allGigs, $filters);
        $stats = $this->buildGigStats($allGigs);

        $ownerById = $this->buildOwnerMap($gigs, $userRepository);

        return $this->render('admin/gigs.html.twig', [
            'avatar_proxy_enabled' => $blobStorageService->isConfigured() && $currentUser->hasProfileImage(),
            'gigs' => $gigs,
            'owner_by_id' => $ownerById,
            'stats' => $stats,
            'filters' => $filters,
            'categories' => $categoryRepository->findAllOrdered(),
        ]);
    }

    #[Route('/gigs/{id}/status', name: 'app_admin_gig_status', methods: ['POST'])]
    public function updateGigStatus(
        Request $request,
        Gig $gig,
        EntityManagerInterface $entityManager,
    ): Response {
        if (!$this->isGranted('ROLE_ADMIN')) {
            return $this->redirectToRoute('app_profile');
        }

        if (!$this->isCsrfTokenValid('admin_gig_status_'.$gig->getId(), (string) $request->request->get('_token'))) {
            $this->addFlash('error', 'Status action expired. Please try again.');

            return $this->redirectToRoute('app_admin_gigs');
        }

        $status = strtoupper(trim((string) $request->request->get('status', '')));
        if (!in_array($status, ['ACTIVE', 'INACTIVE'], true)) {
            $this->addFlash('error', 'Invalid status selected.');

            return $this->redirectToRoute('app_admin_gigs');
        }

        $gig->setStatus($status);
        $entityManager->flush();

        $this->addFlash('success', sprintf('Gig "%s" status updated to %s.', $gig->getTitle(), $status));

        return $this->redirectToRoute('app_admin_gigs');
    }

    #[Route('/gigs/{id}/delete', name: 'app_admin_gig_delete', methods: ['POST'])]
    public function deleteGig(
        Request $request,
        Gig $gig,
        EntityManagerInterface $entityManager,
    ): Response {
        if (!$this->isGranted('ROLE_ADMIN')) {
            return $this->redirectToRoute('app_profile');
        }

        if (!$this->isCsrfTokenValid('admin_delete_gig_'.$gig->getId(), (string) $request->request->get('_token'))) {
            $this->addFlash('error', 'Delete action expired. Please try again.');

            return $this->redirectToRoute('app_admin_gigs');
        }

        $title = $gig->getTitle();
        $entityManager->remove($gig);
        $entityManager->flush();

        $this->addFlash('success', sprintf('Gig "%s" deleted.', $title));

        return $this->redirectToRoute('app_admin_gigs');
    }

    /**
     * @param Gig[] $gigs
     *
     * @return array{total:int, active:int, inactive:int, expired:int, revenue:float}
     */
    private function buildGigStats(array $gigs): array
    {
        $active = 0;
        $inactive = 0;
        $expired = 0;
        $revenue = 0.0;
        $now = new \DateTimeImmutable();

        foreach ($gigs as $gig) {
            $status = $gig->getDisplayStatus($now);
            if ('ACTIVE' === $status) {
                ++$active;
            } elseif ('INACTIVE' === $status) {
                ++$inactive;
            } elseif ('EXPIRED' === $status) {
                ++$expired;
            }

            $revenue += $gig->getPrice();
        }

        return [
            'total' => count($gigs),
            'active' => $active,
            'inactive' => $inactive,
            'expired' => $expired,
            'revenue' => $revenue,
        ];
    }

    /**
     * @param Gig[] $gigs
     *
     * @return array<int, string>
     */
    private function buildOwnerMap(array $gigs, UserRepository $userRepository): array
    {
        $ids = [];
        foreach ($gigs as $gig) {
            $userId = $gig->getUserId();
            if (null !== $userId) {
                $ids[$userId] = true;
            }
        }

        if ([] === $ids) {
            return [];
        }

        $users = $userRepository->findBy(['id' => array_keys($ids)]);
        $map = [];

        foreach ($users as $user) {
            $map[$user->getId()] = $user->getDisplayName();
        }

        return $map;
    }

    private function toNullableInt(mixed $value): ?int
    {
        if (null === $value) {
            return null;
        }

        $normalized = trim((string) $value);
        if ('' === $normalized || !is_numeric($normalized)) {
            return null;
        }

        $int = (int) $normalized;

        return $int > 0 ? $int : null;
    }

    private function getCurrentUser(): ?User
    {
        $user = $this->getUser();

        return $user instanceof User ? $user : null;
    }
}
