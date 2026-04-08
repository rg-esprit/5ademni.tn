<?php

namespace App\Controller;

use App\Entity\Category;
use App\Entity\Gig;
use App\Entity\User;
use App\Repository\CategoryRepository;
use App\Repository\GigRepository;
use App\Service\BlobStorageService;
use App\Service\ContentModerationService;
use App\Service\GigAiService;
use App\Service\PriceSuggestionService;
use App\Service\SpamDetectionService;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

class GigController extends AbstractController
{
    #[Route('/gigs', name: 'app_gigs', methods: ['GET'])]
    public function index(
        Request $request,
        GigRepository $gigRepository,
        CategoryRepository $categoryRepository,
        BlobStorageService $blobStorageService,
    ): Response {
        $user = $this->getCurrentUser();
        if (!$user instanceof User) {
            return $this->redirectToRoute('app_login');
        }

        $filters = $this->extractFilters($request);
        $allGigs = $gigRepository->findAllWithCategory();
        $filtered = $gigRepository->filterInMemory($allGigs, $filters);
        $stats = $gigRepository->summarize($allGigs);
        $categories = $categoryRepository->findAllOrdered();

        $now = new \DateTimeImmutable();
        $cards = [];
        foreach ($filtered as $gig) {
            $status = $gig->getDisplayStatus($now);
            $cards[] = [
                'id' => $gig->getId(),
                'title' => $gig->getTitle(),
                'description' => $gig->getDescription(),
                'price' => $gig->getPrice(),
                'image' => $gig->getImage(),
                'status' => $status,
                'category' => $gig->getCategory()?->getName() ?? 'Uncategorized',
                'is_owner' => $gig->isOwner($user),
                'is_expired' => 'EXPIRED' === $status,
                'user_id' => $gig->getUserId(),
            ];
        }

        return $this->render('gig/index.html.twig', [
            'avatar_proxy_enabled' => $blobStorageService->isConfigured() && $user->hasProfileImage(),
            'cards' => $cards,
            'filters' => $filters,
            'stats' => $stats,
            'categories' => $categories,
        ]);
    }

    #[Route('/gigs/new', name: 'app_gig_new', methods: ['GET', 'POST'])]
    public function create(
        Request $request,
        EntityManagerInterface $entityManager,
        CategoryRepository $categoryRepository,
        BlobStorageService $blobStorageService,
        GigAiService $gigAiService,
        PriceSuggestionService $priceSuggestionService,
        SpamDetectionService $spamDetectionService,
        ContentModerationService $contentModerationService,
    ): Response {
        return $this->handleForm(
            $request,
            $entityManager,
            $categoryRepository,
            $blobStorageService,
            $gigAiService,
            $priceSuggestionService,
            $spamDetectionService,
            $contentModerationService,
            null,
        );
    }

    #[Route('/gigs/{id}/edit', name: 'app_gig_edit', methods: ['GET', 'POST'])]
    public function edit(
        Request $request,
        Gig $gig,
        EntityManagerInterface $entityManager,
        CategoryRepository $categoryRepository,
        BlobStorageService $blobStorageService,
        GigAiService $gigAiService,
        PriceSuggestionService $priceSuggestionService,
        SpamDetectionService $spamDetectionService,
        ContentModerationService $contentModerationService,
    ): Response {
        return $this->handleForm(
            $request,
            $entityManager,
            $categoryRepository,
            $blobStorageService,
            $gigAiService,
            $priceSuggestionService,
            $spamDetectionService,
            $contentModerationService,
            $gig,
        );
    }

    #[Route('/gigs/{id}/delete', name: 'app_gig_delete', methods: ['POST'])]
    public function delete(Request $request, Gig $gig, EntityManagerInterface $entityManager): Response
    {
        $user = $this->getCurrentUser();
        if (!$user instanceof User) {
            return $this->redirectToRoute('app_login');
        }

        if (!$gig->isOwner($user)) {
            $this->addFlash('error', 'Only the gig owner can delete this gig.');

            return $this->redirectToRoute('app_gigs');
        }

        if (!$this->isCsrfTokenValid('delete_gig_'.$gig->getId(), (string) $request->request->get('_token'))) {
            $this->addFlash('error', 'Delete action expired. Please try again.');

            return $this->redirectToRoute('app_gigs');
        }

        try {
            $entityManager->remove($gig);
            $entityManager->flush();
            $this->addFlash('success', 'Gig deleted successfully.');
        } catch (\Throwable $exception) {
            $this->addFlash('error', 'Gig could not be deleted: '.$exception->getMessage());
        }

        return $this->redirectToRoute('app_gigs');
    }

