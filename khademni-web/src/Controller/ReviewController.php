<?php

namespace App\Controller;

use App\Entity\Review;
use App\Entity\User;
use App\Repository\ReviewRepository;
use App\Repository\UserRepository;
use App\Service\BlobStorageService;
use App\Service\ReviewAiService;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;
use Symfony\Contracts\HttpClient\HttpClientInterface;

class ReviewController extends AbstractController
{
    #[Route('/reviews', name: 'app_reviews', methods: ['GET'])]
    public function index(
        Request $request,
        ReviewRepository $reviewRepository,
        UserRepository $userRepository,
        BlobStorageService $blobStorageService,
    ): Response {
        $user = $this->getCurrentUser();

        if (!$user instanceof User) {
            return $this->redirectToRoute('app_login');
        }

        $filters = $this->extractFilters($request);
        $paginationState = $this->extractPaginationState($request);
        $userSearch = trim((string) $request->query->get('user_search', ''));
        $editingReview = null;
        $selectedTarget = null;
        $searchResults = [];
        $formData = $this->defaultFormData();
        $editId = $request->query->getInt('edit');

        if ($editId > 0) {
            $editingReview = $reviewRepository->findEditableReviewForClient($editId, $user);

            if (!$editingReview instanceof Review) {
                $this->addFlash('error', 'That review could not be edited.');

                return $this->redirectToRoute('app_reviews', $this->buildReviewQuery($filters, $paginationState));
            }

            $selectedTarget = $editingReview->getFreelancer();
            $formData = [
                'review_id' => $editingReview->getId(),
                'freelancer_id' => $selectedTarget?->getId(),
                'rating' => $editingReview->getRating(),
                'review_text' => $editingReview->getReviewText(),
                'ai_prompt' => '',
            ];
        } else {
            $selectedTarget = $this->resolveSelectedTarget($request->query->getInt('selected_user'), $user, $userRepository);
            $searchResults = $this->loadSearchResults($userSearch, $user, $userRepository, $selectedTarget);
        }

        return $this->renderReviewPage(
            $user,
            $reviewRepository,
            $blobStorageService,
            $filters,
            $paginationState,
            $formData,
            $selectedTarget,
            $editingReview,
            null,
            $userSearch,
            $searchResults,
        );
    }

