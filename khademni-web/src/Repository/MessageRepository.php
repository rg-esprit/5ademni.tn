<?php

namespace App\Repository;

use App\Entity\Conversation;
use App\Entity\Message;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<Message>
 */
class MessageRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, Message::class);
    }

    /**
     * @return Message[]
     */
    public function findByConversation(Conversation $conversation): array
    {
        return $this->createQueryBuilder('message')
            ->andWhere('message.conversation = :conversation')
            ->setParameter('conversation', $conversation)
            ->orderBy('message.dateEnvoi', 'ASC')
            ->addOrderBy('message.id', 'ASC')
            ->getQuery()
            ->getResult();
    }

    /**
     * @return Message[]
     */
    public function findRecentByConversation(Conversation $conversation, int $limit = 50): array
    {
        return $this->createQueryBuilder('message')
            ->andWhere('message.conversation = :conversation')
            ->setParameter('conversation', $conversation)
            ->orderBy('message.dateEnvoi', 'DESC')
            ->addOrderBy('message.id', 'DESC')
            ->setMaxResults($limit)
            ->getQuery()
            ->getResult();
    }

    /**
     * @return Message[]
     */
    public function findByConversationSinceId(Conversation $conversation, int $since): array
    {
        return $this->createQueryBuilder('message')
            ->andWhere('message.conversation = :conversation')
            ->andWhere('message.id > :since')
            ->setParameter('conversation', $conversation)
            ->setParameter('since', $since)
            ->orderBy('message.dateEnvoi', 'ASC')
            ->addOrderBy('message.id', 'ASC')
            ->getQuery()
            ->getResult();
    }
}
