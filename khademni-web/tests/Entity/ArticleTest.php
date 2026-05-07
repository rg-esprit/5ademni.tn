<?php

namespace App\Tests\Entity;

use App\Entity\Article;
use PHPUnit\Framework\TestCase;

class ArticleTest extends TestCase
{
    public function testArticleCreatedAtAcceptsImmutableDate(): void
    {
        $article = new Article();

        $article->setCreatedAt(new \DateTimeImmutable('2026-05-07 12:30:00'));

        self::assertInstanceOf(\DateTime::class, $article->getCreatedAt());
        self::assertSame('2026-05-07 12:30:00', $article->getCreatedAt()->format('Y-m-d H:i:s'));
    }
}