    private function handleForm(
        Request $request,
        EntityManagerInterface $entityManager,
        CategoryRepository $categoryRepository,
        BlobStorageService $blobStorageService,
        GigAiService $gigAiService,
        PriceSuggestionService $priceSuggestionService,
        SpamDetectionService $spamDetectionService,
        ContentModerationService $contentModerationService,
        ?Gig $gig,
    ): Response {
        $user = $this->getCurrentUser();
        if (!$user instanceof User) {
            return $this->redirectToRoute('app_login');
        }

        $isEdit = $gig instanceof Gig;
        $entity = $gig ?? new Gig();

        if ($isEdit && !$entity->isOwner($user)) {
            $this->addFlash('error', 'Only the gig owner can edit this gig.');

            return $this->redirectToRoute('app_gigs');
        }

        $isExpired = $isEdit && 'EXPIRED' === $entity->getDisplayStatus();
        $categories = $categoryRepository->findActiveOrdered();
        $categoryNames = array_map(static fn (Category $category): string => $category->getName(), $categories);

        $deliveryTime = $entity->getDeliveryTime();
        $values = [
            'title' => $isEdit ? $entity->getTitle() : '',
            'description' => $isEdit ? $entity->getDescription() : '',
            'price' => $isEdit ? number_format($entity->getPrice(), 2, '.', '') : '',
            'category_id' => $isEdit ? $entity->getCategory()?->getId() : null,
            'delivery_date' => $deliveryTime instanceof \DateTimeInterface ? $deliveryTime->format('Y-m-d') : '',
            'delivery_hour' => $deliveryTime instanceof \DateTimeInterface ? (int) $deliveryTime->format('H') : 12,
            'image' => $isEdit ? (string) $entity->getImage() : '',
            'status' => $isEdit ? strtolower($entity->getDisplayStatus()) : 'active',
            'ai_prompt' => '',
            'requested_category_name' => '',
        ];

        if (!in_array($values['status'], ['active', 'inactive'], true)) {
            $values['status'] = 'active';
        }

        $errors = [];
        $aiStatus = null;

        if ($request->isMethod('POST')) {
            if (!$this->isCsrfTokenValid('gig_form', (string) $request->request->get('_token'))) {
                $this->addFlash('error', 'The gig form expired. Please try again.');

                return $this->redirectToRoute($isEdit ? 'app_gig_edit' : 'app_gig_new', $isEdit ? ['id' => $entity->getId()] : []);
            }

            $values = [
                'title' => trim((string) $request->request->get('title', '')),
                'description' => trim((string) $request->request->get('description', '')),
                'price' => trim((string) $request->request->get('price', '')),
                'category_id' => $this->toNullableInt($request->request->get('category_id')),
                'delivery_date' => trim((string) $request->request->get('delivery_date', '')),
                'delivery_hour' => max(0, min(23, (int) $request->request->get('delivery_hour', 12))),
                'image' => trim((string) $request->request->get('image', '')),
                'status' => strtolower(trim((string) $request->request->get('status', 'active'))),
                'ai_prompt' => trim((string) $request->request->get('ai_prompt', '')),
                'requested_category_name' => trim((string) $request->request->get('requested_category_name', '')),
            ];

            if (!in_array($values['status'], ['active', 'inactive'], true)) {
                $values['status'] = 'active';
            }

            $action = (string) $request->request->get('form_action', 'save');

            if ('generate_gig' === $action) {
                if ('' === $values['ai_prompt']) {
                    $errors['ai_prompt'] = 'Please describe your skill before AI generation.';
                } else {
                    $generated = $gigAiService->generateGig($values['ai_prompt'], $categoryNames);
                    $values['title'] = $generated['title'];
                    $values['description'] = $generated['description'];
                    $values['price'] = number_format($generated['price'], 2, '.', '');
                    $values['status'] = 'active';
                    $values['delivery_date'] = (new \DateTimeImmutable())->modify('+'.$generated['delivery_days'].' days')->format('Y-m-d');
                    $values['delivery_hour'] = 12;
                    $values['category_id'] = $this->findCategoryIdByName($generated['category'], $categories);
                    $aiStatus = 'AI gig generated and pre-filled. Review the fields before saving.';
                    $this->addFlash('success', $aiStatus);
                }
            } elseif ('generate_description' === $action) {
                if ('' === $values['title']) {
                    $errors['title'] = 'Title is required before generating a description.';
                } else {
                    $categoryName = $this->findCategoryNameById($values['category_id'], $categories);
                    $values['description'] = $gigAiService->generateGigDescription($values['title'], $categoryName ?? 'General');
                    $aiStatus = 'AI description generated.';
                    $this->addFlash('success', $aiStatus);
                }
            } elseif ('suggest_category' === $action) {
                $suggestion = $gigAiService->suggestCategory($values['title'], $values['description'], $categoryNames);
                if (null !== $suggestion) {
                    $values['category_id'] = $this->findCategoryIdByName($suggestion, $categories);
                    $aiStatus = sprintf('AI suggested category: %s.', $suggestion);
                    $this->addFlash('success', $aiStatus);
                } else {
                    $errors['category_id'] = 'AI could not suggest a category yet. Add more detail in title/description.';
                }
            } elseif ('request_category' === $action) {
                $requestedCategoryName = trim((string) $values['requested_category_name']);

                if (mb_strlen($requestedCategoryName) < 3) {
                    $errors['requested_category_name'] = 'Requested category name must be at least 3 characters.';
                } elseif ($categoryRepository->nameExists($requestedCategoryName)) {
                    $errors['requested_category_name'] = 'This category already exists or is pending admin activation.';
                } else {
                    $requestDescription = sprintf(
                        'Category request submitted by %s (user #%d) on %s.',
                        $user->getDisplayName(),
                        (int) $user->getId(),
                        (new \DateTimeImmutable())->format('Y-m-d H:i')
                    );

                    $requestedCategory = (new Category())
                        ->setName($requestedCategoryName)
                        ->setDescription($requestDescription)
                        ->setIsActive(false);

                    $entityManager->persist($requestedCategory);
                    $entityManager->flush();

                    $values['requested_category_name'] = '';
                    $this->addFlash('success', sprintf('Category request "%s" submitted for admin review.', $requestedCategoryName));
                }
            } elseif ('suggest_price' === $action) {
                $selectedCategoryName = $this->findCategoryNameById($values['category_id'], $categories);
                if (null === $selectedCategoryName) {
                    $errors['category_id'] = 'Select a category before suggesting a price.';
                } else {
                    $delivery = $this->parseDeliveryDateTime($values['delivery_date'], $values['delivery_hour']);
                    $estimate = $priceSuggestionService->suggest($selectedCategoryName, $values['description'], $delivery);
                    $values['price'] = number_format($estimate['price'], 2, '.', '');
                    $aiStatus = sprintf('%s suggested %0.2f TND.', $estimate['source'], $estimate['price']);
                    $this->addFlash('success', $aiStatus);
                }
            } else {
                if ($isExpired) {
                    $errors['global'] = 'Expired gigs cannot be edited.';
                }

                $selectedCategory = $this->findCategoryById($values['category_id'], $categories);
                $delivery = $this->parseDeliveryDateTime($values['delivery_date'], $values['delivery_hour']);
                $price = $this->parsePrice($values['price']);

                if ('' === $values['title']) {
                    $errors['title'] = 'Title is required.';
                }

                if ('' === $values['description']) {
                    $errors['description'] = 'Description is required.';
                }

                if (null === $price) {
                    $errors['price'] = 'Price format is invalid.';
                } elseif ($price < 10.0) {
                    $errors['price'] = 'Minimum price is 10.00 TND.';
                }

                if (!$selectedCategory instanceof Category) {
                    $errors['category_id'] = 'Category is required.';
                }

                if (!$delivery instanceof \DateTimeImmutable) {
                    $errors['delivery_date'] = 'Delivery date is required.';
                } elseif ($delivery < new \DateTimeImmutable('today')) {
                    $errors['delivery_date'] = 'Delivery date must be today or in the future.';
                }

                if ([] === $errors && null !== $price && $selectedCategory instanceof Category && $delivery instanceof \DateTimeImmutable) {
                    $spam = $spamDetectionService->analyze($values['title'], $values['description'], $price);
                    if ($spam['is_spam']) {
                        $errors['global'] = sprintf('Gig flagged as spam (score %d/100): %s', $spam['score'], implode(' ', $spam['reasons']));
                    }

                    $moderation = $contentModerationService->analyze($values['title'], $values['description']);
                    if ($moderation['has_bad_words'] && in_array($moderation['severity'], ['MEDIUM', 'HIGH'], true)) {
                        $flagged = [] !== $moderation['detected_words'] ? implode(', ', $moderation['detected_words']) : 'AI-detected terms';
                        $errors['global'] = sprintf('Inappropriate content detected (%s): %s. %s', $moderation['severity'], $flagged, $moderation['explanation']);
                    }
                }

                if ([] === $errors && null !== $price && $selectedCategory instanceof Category && $delivery instanceof \DateTimeImmutable) {
                    $entity
                        ->setTitle($values['title'])
                        ->setDescription($values['description'])
                        ->setPrice($price)
                        ->setCategory($selectedCategory)
                        ->setDeliveryTime($delivery)
                        ->setImage($values['image'])
                        ->setStatus(strtoupper($values['status']));

                    if (!$isEdit) {
                        $entity->setUserId($user->getId());
                        $entityManager->persist($entity);
                    }

                    $entityManager->flush();
                    $this->addFlash('success', $isEdit ? 'Gig updated successfully.' : 'Gig created successfully.');

                    return $this->redirectToRoute('app_gigs');
                }
            }
        }

        return $this->render('gig/form.html.twig', [
            'avatar_proxy_enabled' => $blobStorageService->isConfigured() && $user->hasProfileImage(),
            'is_edit' => $isEdit,
            'is_expired' => $isExpired,
            'gig' => $entity,
            'values' => $values,
            'errors' => $errors,
            'ai_status' => $aiStatus,
            'categories' => $categories,
        ]);
    }

