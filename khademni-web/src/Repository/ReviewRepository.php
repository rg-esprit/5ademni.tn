<?php

namespace App\Repository;

use App\Entity\Review;
use App\Entity\User;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\ORM\QueryBuilder;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<Review>
 */
class ReviewRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, Review::class);
    }

    /**
     * @return Review[]
     */
    public function findReceivedForUser(User $user): array
    {
        return $this->createQueryBuilder('review')
            ->addSelect('client', 'freelancer')
            ->join('review.client', 'client')
            ->join('review.freelancer', 'freelancer')
            ->andWhere('review.freelancer = :user')
            ->setParameter('user', $user)
            ->orderBy('review.id', 'DESC')
            ->getQuery()
            ->getResult();
    }

    /**
     * @return Review[]
     */
    public function findGivenForUser(User $user): array
    {
        return $this->createQueryBuilder('review')
            ->addSelect('client', 'freelancer')
            ->join('review.client', 'client')
            ->join('review.freelancer', 'freelancer')
            ->andWhere('review.client = :user')
            ->setParameter('user', $user)
            ->orderBy('review.id', 'DESC')
            ->getQuery()
            ->getResult();
    }

    public function findEditableReviewForClient(int $reviewId, User $client): ?Review
    {
        return $this->createQueryBuilder('review')
            ->addSelect('reviewClient', 'freelancer')
            ->join('review.client', 'reviewClient')
            ->join('review.freelancer', 'freelancer')
            ->andWhere('review.id = :reviewId')
            ->andWhere('review.client = :client')
            ->setParameter('reviewId', $reviewId)
            ->setParameter('client', $client)
            ->getQuery()
            ->getOneOrNullResult();
    }

    public function existsForClientAndFreelancer(User $client, User $freelancer, ?int $excludeId = null): bool
    {
        $queryBuilder = $this->createQueryBuilder('review')
            ->select('COUNT(review.id)')
            ->andWhere('review.client = :client')
            ->andWhere('review.freelancer = :freelancer')
            ->setParameter('client', $client)
            ->setParameter('freelancer', $freelancer);

        if (null !== $excludeId) {
            $queryBuilder
                ->andWhere('review.id != :excludeId')
                ->setParameter('excludeId', $excludeId);
        }

        return (int) $queryBuilder->getQuery()->getSingleScalarResult() > 0;
    }

    public function countReceivedForUser(User $user): int
    {
        return (int) $this->createQueryBuilder('review')
            ->select('COUNT(review.id)')
            ->andWhere('review.freelancer = :user')
            ->setParameter('user', $user)
            ->getQuery()
            ->getSingleScalarResult();
    }

    public function countGivenForUser(User $user): int
    {
        return (int) $this->createQueryBuilder('review')
            ->select('COUNT(review.id)')
            ->andWhere('review.client = :user')
            ->setParameter('user', $user)
            ->getQuery()
            ->getSingleScalarResult();
    }

    /**
     * @param array{query:string, rating:?int} $filters
     *
     * @return array{items: Review[], total: int, page: int, pages: int, per_page: int, has_previous: bool, has_next: bool}
     */
    public function paginateReceivedForUser(User $user, array $filters, int $page, int $perPage = 5): array
    {
        return $this->paginate($this->createFilteredQueryBuilder($user, true, $filters), $page, $perPage);
    }

    /**
     * @param array{query:string, rating:?int} $filters
     *
     * @return array{items: Review[], total: int, page: int, pages: int, per_page: int, has_previous: bool, has_next: bool}
     */
    public function paginateGivenForUser(User $user, array $filters, int $page, int $perPage = 5): array
    {
        return $this->paginate($this->createFilteredQueryBuilder($user, false, $filters), $page, $perPage);
    }

    /**
     * @param array{query:string, rating:?int} $filters
     */
    private function createFilteredQueryBuilder(User $user, bool $received, array $filters): QueryBuilder
    {
        $counterpartAlias = $received ? 'client' : 'freelancer';
        $queryBuilder = $this->createQueryBuilder('review')
            ->addSelect('client', 'freelancer')
            ->join('review.client', 'client')
            ->join('review.freelancer', 'freelancer')
            ->andWhere($received ? 'review.freelancer = :user' : 'review.client = :user')
            ->setParameter('user', $user)
            ->orderBy('review.id', 'DESC');

        if (null !== $filters['rating']) {
            $queryBuilder
                ->andWhere('review.rating = :rating')
                ->setParameter('rating', $filters['rating']);
        }

        if ('' !== $filters['query']) {
            $queryBuilder
                ->andWhere(
                    sprintf(
                        'LOWER(review.reviewText) LIKE :pattern OR LOWER(%1$s.firstName) LIKE :pattern OR LOWER(%1$s.lastName) LIKE :pattern OR LOWER(%1$s.email) LIKE :pattern OR LOWER(CONCAT(%1$s.firstName, :space, %1$s.lastName)) LIKE :pattern',
                        $counterpartAlias,
                    ),
                )
                ->setParameter('pattern', '%'.$filters['query'].'%')
                ->setParameter('space', ' ');
        }

        return $queryBuilder;
    }

    /**
     * @return array{items: Review[], total: int, page: int, pages: int, per_page: int, has_previous: bool, has_next: bool}
     */
    private function paginate(QueryBuilder $queryBuilder, int $page, int $perPage): array
    {
        $page = max(1, $page);

        $countQueryBuilder = clone $queryBuilder;
        $countQueryBuilder
            ->select('COUNT(review.id)')
            ->resetDQLPart('orderBy');

        $total = (int) $countQueryBuilder->getQuery()->getSingleScalarResult();
        $pages = max(1, (int) ceil($total / $perPage));
        $page = min($page, $pages);

        $itemsQueryBuilder = clone $queryBuilder;
        $items = $itemsQueryBuilder
            ->setFirstResult(($page - 1) * $perPage)
            ->setMaxResults($perPage)
            ->getQuery()
            ->getResult();

        return [
            'items' => $items,
            'total' => $total,
            'page' => $page,
            'pages' => $pages,
            'per_page' => $perPage,
            'has_previous' => $page > 1,
            'has_next' => $page < $pages,
        ];
    }
}
