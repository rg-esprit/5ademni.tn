<?php

namespace App\Controller;

use App\Entity\Category;
use App\Entity\Gig;
use App\Entity\User;
use App\Repository\CategoryRepository;
use App\Repository\GigRepository;
use App\Repository\UserRepository;
use App\Service\BlobStorageService;
use App\Service\CategoryRequestNotificationService;
use App\Service\GigApprovalNotificationService;
use App\Service\SearchQueryFactory;
use FOS\ElasticaBundle\Finder\TransformedFinder;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Component\DependencyInjection\Attribute\Autowire;
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
                'active_gigs' => $gigStats['approved'],
                'inactive_gigs' => $gigStats['draft'] + $gigStats['pending'] + $gigStats['rejected'] + $gigStats['archived'],
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
        #[Autowire(service: 'fos_elastica.finder.category')]
        TransformedFinder $categoryFinder,
        SearchQueryFactory $searchQueryFactory,
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

        try {
            $categories = $categoryFinder->find($searchQueryFactory->buildCategorySearchQuery($filters['q'], $filters['status']));
        } catch (\Throwable) {
            $categories = $categoryRepository->findForFilters($filters['q'], $filters['status']);
        }
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
        UserRepository $userRepository,
        CategoryRequestNotificationService $categoryRequestNotificationService,
    ): Response {
        if (!$this->isGranted('ROLE_ADMIN')) {
            return $this->redirectToRoute('app_profile');
        }

        if (!$this->isCsrfTokenValid('admin_toggle_category_'.$category->getId(), (string) $request->request->get('_token'))) {
            $this->addFlash('error', 'Category action expired. Please try again.');

            return $this->redirectToRoute('app_admin_categories');
        }

        $wasActive = $category->isActive();
        $category->setIsActive(!$wasActive);
        $entityManager->flush();

        if (!$wasActive && $category->isActive()) {
            $requester = $this->findCategoryRequester($category, $userRepository);
            if ($requester instanceof User) {
                try {
                    $categoryRequestNotificationService->sendApproval($requester, $category);
                } catch (\Throwable) {
                    $this->addFlash('warning', 'Category approved, but the approval email could not be sent.');
                }
            }
        }

        $this->addFlash('success', sprintf('Category "%s" is now %s.', $category->getName(), $category->isActive() ? 'active' : 'inactive'));

        return $this->redirectToRoute('app_admin_categories');
    }

    #[Route('/categories/{id}/reject', name: 'app_admin_category_reject', methods: ['POST'])]
    public function rejectCategory(
        Request $request,
        Category $category,
        EntityManagerInterface $entityManager,
        UserRepository $userRepository,
        CategoryRequestNotificationService $categoryRequestNotificationService,
    ): Response {
        if (!$this->isGranted('ROLE_ADMIN')) {
            return $this->redirectToRoute('app_profile');
        }

        if (!$this->isCsrfTokenValid('admin_reject_category_'.$category->getId(), (string) $request->request->get('_token'))) {
            $this->addFlash('error', 'Reject action expired. Please try again.');

            return $this->redirectToRoute('app_admin_categories');
        }

        if (!$this->isCategoryPendingRequest($category)) {
            $this->addFlash('error', 'Only pending category requests can be rejected with a reason.');

            return $this->redirectToRoute('app_admin_categories');
        }

        if ($category->getGigs()->count() > 0) {
            $this->addFlash('error', 'Cannot reject this category request while gigs are assigned to it.');

            return $this->redirectToRoute('app_admin_categories');
        }

        $reason = trim((string) $request->request->get('reason', ''));
        if (mb_strlen($reason) < 8) {
            $this->addFlash('error', 'Rejection reason must be at least 8 characters.');

            return $this->redirectToRoute('app_admin_categories');
        }

        $requester = $this->findCategoryRequester($category, $userRepository);
        if (!$requester instanceof User) {
            $this->addFlash('error', 'Could not identify the requester for this category.');

            return $this->redirectToRoute('app_admin_categories');
        }

        $categoryName = $category->getName();
        $entityManager->remove($category);
        $entityManager->flush();

        try {
            $categoryRequestNotificationService->sendRejection($requester, $categoryName, $reason);
        } catch (\Throwable) {
            $this->addFlash('warning', 'Category request rejected, but the rejection email could not be sent.');
        }

        $this->addFlash('success', sprintf('Category request "%s" rejected.', $categoryName));

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
        #[Autowire(service: 'fos_elastica.finder.gig')]
        TransformedFinder $gigFinder,
        SearchQueryFactory $searchQueryFactory,
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

        if (!in_array($filters['status'], Gig::getFilterableStatuses(), true)) {
            $filters['status'] = 'ALL';
        }

        $allGigs = $gigRepository->findAllWithCategory();
        try {
            $gigs = $gigFinder->find($searchQueryFactory->buildGigSearchQuery($filters));
        } catch (\Throwable) {
            $gigs = $gigRepository->filterInMemory($allGigs, $filters);
        }
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
        UserRepository $userRepository,
        GigApprovalNotificationService $gigApprovalNotificationService,
    ): Response {
        if (!$this->isGranted('ROLE_ADMIN')) {
            return $this->redirectToRoute('app_profile');
        }

        if (!$this->isCsrfTokenValid('admin_gig_status_'.$gig->getId(), (string) $request->request->get('_token'))) {
            $this->addFlash('error', 'Status action expired. Please try again.');

            return $this->redirectToRoute('app_admin_gigs');
        }

        $status = Gig::normalizeStatus((string) $request->request->get('status', ''));
        if (!in_array($status, Gig::getAdminActionStatuses(), true)) {
            $this->addFlash('error', 'Invalid status selected.');

            return $this->redirectToRoute('app_admin_gigs');
        }

        if (!$gig->canTransitionTo($status, true)) {
            $this->addFlash('error', sprintf('Transition not allowed from %s to %s.', $gig->getWorkflowStatus(), $status));

            return $this->redirectToRoute('app_admin_gigs');
        }

        if (Gig::STATUS_APPROVED === $status) {
            $validationErrors = $gig->getApprovalValidationErrors();
            if ([] !== $validationErrors) {
                $this->addFlash('error', 'Cannot approve this gig: '.implode(' ', $validationErrors));

                return $this->redirectToRoute('app_admin_gigs');
            }
        }

        $wasApproved = Gig::STATUS_APPROVED === $gig->getWorkflowStatus();
        $wasRejected = Gig::STATUS_REJECTED === $gig->getWorkflowStatus();

        $gig->setStatus($status);
        $entityManager->flush();

        if (!$wasApproved && Gig::STATUS_APPROVED === $gig->getWorkflowStatus()) {
            $ownerId = $gig->getUserId();
            if (null !== $ownerId) {
                $owner = $userRepository->find($ownerId);
                if ($owner instanceof User) {
                    try {
                        $gigApprovalNotificationService->sendApproval($owner, $gig);
                    } catch (\Throwable) {
                        $this->addFlash('warning', 'Gig approved, but the approval email could not be queued.');
                    }
                }
            }
        }

        if (!$wasRejected && Gig::STATUS_REJECTED === $gig->getWorkflowStatus()) {
            $ownerId = $gig->getUserId();
            if (null !== $ownerId) {
                $owner = $userRepository->find($ownerId);
                if ($owner instanceof User) {
                    try {
                        $gigApprovalNotificationService->sendRejection($owner, $gig);
                    } catch (\Throwable) {
                        $this->addFlash('warning', 'Gig rejected, but the rejection email could not be queued.');
                    }
                }
            }
        }

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
     * @return array{total:int, draft:int, pending:int, approved:int, rejected:int, archived:int, expired:int, revenue:float}
     */
    private function buildGigStats(array $gigs): array
    {
        $draft = 0;
        $pending = 0;
        $approved = 0;
        $rejected = 0;
        $archived = 0;
        $expired = 0;
        $revenue = 0.0;
        $now = new \DateTimeImmutable();

        foreach ($gigs as $gig) {
            $status = $gig->getDisplayStatus($now);
            if (Gig::STATUS_DRAFT === $status) {
                ++$draft;
            } elseif (Gig::STATUS_PENDING === $status) {
                ++$pending;
            } elseif (Gig::STATUS_APPROVED === $status) {
                ++$approved;
            } elseif (Gig::STATUS_REJECTED === $status) {
                ++$rejected;
            } elseif (Gig::STATUS_ARCHIVED === $status) {
                ++$archived;
            } elseif (Gig::STATUS_EXPIRED === $status) {
                ++$expired;
            }

            $revenue += $gig->getPrice();
        }

        return [
            'total' => count($gigs),
            'draft' => $draft,
            'pending' => $pending,
            'approved' => $approved,
            'rejected' => $rejected,
            'archived' => $archived,
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

    private function isCategoryPendingRequest(Category $category): bool
    {
        return !$category->isActive() && null !== $this->extractCategoryRequesterId($category);
    }

    private function findCategoryRequester(Category $category, UserRepository $userRepository): ?User
    {
        $requesterId = $this->extractCategoryRequesterId($category);
        if (null === $requesterId) {
            return null;
        }

        $requester = $userRepository->find($requesterId);

        return $requester instanceof User ? $requester : null;
    }

    private function extractCategoryRequesterId(Category $category): ?int
    {
        $description = (string) ($category->getDescription() ?? '');
        if ('' === trim($description)) {
            return null;
        }

        if (!preg_match('/\(user\s+#(\d+)\)/i', $description, $matches)) {
            return null;
        }

        $userId = (int) ($matches[1] ?? 0);

        return $userId > 0 ? $userId : null;
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
