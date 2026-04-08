<?php

namespace App\Controller;

use App\Entity\Category;
use App\Entity\User;
use App\Repository\CategoryRepository;
use App\Service\BlobStorageService;
use App\Service\GigAiService;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

class CategoryController extends AbstractController
{
    #[Route('/categories', name: 'app_categories', methods: ['GET'])]
    public function index(
        Request $request,
        CategoryRepository $categoryRepository,
        BlobStorageService $blobStorageService,
    ): Response {
        $user = $this->getCurrentUser();
        if (!$user instanceof User) {
            return $this->redirectToRoute('app_login');
        }

        if ($redirect = $this->redirectIfNotAdmin()) {
            return $redirect;
        }

        $query = trim((string) $request->query->get('q', ''));
        $status = strtolower(trim((string) $request->query->get('status', 'all')));
        if (!in_array($status, ['all', 'active', 'inactive'], true)) {
            $status = 'all';
        }

        $categories = $categoryRepository->findForFilters($query, $status);
        $dashboard = $categoryRepository->getDashboardData();
        $distributionById = [];
        foreach ($dashboard['rows'] as $row) {
            $distributionById[(int) $row['id']] = $row;
        }

        return $this->render('category/index.html.twig', [
            'avatar_proxy_enabled' => $blobStorageService->isConfigured() && $user->hasProfileImage(),
            'categories' => $categories,
            'dashboard' => $dashboard,
            'distribution_by_id' => $distributionById,
            'filters' => [
                'q' => $query,
                'status' => $status,
            ],
        ]);
    }

    #[Route('/categories/new', name: 'app_category_new', methods: ['GET', 'POST'])]
    public function create(
        Request $request,
        EntityManagerInterface $entityManager,
        GigAiService $gigAiService,
        BlobStorageService $blobStorageService,
    ): Response {
        return $this->handleForm($request, $entityManager, $gigAiService, $blobStorageService, null);
    }

    #[Route('/categories/{id}/edit', name: 'app_category_edit', methods: ['GET', 'POST'])]
    public function edit(
        Request $request,
        Category $category,
        EntityManagerInterface $entityManager,
        GigAiService $gigAiService,
        BlobStorageService $blobStorageService,
    ): Response {
        return $this->handleForm($request, $entityManager, $gigAiService, $blobStorageService, $category);
    }

    #[Route('/categories/{id}/delete', name: 'app_category_delete', methods: ['POST'])]
    public function delete(
        Request $request,
        Category $category,
        EntityManagerInterface $entityManager,
    ): Response {
        $user = $this->getCurrentUser();
        if (!$user instanceof User) {
            return $this->redirectToRoute('app_login');
        }

        if ($redirect = $this->redirectIfNotAdmin()) {
            return $redirect;
        }

        if (!$this->isCsrfTokenValid('delete_category_'.$category->getId(), (string) $request->request->get('_token'))) {
            $this->addFlash('error', 'Delete action expired. Please try again.');

            return $this->redirectToRoute('app_categories');
        }

        try {
            $entityManager->remove($category);
            $entityManager->flush();
            $this->addFlash('success', 'Category deleted successfully.');
        } catch (\Throwable $exception) {
            $this->addFlash('error', 'Category could not be deleted: '.$exception->getMessage());
        }

        return $this->redirectToRoute('app_categories');
    }

    private function handleForm(
        Request $request,
        EntityManagerInterface $entityManager,
        GigAiService $gigAiService,
        BlobStorageService $blobStorageService,
        ?Category $category,
    ): Response {
        $user = $this->getCurrentUser();
        if (!$user instanceof User) {
            return $this->redirectToRoute('app_login');
        }

        if ($redirect = $this->redirectIfNotAdmin()) {
            return $redirect;
        }

        $isEdit = $category instanceof Category;
        $entity = $category ?? (new Category());

        $values = [
            'name' => $isEdit ? $entity->getName() : '',
            'description' => $isEdit ? (string) $entity->getDescription() : '',
            'is_active' => $isEdit ? $entity->isActive() : true,
        ];

        $errors = [];

        if ($request->isMethod('POST')) {
            if (!$this->isCsrfTokenValid('category_form', (string) $request->request->get('_token'))) {
                $this->addFlash('error', 'The category form expired. Please try again.');

                return $this->redirectToRoute($isEdit ? 'app_category_edit' : 'app_category_new', $isEdit ? ['id' => $entity->getId()] : []);
            }

            $values['name'] = trim((string) $request->request->get('name', ''));
            $values['description'] = trim((string) $request->request->get('description', ''));
            $values['is_active'] = '1' === (string) $request->request->get('is_active');

            $action = (string) $request->request->get('form_action', 'save');

            if ('generate_description' === $action) {
                if ('' === $values['name']) {
                    $errors['name'] = 'Name is required before generating a description.';
                } else {
                    $values['description'] = $gigAiService->generateCategoryDescription($values['name']);
                    $this->addFlash('success', 'AI description generated. You can edit it before saving.');
                }
            } else {
                if (mb_strlen($values['name']) < 3) {
                    $errors['name'] = 'Name must be at least 3 characters.';
                }

                if (mb_strlen($values['description']) < 10) {
                    $errors['description'] = 'Description must be at least 10 characters.';
                }

                if ([] === $errors) {
                    $entity
                        ->setName($values['name'])
                        ->setDescription($values['description'])
                        ->setIsActive($values['is_active']);

                    if (!$isEdit) {
                        $entityManager->persist($entity);
                    }

                    $entityManager->flush();
                    $this->addFlash('success', $isEdit ? 'Category updated successfully.' : 'Category created successfully.');

                    return $this->redirectToRoute('app_categories');
                }
            }
        }

        return $this->render('category/form.html.twig', [
            'avatar_proxy_enabled' => $blobStorageService->isConfigured() && $user->hasProfileImage(),
            'is_edit' => $isEdit,
            'category' => $entity,
            'values' => $values,
            'errors' => $errors,
        ]);
    }

    private function getCurrentUser(): ?User
    {
        $user = $this->getUser();

        return $user instanceof User ? $user : null;
    }

    private function redirectIfNotAdmin(): ?Response
    {
        if ($this->isGranted('ROLE_ADMIN')) {
            return null;
        }

        $this->addFlash('error', 'Only admins can manage categories.');

        return $this->redirectToRoute('app_profile');
    }
}
