<?php

namespace App\Repository;

use App\Entity\Favori;
use App\Entity\User;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<Favori>
 */
class FavoriRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, Favori::class);
    }

       public function countByArticleIds(array $articleIds): array
    {
        if ($articleIds === []) {
            return [];
        }

        $rows = $this->createQueryBuilder('f')
            ->select('IDENTITY(f.article) AS articleId, COUNT(f.id) AS total')
            ->andWhere('f.article IN (:ids)')
            ->setParameter('ids', $articleIds)
            ->groupBy('f.article')
            ->getQuery()
            ->getArrayResult();

        $map = [];
        foreach ($rows as $row) {
            $map[(int) $row['articleId']] = (int) $row['total'];
        }

        return $map;
    }

    public function findArticleIdsByUser(User $user, array $articleIds = []): array
    {
        $qb = $this->createQueryBuilder('f')
            ->select('IDENTITY(f.article) AS articleId')
            ->andWhere('f.user = :user')
            ->setParameter('user', $user);

        if ($articleIds !== []) {
            $qb->andWhere('f.article IN (:ids)')
               ->setParameter('ids', $articleIds);
        }

        $rows = $qb->getQuery()->getArrayResult();

        return array_map(fn (array $r) => (int) $r['articleId'], $rows);
    }
}