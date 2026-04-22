<?php

namespace App\Repository;

use App\Entity\Gig;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<Gig>
 */
class GigRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, Gig::class);
    }

    /**
     * @return Gig[]
     */
    public function findAllWithCategory(): array
    {
        return $this->createQueryBuilder('gig')
            ->addSelect('category')
            ->leftJoin('gig.category', 'category')
            ->orderBy('gig.id', 'DESC')
            ->getQuery()
            ->getResult();
    }

    /**
     * @param Gig[] $gigs
     * @param array{query:string, category_id:?int, status:string, min_price:?float, max_price:?float} $filters
     *
     * @return Gig[]
     */
    public function filterInMemory(array $gigs, array $filters): array
    {
        $query = mb_strtolower(trim($filters['query']));
        $categoryId = $filters['category_id'];
        $status = strtoupper(trim($filters['status']));
        $minPrice = $filters['min_price'];
        $maxPrice = $filters['max_price'];
        $now = new \DateTimeImmutable();

        return array_values(array_filter($gigs, static function (Gig $gig) use ($query, $categoryId, $status, $minPrice, $maxPrice, $now): bool {
            if ('' !== $query) {
                $title = mb_strtolower($gig->getTitle());
                $description = mb_strtolower($gig->getDescription());
                if (!str_contains($title, $query) && !str_contains($description, $query)) {
                    return false;
                }
            }

            if (null !== $categoryId) {
                $gigCategoryId = $gig->getCategory()?->getId();
                if ($gigCategoryId !== $categoryId) {
                    return false;
                }
            }

            if ('' !== $status && 'ALL' !== $status) {
                if ($gig->getDisplayStatus($now) !== $status) {
                    return false;
                }
            }

            if (null !== $minPrice && $gig->getPrice() < $minPrice) {
                return false;
            }

            if (null !== $maxPrice && $gig->getPrice() > $maxPrice) {
                return false;
            }

            return true;
        }));
    }

    /**
     * @param Gig[] $gigs
     *
     * @return array{total:int, approved:int, revenue:float}
     */
    public function summarize(array $gigs): array
    {
        $approved = 0;
        $revenue = 0.0;
        $now = new \DateTimeImmutable();

        foreach ($gigs as $gig) {
            if (Gig::STATUS_APPROVED === $gig->getDisplayStatus($now)) {
                ++$approved;
            }

            $revenue += $gig->getPrice();
        }

        return [
            'total' => count($gigs),
            'approved' => $approved,
            'revenue' => $revenue,
        ];
    }

    /**
     * @return Gig[]
     */
    public function findByArticleKeywords(string $keywords): array
    {
        $words = array_values(array_filter(preg_split('/\s+/', trim($keywords)) ?: [], static fn (string $word): bool => mb_strlen($word) > 2));

        if ([] === $words) {
            return [];
        }

        $queryBuilder = $this->createQueryBuilder('gig')
            ->leftJoin('gig.category', 'category')
            ->addSelect('category');

        foreach ($words as $index => $word) {
            $parameter = 'word'.$index;
            $queryBuilder
                ->orWhere(sprintf('LOWER(gig.title) LIKE LOWER(:%s)', $parameter))
                ->orWhere(sprintf('LOWER(gig.description) LIKE LOWER(:%s)', $parameter))
                ->setParameter($parameter, '%'.$word.'%');
        }

        return $queryBuilder
            ->addSelect('CASE WHEN LOWER(gig.title) LIKE :exactMatch THEN 1 ELSE 0 END AS HIDDEN relevance')
            ->setParameter('exactMatch', '%'.mb_strtolower(trim($keywords)).'%')
            ->orderBy('relevance', 'DESC')
            ->addOrderBy('gig.title', 'ASC')
            ->getQuery()
            ->getResult();
    }
}
