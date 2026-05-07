<?php

namespace App\Tests\Entity;

use App\Entity\Gig;
use PHPUnit\Framework\TestCase;

class GigTest extends TestCase
{
    public function testApprovedGigIsExpiredAfterDeliveryDate(): void
    {
        $gig = (new Gig())
            ->setStatus(Gig::STATUS_APPROVED)
            ->setDeliveryTime(new \DateTimeImmutable('2026-01-01 10:00:00'));

        self::assertSame(
            Gig::STATUS_EXPIRED,
            $gig->getDisplayStatus(new \DateTimeImmutable('2026-01-02 10:00:00'))
        );
    }
}