    /**
     * @return array{query:string, category_id:?int, status:string, min_price:?float, max_price:?float}
     */
    private function extractFilters(Request $request): array
    {
        $status = strtoupper(trim((string) $request->query->get('status', 'ALL')));
        if (!in_array($status, ['ALL', 'ACTIVE', 'INACTIVE', 'EXPIRED'], true)) {
            $status = 'ALL';
        }

        return [
            'query' => trim((string) $request->query->get('q', '')),
            'category_id' => $this->toNullableInt($request->query->get('category_id')),
            'status' => $status,
            'min_price' => $this->toNullableFloat($request->query->get('min_price')),
            'max_price' => $this->toNullableFloat($request->query->get('max_price')),
        ];
    }

    /**
     * @param Category[] $categories
     */
    private function findCategoryById(?int $id, array $categories): ?Category
    {
        if (null === $id) {
            return null;
        }

        foreach ($categories as $category) {
            if ($category->getId() === $id) {
                return $category;
            }
        }

        return null;
    }

    /**
     * @param Category[] $categories
     */
    private function findCategoryNameById(?int $id, array $categories): ?string
    {
        $category = $this->findCategoryById($id, $categories);

        return $category?->getName();
    }

    /**
     * @param Category[] $categories
     */
    private function findCategoryIdByName(string $name, array $categories): ?int
    {
        $normalized = mb_strtolower(trim($name));
        if ('' === $normalized) {
            return null;
        }

        foreach ($categories as $category) {
            if (mb_strtolower($category->getName()) === $normalized) {
                return $category->getId();
            }
        }

        foreach ($categories as $category) {
            $candidate = mb_strtolower($category->getName());
            if (str_contains($candidate, $normalized) || str_contains($normalized, $candidate)) {
                return $category->getId();
            }
        }

        return null;
    }

    private function parseDeliveryDateTime(string $date, int $hour): ?\DateTimeImmutable
    {
        if ('' === trim($date)) {
            return null;
        }

        try {
            return new \DateTimeImmutable(sprintf('%s %02d:00:00', $date, $hour));
        } catch (\Throwable) {
            return null;
        }
    }

    private function parsePrice(string $value): ?float
    {
        $normalized = str_replace(',', '.', trim($value));
        if ('' === $normalized || !is_numeric($normalized)) {
            return null;
        }

        return (float) $normalized;
    }

    private function toNullableFloat(mixed $value): ?float
    {
        if (null === $value) {
            return null;
        }

        $normalized = str_replace(',', '.', trim((string) $value));
        if ('' === $normalized || !is_numeric($normalized)) {
            return null;
        }

        return (float) $normalized;
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
