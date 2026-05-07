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
        int $maxSalary   = 1000000,
        int $limit       = 20
    ): array {
        $qb = $this->createQueryBuilder('j')
            ->leftJoin('j.user', 'u')
            ->addSelect('u');

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

        if ('most_liked' === $sort) {
            $qb->leftJoin('j.savedByUsers', 's')
                ->addSelect('COUNT(DISTINCT s.id) AS HIDDEN saveCount')
                ->groupBy('j.id')
                ->orderBy('saveCount', 'DESC');
        } else {
            match ($sort) {
                'oldest' => $qb->orderBy('j.postedDate', 'ASC'),
                default => $qb->orderBy('j.postedDate', 'DESC'),
            };
        }

        $results = $qb->setMaxResults($limit)->getQuery()->getResult();

        if ($minSalary > 0 || $maxSalary < 1000000) {
            $results = array_values(array_filter($results, function (Job $job) use ($minSalary, $maxSalary): bool {
                [$jobMinSalary, $jobMaxSalary] = $this->extractSalaryBounds($job->getSalaryRange());

                if (null === $jobMinSalary && null === $jobMaxSalary) {
                    return false;
                }

                $effectiveMin = $jobMinSalary ?? $jobMaxSalary;
                $effectiveMax = $jobMaxSalary ?? $jobMinSalary;

                if (null === $effectiveMin || null === $effectiveMax) {
                    return false;
                }

                return $effectiveMax >= $minSalary && $effectiveMin <= $maxSalary;
            }));
        }

        return $results;
    }

    public function getMinMaxSalaries(): array
    {
        $rows = $this->createQueryBuilder('j')
            ->select('j.salaryRange AS salaryRange')
            ->getQuery()
            ->getArrayResult();

        $minimums = [];
        $maximums = [];

        foreach ($rows as $row) {
            [$minSalary, $maxSalary] = $this->extractSalaryBounds($row['salaryRange'] ?? null);

            if (null !== $minSalary) {
                $minimums[] = $minSalary;
            }

            if (null !== $maxSalary) {
                $maximums[] = $maxSalary;
            } elseif (null !== $minSalary) {
                $maximums[] = $minSalary;
            }
        }

        return [
            'minSal' => [] === $minimums ? 0 : min($minimums),
            'maxSal' => [] === $maximums ? 10000 : max($maximums),
        ];
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

    private function extractSalaryBounds(?string $salaryRange): array
    {
        if (null === $salaryRange || '' === trim($salaryRange)) {
            return [null, null];
        }

        preg_match_all('/\d+(?:\.\d+)?/', $salaryRange, $matches);
        $values = $matches[0] ?? [];

        if ([] === $values) {
            return [null, null];
        }

        $minSalary = (float) $values[0];
        $maxSalary = isset($values[1]) ? (float) $values[1] : $minSalary;

        return [$minSalary, $maxSalary];
    }
}
