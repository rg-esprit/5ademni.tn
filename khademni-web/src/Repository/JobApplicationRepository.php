<?php

namespace App\Repository;

use App\Entity\JobApplication;
use App\Entity\Job;
use App\Entity\User;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<JobApplication>
 */
class JobApplicationRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, JobApplication::class);
    }

    public function findByUser(User $user): array
    {
        return $this->createQueryBuilder('a')
            ->andWhere('a.user = :user')
            ->setParameter('user', $user)
            ->orderBy('a.applicationDate', 'DESC')
            ->getQuery()
            ->getResult();
    }

    public function findByJob(Job $job): array
    {
        return $this->createQueryBuilder('a')
            ->andWhere('a.job = :job')
            ->setParameter('job', $job)
            ->orderBy('a.applicationDate', 'DESC')
            ->getQuery()
            ->getResult();
    }

    public function findByJobAndUser(Job $job, User $user): ?JobApplication
    {
        return $this->createQueryBuilder('a')
            ->andWhere('a.job = :job')
            ->andWhere('a.user = :user')
            ->setParameter('job', $job)
            ->setParameter('user', $user)
            ->getQuery()
            ->getOneOrNullResult();
    }

    public function findAcceptedForJob(Job $job): ?JobApplication
    {
        return $this->createQueryBuilder('a')
            ->andWhere('a.job = :job')
            ->andWhere('a.status IN (:statuses)')
            ->setParameter('job', $job)
            ->setParameter('statuses', [
                JobApplication::STATUS_ACCEPTED,
                JobApplication::STATUS_IN_PROGRESS,
                JobApplication::STATUS_COMPLETED
            ])
            ->setMaxResults(1)
            ->getQuery()
            ->getOneOrNullResult();
    }
}
