<?php


namespace App\Repository;

use App\Entity\Article;
use App\Entity\Commentaire;
use App\Entity\Favori;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

class ArticleRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, Article::class);
    }

    public function findAllWithStats(): array
    {
        return $this->createQueryBuilder('a')
            ->leftJoin('a.favoris', 'f')
            ->leftJoin('a.commentaires', 'c')
            ->addSelect('COUNT(DISTINCT f.id) AS favorisCount')
            ->addSelect('COUNT(DISTINCT c.id) AS commentairesCount')
            ->groupBy('a.id')
            ->orderBy('a.createdAt', 'DESC')
            ->getQuery()
            ->getResult();
    }

    public function getStatusStats(): array
    {
        $rows = $this->createQueryBuilder('a')
            ->select('a.status AS status, COUNT(a.id) AS total')
            ->groupBy('a.status')
            ->getQuery()
            ->getArrayResult();

        $stats = ['VISIBLE' => 0, 'MASQUE' => 0];
        foreach ($rows as $row) {
            $stats[strtoupper((string) $row['status'])] = (int) $row['total'];
        }

        return $stats;
    }


    public function findVisibleOrderedBy(?string $sort = 'favoris'): array
    {
        $qb = $this->createQueryBuilder('a')
            ->leftJoin('a.favoris', 'f')
            ->leftJoin('a.commentaires', 'c')
            ->andWhere('UPPER(a.status) = :status')
            ->setParameter('status', 'VISIBLE')
            ->groupBy('a.id');

        if ($sort === 'commentaires') {
            $qb->addSelect('COUNT(DISTINCT c.id) AS HIDDEN commentaires')
                ->orderBy('commentaires', 'DESC');
        } else {
            $qb->addSelect('COUNT(DISTINCT f.id) AS HIDDEN favoris')
                ->orderBy('favoris', 'DESC');
        }

        return $qb->addOrderBy('a.createdAt', 'DESC')
            ->getQuery()
            ->getResult();
    }



    public function findAllWithFilters(string $search = '', string $status = ''): array
    {
        $qb = $this->createQueryBuilder('a')
            ->leftJoin('a.favoris', 'f')
            ->leftJoin('a.commentaires', 'c')
            ->addSelect('COUNT(f) as favorisCount')
            ->addSelect('COUNT(c) as commentairesCount')
            ->groupBy('a.id');

        // Appliquer le filtre de recherche par titre
        if (!empty($search)) {
            $qb->andWhere('a.title LIKE :search')
                ->setParameter('search', '%' . $search . '%');
        }

        // Appliquer le filtre par statut
        if (!empty($status)) {
            $qb->andWhere('a.status = :status')
                ->setParameter('status', $status);
        }

        return $qb->getQuery()->getResult();
    }

  
    public function createQueryBuilderForVisibleOrderedBy(string $sort, string $q = ''): \Doctrine\ORM\QueryBuilder
    {
        $qb = $this->createQueryBuilder('a')
            ->leftJoin('a.favoris', 'f')
            ->leftJoin('a.commentaires', 'c')
            ->where('UPPER(a.status) = :status')
            ->setParameter('status', 'VISIBLE')
            ->groupBy('a.id');

        if ($q !== '') {
            $qb->andWhere('a.title LIKE :q OR a.content LIKE :q')
               ->setParameter('q', '%' . $q . '%');
        }

        if ($sort === 'favoris') {
            $qb->addSelect('COUNT(DISTINCT f.id) AS HIDDEN favoris')
               ->orderBy('favoris', 'DESC');
        } elseif ($sort === 'commentaires') {
            $qb->addSelect('COUNT(DISTINCT c.id) AS HIDDEN commentaires')
               ->orderBy('commentaires', 'DESC');
        } else {
            $qb->orderBy('a.createdAt', 'DESC');
        }

        return $qb;
    }
}
