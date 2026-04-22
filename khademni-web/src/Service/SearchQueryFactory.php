<?php

namespace App\Service;

use App\Entity\Gig;
use Elastica\Query;
use Elastica\Query\BoolQuery;
use Elastica\Query\Exists;
use Elastica\Query\QueryString;
use Elastica\Query\Range;
use Elastica\Query\Term;

class SearchQueryFactory
{
    public function buildGigSearchQuery(array $filters): Query
    {
        $queryText = trim((string) ($filters['query'] ?? ''));
        $categoryId = $filters['category_id'] ?? null;
        $status = strtoupper(trim((string) ($filters['status'] ?? 'ALL')));
        if (!in_array($status, Gig::getFilterableStatuses(), true)) {
            $status = 'ALL';
        }

        $boolQuery = new BoolQuery();

        if ('' !== $queryText) {
            $textQuery = new QueryString($queryText);
            $textQuery->setDefaultOperator('and');
            $textQuery->setFields(['title^4', 'description^2', 'category.name^2', 'category.slug']);
            $boolQuery->addMust($textQuery);
        }

        if (null !== $categoryId) {
            $boolQuery->addFilter((new Term())->setTerm('category.id', $categoryId));
        }

        $now = (new \DateTimeImmutable())->format(\DATE_ATOM);

        if (Gig::STATUS_APPROVED === $status) {
            $boolQuery->addFilter((new Term())->setTerm('status', Gig::STATUS_APPROVED));
            $boolQuery->addFilter($this->buildApprovedDeliveryFilter($now));
        } elseif (Gig::STATUS_EXPIRED === $status) {
            $boolQuery->addFilter((new Term())->setTerm('status', Gig::STATUS_APPROVED));
            $boolQuery->addFilter(new Range('deliveryTime', ['lt' => $now]));
        } elseif (in_array($status, [Gig::STATUS_DRAFT, Gig::STATUS_PENDING, Gig::STATUS_REJECTED, Gig::STATUS_ARCHIVED], true)) {
            $boolQuery->addFilter((new Term())->setTerm('status', $status));
        }

        $query = new Query($boolQuery);
        $query->setTrackScores(true);
        $query->setSort([
            ['_score' => ['order' => 'desc']],
            ['createdAt' => ['order' => 'desc']],
            ['id' => ['order' => 'desc']],
        ]);

        return $query;
    }

    public function buildCategorySearchQuery(string $queryText, string $status): Query
    {
        $queryText = trim($queryText);
        $status = strtolower(trim($status));

        $boolQuery = new BoolQuery();

        if ('' !== $queryText) {
            $textQuery = new QueryString($queryText);
            $textQuery->setDefaultOperator('and');
            $textQuery->setFields(['name^4', 'description^2', 'slug']);
            $boolQuery->addMust($textQuery);
        }

        if ('active' === $status) {
            $boolQuery->addFilter((new Term())->setTerm('isActive', true));
        } elseif ('inactive' === $status) {
            $boolQuery->addFilter((new Term())->setTerm('isActive', false));
        }

        $query = new Query($boolQuery);
        $query->setTrackScores(true);
        $query->setSort([
            ['_score' => ['order' => 'desc']],
            ['id' => ['order' => 'desc']],
        ]);

        return $query;
    }

    private function buildApprovedDeliveryFilter(string $now): BoolQuery
    {
        $deliveryWindow = new BoolQuery();
        $deliveryWindow->addShould(new Range('deliveryTime', ['gte' => $now]));

        $missingDelivery = new BoolQuery();
        $missingDelivery->addMustNot(new Exists('deliveryTime'));
        $deliveryWindow->addShould($missingDelivery);
        $deliveryWindow->setMinimumShouldMatch(1);

        return $deliveryWindow;
    }
}