    #[Route('/reviews/save', name: 'app_reviews_save', methods: ['POST'])]
    public function save(
        Request $request,
        ReviewRepository $reviewRepository,
        UserRepository $userRepository,
        EntityManagerInterface $entityManager,
        BlobStorageService $blobStorageService,
        HttpClientInterface $httpClient,
        ReviewAiService $reviewAiService,
    ): Response {
        $user = $this->getCurrentUser();

        if (!$user instanceof User) {
            return $this->redirectToRoute('app_login');
        }

        $filters = $this->extractFilters($request);
        $paginationState = $this->extractPaginationState($request);
        $userSearch = trim((string) $request->request->get('user_search', ''));
        $reviewAction = (string) $request->request->get('review_action', 'save');

        if (!$this->isCsrfTokenValid('review_form', (string) $request->request->get('_token'))) {
            $this->addFlash('error', 'The review form expired. Please try again.');

            return $this->redirectToRoute('app_reviews', $this->buildReviewQuery($filters, $paginationState));
        }

        $reviewId = $request->request->getInt('review_id');
        $rating = $request->request->getInt('rating');
        $reviewText = trim((string) $request->request->get('review_text'));
        $aiPrompt = trim((string) $request->request->get('ai_prompt'));
        $editingReview = $reviewId > 0 ? $reviewRepository->findEditableReviewForClient($reviewId, $user) : null;

        if ($reviewId > 0 && !$editingReview instanceof Review) {
            $this->addFlash('error', 'That review could not be updated.');

            return $this->redirectToRoute('app_reviews', $this->buildReviewQuery($filters, $paginationState));
        }

        $selectedTarget = null;
        if ($editingReview instanceof Review) {
            $selectedTarget = $editingReview->getFreelancer();
        } else {
            $selectedTarget = $this->resolveSelectedTarget($request->request->getInt('freelancer_id'), $user, $userRepository);
        }

        $searchResults = $this->loadSearchResults($userSearch, $user, $userRepository, $selectedTarget);

        $formData = [
            'review_id' => $reviewId > 0 ? $reviewId : null,
            'freelancer_id' => $selectedTarget?->getId(),
            'rating' => $rating,
            'review_text' => $reviewText,
            'ai_prompt' => $aiPrompt,
        ];

        if ('generate_ai' === $reviewAction) {
            if ('' === $aiPrompt) {
                return $this->renderReviewPage(
                    $user,
                    $reviewRepository,
                    $blobStorageService,
                    $filters,
                    $paginationState,
                    $formData,
                    $selectedTarget,
                    $editingReview,
                    'Describe what you want the review to say first.',
                    $userSearch,
                    $searchResults,
                );
            }

            try {
                $formData['review_text'] = $reviewAiService->generate($aiPrompt);
            } catch (\RuntimeException $exception) {
                return $this->renderReviewPage(
                    $user,
                    $reviewRepository,
                    $blobStorageService,
                    $filters,
                    $paginationState,
                    $formData,
                    $selectedTarget,
                    $editingReview,
                    $exception->getMessage(),
                    $userSearch,
                    $searchResults,
                );
            }

            $this->addFlash('success', 'Review draft generated. You can edit it before submitting.');

            return $this->renderReviewPage(
                $user,
                $reviewRepository,
                $blobStorageService,
                $filters,
                $paginationState,
                $formData,
                $selectedTarget,
                $editingReview,
                null,
                $userSearch,
                $searchResults,
            );
        }

        if (!$selectedTarget instanceof User || $selectedTarget->getId() === $user->getId()) {
            return $this->renderReviewPage(
                $user,
                $reviewRepository,
                $blobStorageService,
                $filters,
                $paginationState,
                $formData,
                null,
                $editingReview,
                'Please search and select another user to review.',
                $userSearch,
                $searchResults,
            );
        }

        if ($rating < 1 || $rating > 5) {
            return $this->renderReviewPage(
                $user,
                $reviewRepository,
                $blobStorageService,
                $filters,
                $paginationState,
                $formData,
                $selectedTarget,
                $editingReview,
                'Please select a rating between 1 and 5 stars.',
                $userSearch,
                $searchResults,
            );
        }

        if ('' === $reviewText) {
            return $this->renderReviewPage(
                $user,
                $reviewRepository,
                $blobStorageService,
                $filters,
                $paginationState,
                $formData,
                $selectedTarget,
                $editingReview,
                'Please write a review before submitting.',
                $userSearch,
                $searchResults,
            );
        }

        if ($this->containsProfanity($reviewText, $httpClient)) {
            return $this->renderReviewPage(
                $user,
                $reviewRepository,
                $blobStorageService,
                $filters,
                $paginationState,
                $formData,
                $selectedTarget,
                $editingReview,
                'Your review contains inappropriate language. Please revise it before submitting.',
                $userSearch,
                $searchResults,
            );
        }

        if (!$editingReview instanceof Review && $reviewRepository->existsForClientAndFreelancer($user, $selectedTarget)) {
            return $this->renderReviewPage(
                $user,
                $reviewRepository,
                $blobStorageService,
                $filters,
                $paginationState,
                $formData,
                $selectedTarget,
                null,
                'You have already reviewed this user. Edit your existing review instead.',
                $userSearch,
                $searchResults,
            );
        }

        $review = $editingReview ?? (new Review())
            ->setClient($user)
            ->setFreelancer($selectedTarget);

        $review
            ->setRating($rating)
            ->setReviewText($reviewText);

        if (!$editingReview instanceof Review) {
            $entityManager->persist($review);
        }

        $entityManager->flush();

        $this->addFlash('success', $editingReview instanceof Review ? 'Review updated successfully.' : 'Review submitted successfully.');

        return $this->redirectToRoute('app_reviews', $this->buildReviewQuery($filters, $paginationState));
    }

    #[Route('/reviews/{id}/delete', name: 'app_reviews_delete', methods: ['POST'])]
    public function delete(
        int $id,
        Request $request,
        ReviewRepository $reviewRepository,
        EntityManagerInterface $entityManager,
    ): Response {
        $user = $this->getCurrentUser();

        if (!$user instanceof User) {
            return $this->redirectToRoute('app_login');
        }

        $filters = $this->extractFilters($request);
        $paginationState = $this->extractPaginationState($request);

        $review = $reviewRepository->findEditableReviewForClient($id, $user);

        if (!$review instanceof Review) {
            $this->addFlash('error', 'That review could not be deleted.');

            return $this->redirectToRoute('app_reviews', $this->buildReviewQuery($filters, $paginationState));
        }

        if (!$this->isCsrfTokenValid('delete_review_'.$review->getId(), (string) $request->request->get('_token'))) {
            $this->addFlash('error', 'The delete action expired. Please try again.');

            return $this->redirectToRoute('app_reviews', $this->buildReviewQuery($filters, $paginationState));
        }

        $entityManager->remove($review);
        $entityManager->flush();

        $this->addFlash('success', 'Review deleted successfully.');

        return $this->redirectToRoute('app_reviews', $this->buildReviewQuery($filters, $paginationState));
    }

    private function getCurrentUser(): ?User
    {
        $user = $this->getUser();

        return $user instanceof User ? $user : null;
    }

