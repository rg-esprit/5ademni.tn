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
    // Diviser les mots-clés en un tableau
    $words = array_filter(explode(' ', $keywords), function ($word) {
        return strlen($word) > 2; // Ignorer les mots trop courts (moins de 3 caractères)
    });

    // Créer une requête
    $qb = $this->createQueryBuilder('g');

    // Ajouter des conditions pour chaque mot-clé
    foreach ($words as $index => $word) {
        $qb->orWhere('LOWER(g.title) LIKE LOWER(:word' . $index . ')')
           ->orWhere('LOWER(g.description) LIKE LOWER(:word' . $index . ')')
           ->setParameter('word' . $index, '%' . $word . '%');
    }

    // Ajouter un tri par pertinence (par exemple, en fonction du nombre de correspondances)
    $qb->addSelect('(
        CASE 
            WHEN LOWER(g.title) LIKE :exactMatch THEN 1
            ELSE 0
        END
    ) AS HIDDEN relevance')
       ->setParameter('exactMatch', '%' . strtolower($keywords) . '%')
       ->orderBy('relevance', 'DESC')
       ->addOrderBy('g.title', 'ASC');

    return $qb->getQuery()->getResult();
}
}