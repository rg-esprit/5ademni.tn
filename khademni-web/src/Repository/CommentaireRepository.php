<?php

namespace App\Repository;
use App\Entity\Article;
use App\Entity\Commentaire;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<Commentaire>
 */
class CommentaireRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, Commentaire::class);
    }

        public function countByArticleIds(array $articleIds): array
    {
        if ($articleIds === []) {
            return [];
        }

        $rows = $this->createQueryBuilder('c')
            ->select('IDENTITY(c.article) AS articleId, COUNT(c.id) AS total')
            ->andWhere('c.article IN (:ids)')
            ->andWhere('UPPER(c.status) = :status')
            ->setParameter('ids', $articleIds)
            ->setParameter('status', 'VISIBLE')
            ->groupBy('c.article')
            ->getQuery()
            ->getArrayResult();

        $map = [];
        foreach ($rows as $row) {
            $map[(int) $row['articleId']] = (int) $row['total'];
        }

        return $map;
    }

    public function findVisibleByArticle(Article $article): array
    {
        return $this->createQueryBuilder('c')
            ->leftJoin('c.user', 'u')->addSelect('u')
            ->andWhere('c.article = :article')
            ->andWhere('UPPER(c.status) = :status')
            ->setParameter('article', $article)
            ->setParameter('status', 'VISIBLE')
            ->orderBy('c.createdAt', 'DESC')
            ->getQuery()
            ->getResult();
    }
}