<?php

namespace App\Repository;

use App\Entity\Job;
use App\Entity\User;
use App\Entity\WorkLog;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<WorkLog>
 */
class WorkLogRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, WorkLog::class);
    }

    public function findLogsForJob(Job $job): array
    {
        return $this->createQueryBuilder('w')
            ->andWhere('w.job = :job')
            ->setParameter('job', $job)
            ->orderBy('w.createdAt', 'DESC')
            ->getQuery()
            ->getResult();
    }

    public function getTotalProgressForJobAndFreelancer(Job $job, User $freelancer): int
    {
        $result = $this->createQueryBuilder('w')
            ->select('SUM(w.progressChange)')
            ->andWhere('w.job = :job')
            ->andWhere('w.freelancer = :freelancer')
            ->setParameter('job', $job)
            ->setParameter('freelancer', $freelancer)
            ->getQuery()
            ->getSingleScalarResult();

        return min(100, (int) $result);
    }
}
