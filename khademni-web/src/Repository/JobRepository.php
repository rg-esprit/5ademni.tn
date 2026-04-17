<?php

namespace App\Repository;

use App\Entity\Job;
use App\Entity\User;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<Job>
 */
class JobRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, Job::class);
    }

    public function findPostedByUser(User $user): array
    {
        return $this->createQueryBuilder('j')
            ->andWhere('j.user = :user')
            ->setParameter('user', $user)
            ->orderBy('j.postedDate', 'DESC')
            ->getQuery()
            ->getResult();
    }

    public function searchJobs(
        string $query    = '',
        string $category = '',
        string $location = '',
        string $jobType  = '',
        string $sort     = 'newest',
        int $minSalary   = 0,
        int $maxSalary   = 1000000
    ): array {
        $qb = $this->createQueryBuilder('j')
            ->leftJoin('j.applications', 'a')
            ->leftJoin('j.savedByUsers', 's')
            ->leftJoin('j.user', 'u')
            ->addSelect('COUNT(DISTINCT a.id) AS HIDDEN appCount')
            ->addSelect('COUNT(DISTINCT s.id) AS HIDDEN saveCount');

        if ($query !== '') {
            $pattern = '%' . strtolower($query) . '%';
            $qb->andWhere(
                'LOWER(j.title) LIKE :q OR LOWER(j.company) LIKE :q OR LOWER(j.location) LIKE :q OR LOWER(u.firstName) LIKE :q OR LOWER(u.lastName) LIKE :q'
            )->setParameter('q', $pattern);
        }

        if ($category !== '') {
            $qb->andWhere('j.category = :category')
               ->setParameter('category', $category);
        }

        if ($location !== '') {
            $qb->andWhere('LOWER(j.location) LIKE :location')
               ->setParameter('location', '%' . strtolower($location) . '%');
        }

        if ($jobType !== '') {
            $qb->andWhere('j.jobType = :jobType')
               ->setParameter('jobType', $jobType);
        }

        if ($minSalary > 0 || $maxSalary < 1000000) {
            if ($minSalary > 0) {
                $qb->andWhere('j.maxSalary >= :minSal')
                   ->setParameter('minSal', $minSalary);
            }
            if ($maxSalary < 1000000) {
                $qb->andWhere('j.minSalary <= :maxSal')
                   ->setParameter('maxSal', $maxSalary);
            }
        }

        $qb->groupBy('j.id');

        match ($sort) {
            'oldest'     => $qb->orderBy('j.postedDate', 'ASC'),
            'most_popular' => $qb->orderBy('appCount', 'DESC'),
            'most_liked' => $qb->orderBy('saveCount', 'DESC'),
            default      => $qb->orderBy('j.postedDate', 'DESC'),
        };

        return $qb->getQuery()->getResult();
    }

    public function getMinMaxSalaries(): array
    {
        return $this->createQueryBuilder('j')
            ->select('MIN(j.minSalary) as minSal, MAX(j.maxSalary) as maxSal')
            ->getQuery()
            ->getSingleResult();
    }

    public function countTotalSavesForUserJobs(User $user): int
    {
        return (int) $this->getEntityManager()->createQueryBuilder()
            ->select('COUNT(u.id)')
            ->from(User::class, 'u')
            ->join('u.savedJobs', 'j')
            ->where('j.user = :owner')
            ->setParameter('owner', $user)
            ->getQuery()
            ->getSingleScalarResult();
    }

    public function countAllSaves(): int
    {
        return (int) $this->getEntityManager()->createQueryBuilder()
            ->select('COUNT(u.id)')
            ->from(User::class, 'u')
            ->join('u.savedJobs', 'j')
            ->getQuery()
            ->getSingleScalarResult();
    }
}
