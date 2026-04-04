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
        int $minSalary   = 0
    ): array {
        $qb = $this->createQueryBuilder('j')
            ->leftJoin('j.applications', 'a')
            ->addSelect('COUNT(a.id) AS HIDDEN appCount');

        if ($query !== '') {
            $pattern = '%' . strtolower($query) . '%';
            $qb->andWhere(
                'LOWER(j.title) LIKE :q OR LOWER(j.company) LIKE :q OR LOWER(j.description) LIKE :q OR LOWER(j.requirements) LIKE :q'
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

        $qb->groupBy('j.id');

        match ($sort) {
            'oldest'       => $qb->orderBy('j.postedDate', 'ASC'),
            'most_applied' => $qb->orderBy('appCount', 'DESC'),
            default        => $qb->orderBy('j.postedDate', 'DESC'),
        };

        $results = $qb->getQuery()->getResult();

        if ($minSalary > 0) {
            $results = array_filter($results, function (Job $job) use ($minSalary) {
                $salaryRange = $job->getSalaryRange();
                if (!$salaryRange) return false;
                // extract first number from salary string
                if (preg_match('/\d+/', $salaryRange, $matches)) {
                    return (int)$matches[0] >= $minSalary;
                }
                return false;
            });
        }

        return $results;
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
}
