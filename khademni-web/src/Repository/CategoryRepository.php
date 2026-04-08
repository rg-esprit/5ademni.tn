<?php

namespace App\Repository;

use App\Entity\Category;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<Category>
 */
class CategoryRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, Category::class);
    }

    /**
     * @return Category[]
     */
    public function findForFilters(string $query = '', string $status = 'all'): array
    {
        $queryBuilder = $this->createQueryBuilder('category')
            ->orderBy('category.id', 'DESC');

        if ('' !== $query) {
            $queryBuilder
                ->andWhere('LOWER(category.name) LIKE :pattern OR LOWER(COALESCE(category.description, :empty)) LIKE :pattern')
                ->setParameter('pattern', '%'.mb_strtolower($query).'%')
                ->setParameter('empty', '');
        }

        if ('active' === $status) {
            $queryBuilder->andWhere('category.isActive = true');
        } elseif ('inactive' === $status) {
            $queryBuilder->andWhere('category.isActive = false OR category.isActive IS NULL');
        }

        return $queryBuilder->getQuery()->getResult();
    }

    /**
     * @return Category[]
     */
    public function findAllOrdered(): array
    {
        return $this->createQueryBuilder('category')
            ->orderBy('category.name', 'ASC')
            ->getQuery()
            ->getResult();
    }

    /**
     * @return Category[]
     */
    public function findActiveOrdered(): array
    {
        return $this->createQueryBuilder('category')
            ->andWhere('category.isActive = true')
            ->orderBy('category.name', 'ASC')
            ->getQuery()
            ->getResult();
    }

    public function nameExists(string $name, ?int $excludeId = null): bool
    {
        $queryBuilder = $this->createQueryBuilder('category')
            ->select('COUNT(category.id)')
            ->andWhere('LOWER(category.name) = :name')
            ->setParameter('name', mb_strtolower(trim($name)));

        if (null !== $excludeId) {
            $queryBuilder
                ->andWhere('category.id != :excludeId')
                ->setParameter('excludeId', $excludeId);
        }

        return (int) $queryBuilder->getQuery()->getSingleScalarResult() > 0;
    }

    /**
     * @return array{rows: array<int, array{id:int, name:string, is_active:bool, gig_count:int, percentage:float, color:string}>, total_categories:int, active_categories:int, inactive_categories:int, total_gigs:int, most_active:string}
     */
    public function getDashboardData(): array
    {
        $rows = $this->createQueryBuilder('category')
            ->select('category.id AS id, category.name AS name, category.isActive AS isActive, COUNT(gig.id) AS gigCount')
            ->leftJoin('category.gigs', 'gig')
            ->groupBy('category.id')
            ->orderBy('gigCount', 'DESC')
            ->addOrderBy('category.id', 'DESC')
            ->getQuery()
            ->getArrayResult();

        $totalCategories = count($rows);
        $activeCategories = 0;
        $totalGigs = 0;

        foreach ($rows as $row) {
            if ((bool) $row['isActive']) {
                ++$activeCategories;
            }

            $totalGigs += (int) $row['gigCount'];
        }

        $colors = ['#6366f1', '#10b981', '#f59e0b', '#ef4444', '#3b82f6', '#8b5cf6', '#ec4899', '#14b8a6', '#f97316', '#84cc16'];
        $distribution = [];

        foreach ($rows as $index => $row) {
            $count = (int) $row['gigCount'];
            $distribution[] = [
                'id' => (int) $row['id'],
                'name' => (string) $row['name'],
                'is_active' => (bool) $row['isActive'],
                'gig_count' => $count,
                'percentage' => $totalGigs > 0 ? round(($count * 100) / $totalGigs, 1) : 0.0,
                'color' => $colors[$index % count($colors)],
            ];
        }

        $mostActive = '-';
        if ([] !== $distribution && $distribution[0]['gig_count'] > 0) {
            $mostActive = sprintf('%s (%d)', $distribution[0]['name'], $distribution[0]['gig_count']);
        }

        return [
            'rows' => $distribution,
            'total_categories' => $totalCategories,
            'active_categories' => $activeCategories,
            'inactive_categories' => $totalCategories - $activeCategories,
            'total_gigs' => $totalGigs,
            'most_active' => $mostActive,
        ];
    }
}
