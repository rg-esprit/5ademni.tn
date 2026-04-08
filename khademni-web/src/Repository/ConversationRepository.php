<?php

namespace App\Repository;

use App\Entity\Conversation;
use App\Entity\User;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<Conversation>
 */
class ConversationRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, Conversation::class);
    }

    /**
     * @return Conversation[]
     */
    public function findForUser(User $user, ?string $search = null, string $filter = 'Toutes'): array
    {
        $qb = $this->createQueryBuilder('c')
            ->leftJoin('c.client', 'client')
            ->leftJoin('c.freelance', 'freelance')
            ->leftJoin('c.members', 'member')
            ->leftJoin('member.user', 'memberUser')
            ->addSelect('client', 'freelance')
            ->where('c.client = :user OR c.freelance = :user OR memberUser = :user')
            ->setParameter('user', $user)
            ->distinct()
            ->orderBy('c.dateCreation', 'DESC');

        if (null !== $search && '' !== trim($search)) {
            $pattern = '%'.mb_strtolower(trim($search)).'%';
            $qb->andWhere(
                $qb->expr()->orX(
                    'LOWER(c.title) LIKE :pattern',
                    'LOWER(CONCAT(client.firstName, ' . "' '" . ', client.lastName)) LIKE :pattern',
                    'LOWER(CONCAT(freelance.firstName, ' . "' '" . ', freelance.lastName)) LIKE :pattern'
                )
            )
            ->setParameter('pattern', $pattern);
        }

        if ('Actives' === $filter) {
            $qb->andWhere('c.statut = :activeStatus')
                ->setParameter('activeStatus', 'ACTIVE');
        }

        if ('Inactives' === $filter) {
            $qb->andWhere('c.statut <> :activeStatus')
                ->setParameter('activeStatus', 'ACTIVE');
        }

        if ('Non lues' === $filter) {
            $qb->andWhere('(c.client = :user AND c.nonLusClient > 0) OR (c.freelance = :user AND c.nonLusFreelance > 0)');
        }

        return $qb->getQuery()->getResult();
    }

    public function findOneForUser(int $conversationId, User $user): ?Conversation
    {
        // Allow admins to access all conversations for management
        if (in_array('ROLE_ADMIN', $user->getRoles(), true)) {
            return $this->find($conversationId);
        }

        return $this->createQueryBuilder('c')
            ->leftJoin('c.members', 'member')
            ->leftJoin('member.user', 'memberUser')
            ->where('c.id = :id')
            ->andWhere('c.client = :user OR c.freelance = :user OR memberUser = :user')
            ->setParameter('id', $conversationId)
            ->setParameter('user', $user)
            ->getQuery()
            ->getOneOrNullResult();
    }

    public function findExistingConversation(User $first, User $second): ?Conversation
    {
        return $this->createQueryBuilder('c')
            ->where('(c.client = :first AND c.freelance = :second) OR (c.client = :second AND c.freelance = :first)')
            ->orWhere('EXISTS(SELECT m1 FROM App\\Entity\\ConversationMember m1 WHERE m1.conversation = c AND m1.user = :first) AND EXISTS(SELECT m2 FROM App\\Entity\\ConversationMember m2 WHERE m2.conversation = c AND m2.user = :second)')
            ->setParameter('first', $first)
            ->setParameter('second', $second)
            ->distinct()
            ->getQuery()
            ->getOneOrNullResult();
    }
}
