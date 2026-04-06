<?php

namespace App\Repository;

use App\Entity\Gig;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

class GigRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, Gig::class);
    }

    public function findByArticleKeywords(string $keywords): array
{
    $words = explode(' ', $keywords);
    $qb = $this->createQueryBuilder('g');

    foreach ($words as $index => $word) {
        $qb->orWhere('g.title LIKE :word' . $index)
           ->orWhere('g.description LIKE :word' . $index)
           ->setParameter('word' . $index, '%' . $word . '%');
    }

    return $qb->getQuery()->getResult();
}
}