    /**
     * @param array{query:string, rating:?int} $filters
     * @param array{received_page:int, given_page:int} $paginationState
     * @param array{review_id:?int, freelancer_id:?int, rating:int, review_text:string, ai_prompt:string} $formData
     * @param User[] $searchResults
     */
    private function renderReviewPage(
        User $user,
        ReviewRepository $reviewRepository,
        BlobStorageService $blobStorageService,
        array $filters,
        array $paginationState,
        array $formData,
        ?User $selectedTarget,
        ?Review $editingReview,
        ?string $formError = null,
        string $userSearch = '',
        array $searchResults = [],
    ): Response {
        $receivedPagination = $reviewRepository->paginateReceivedForUser($user, $filters, $paginationState['received_page']);
        $givenPagination = $reviewRepository->paginateGivenForUser($user, $filters, $paginationState['given_page']);
        $queryState = $this->buildReviewQuery($filters, [
            'received_page' => $receivedPagination['page'],
            'given_page' => $givenPagination['page'],
        ]);

        return $this->render('review/index.html.twig', [
            'avatar_proxy_enabled' => $blobStorageService->isConfigured() && $user->hasProfileImage(),
            'editing_review' => $editingReview,
            'form_data' => $formData,
            'form_error' => $formError,
            'given_pagination' => $givenPagination,
            'received_reviews_count' => $reviewRepository->countReceivedForUser($user),
            'given_reviews_count' => $reviewRepository->countGivenForUser($user),
            'received_pagination' => $receivedPagination,
            'review_filters' => $filters,
            'review_query' => $queryState,
            'search_results' => $searchResults,
            'selected_target' => $selectedTarget,
            'user_search' => $userSearch,
        ]);
    }

    /**
     * @return array{review_id:?int, freelancer_id:?int, rating:int, review_text:string, ai_prompt:string}
     */
    private function defaultFormData(): array
    {
        return [
            'review_id' => null,
            'freelancer_id' => null,
            'rating' => 0,
            'review_text' => '',
            'ai_prompt' => '',
        ];
    }

    private function containsProfanity(string $text, HttpClientInterface $httpClient): bool
    {
        try {
            $response = $httpClient->request('POST', 'https://vector.profanity.dev', [
                'headers' => [
                    'Content-Type' => 'application/json',
                ],
                'json' => [
                    'message' => $text,
                ],
            ]);

            $data = $response->toArray(false);

            return isset($data['isProfanity']) && true === $data['isProfanity'];
        } catch (\Throwable) {
            return false;
        }
    }

    /**
     * @return array{query:string, rating:?int}
     */
    private function extractFilters(Request $request): array
    {
        $query = strtolower(trim((string) $request->get('filter_q', '')));
        $rating = (int) $request->get('filter_rating', 0);

        return [
            'query' => $query,
            'rating' => $rating >= 1 && $rating <= 5 ? $rating : null,
        ];
    }

    /**
     * @return array{received_page:int, given_page:int}
     */
    private function extractPaginationState(Request $request): array
    {
        return [
            'received_page' => max(1, (int) $request->get('received_page', 1)),
            'given_page' => max(1, (int) $request->get('given_page', 1)),
        ];
    }

    /**
     * @param array{query:string, rating:?int} $filters
     * @param array{received_page:int, given_page:int}|null $paginationState
     *
     * @return array<string, int|string>
     */
    private function buildReviewQuery(array $filters, ?array $paginationState = null): array
    {
        $query = [];

        if ('' !== $filters['query']) {
            $query['filter_q'] = $filters['query'];
        }

        if (null !== $filters['rating']) {
            $query['filter_rating'] = $filters['rating'];
        }

        if (null !== $paginationState) {
            if ($paginationState['received_page'] > 1) {
                $query['received_page'] = $paginationState['received_page'];
            }

            if ($paginationState['given_page'] > 1) {
                $query['given_page'] = $paginationState['given_page'];
            }
        }

        return $query;
    }

    private function resolveSelectedTarget(int $selectedUserId, User $currentUser, UserRepository $userRepository): ?User
    {
        if ($selectedUserId <= 0) {
            return null;
        }

        $selectedTarget = $userRepository->find($selectedUserId);

        if (!$selectedTarget instanceof User || $selectedTarget->getId() === $currentUser->getId()) {
            return null;
        }

        return $selectedTarget;
    }

    /**
     * @return User[]
     */
    private function loadSearchResults(string $userSearch, User $currentUser, UserRepository $userRepository, ?User $selectedTarget = null): array
    {
        if ($selectedTarget instanceof User || strlen($userSearch) < 2) {
            return [];
        }

        return $userRepository->searchReviewTargets($userSearch, $currentUser);
    }
}
