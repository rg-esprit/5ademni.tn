<?php

namespace App\Repository;

use App\Entity\User;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<User>
 */
class UserRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, User::class);
    }

    public function emailExists(string $email, ?int $excludeId = null): bool
    {
        $queryBuilder = $this->createQueryBuilder('user')
            ->select('COUNT(user.id)')
            ->andWhere('LOWER(user.email) = LOWER(:email)')
            ->setParameter('email', trim($email));

        if (null !== $excludeId) {
            $queryBuilder
                ->andWhere('user.id != :excludeId')
                ->setParameter('excludeId', $excludeId);
        }

        return (int) $queryBuilder->getQuery()->getSingleScalarResult() > 0;
    }

    /**
     * @return User[]
     */
    public function searchReviewTargets(string $query, User $currentUser, int $limit = 8): array
    {
        $pattern = '%'.strtolower(trim($query)).'%';

        return $this->createQueryBuilder('user')
            ->andWhere('user != :currentUser')
            ->andWhere('LOWER(user.firstName) LIKE :pattern OR LOWER(user.lastName) LIKE :pattern OR LOWER(user.email) LIKE :pattern OR LOWER(CONCAT(user.firstName, :space, user.lastName)) LIKE :pattern')
            ->setParameter('currentUser', $currentUser)
            ->setParameter('pattern', $pattern)
            ->setParameter('space', ' ')
            ->orderBy('user.firstName', 'ASC')
            ->addOrderBy('user.lastName', 'ASC')
            ->setMaxResults($limit)
            ->getQuery()
            ->getResult();
    }
}
