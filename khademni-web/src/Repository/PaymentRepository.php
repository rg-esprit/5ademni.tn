<?php

namespace App\Repository;

use App\Entity\Payment;
use App\Entity\User;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\ORM\QueryBuilder;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<Payment>
 *
 * @method Payment|null find($id, $lockMode = null, $lockVersion = null)
 * @method Payment|null findOneBy(array $criteria, array $orderBy = null)
 * @method Payment[]    findAll()
 * @method Payment[]    findBy(array $criteria, array $orderBy = null, $limit = null, $offset = null)
 */
class PaymentRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, Payment::class);
    }

    /**
     * @return Payment[]
     */
    public function searchPayments(string $search): array
    {
        return $this->createQueryBuilder('p')
            ->leftJoin('p.contrat', 'c')
            ->where('p.status LIKE :search')
            ->orWhere('c.titre LIKE :search')
            ->setParameter('search', '%' . $search . '%')
            ->orderBy('p.createdAt', 'DESC')
            ->getQuery()
            ->getResult();
    }

    /**
     * Returns payments linked to contracts where the user is client or freelancer.
     * Optionally filters by search term. NO database changes — read-only query.
     *
     * @return Payment[]
     */
    public function findByUser(User $user, ?string $search = null): array
    {
        return $this->findByUserQueryBuilder($user, $search)->getQuery()->getResult();
    }

    /**
     * Same as findByUser() but returns a QueryBuilder for use with KnpPaginatorBundle.
     */
    public function findByUserQueryBuilder(User $user, ?string $search = null): QueryBuilder
    {
        $qb = $this->createQueryBuilder('p')
            ->join('p.contrat', 'c')
            ->where('c.clientId = :userId OR c.freelancerId = :userId')
            ->setParameter('userId', $user->getId())
            ->orderBy('p.createdAt', 'DESC');

        if ($search) {
            $qb->andWhere('p.status LIKE :search OR c.titre LIKE :search')
               ->setParameter('search', '%' . $search . '%');
        }

        return $qb;
    }

    /**
     * Returns all payments as a QueryBuilder for admin pagination.
     */
    public function findAllQueryBuilder(?string $search = null): QueryBuilder
    {
        $qb = $this->createQueryBuilder('p')
            ->leftJoin('p.contrat', 'c')
            ->orderBy('p.createdAt', 'DESC');

        if ($search) {
            $qb->where('p.status LIKE :search OR c.titre LIKE :search')
               ->setParameter('search', '%' . $search . '%');
        }

        return $qb;
    }
}

