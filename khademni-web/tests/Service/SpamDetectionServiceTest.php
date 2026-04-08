<?php

namespace App\Tests\Service;

use App\Service\SpamDetectionService;
use PHPUnit\Framework\TestCase;

class SpamDetectionServiceTest extends TestCase
{
    public function testForbiddenPhraseTriggersSpam(): void
    {
        $service = new SpamDetectionService();

        $result = $service->analyze(
            'Professional logo design',
            'Contact me on telegram for details',
            50.0
        );

        self::assertTrue($result['is_spam']);
        self::assertGreaterThanOrEqual(40, $result['score']);
        self::assertContains('Contains forbidden phrase: "telegram".', $result['reasons']);
    }

    public function testMixedCaseForbiddenPhraseIsDetected(): void
    {
        $service = new SpamDetectionService();

        $result = $service->analyze(
            'Reliable tutoring service',
            '100% guaranteed success if you enroll now',
            60.0
        );

        self::assertTrue($result['is_spam']);
        self::assertGreaterThanOrEqual(40, $result['score']);
        self::assertContains('Contains forbidden phrase: "100% Guaranteed".', $result['reasons']);
    }

    public function testLegitimateContentIsNotSpam(): void
    {
        $service = new SpamDetectionService();

        $result = $service->analyze(
            'Website landing page redesign',
            'I will redesign your landing page with responsive layout and clear CTA.',
            120.0
        );

        self::assertFalse($result['is_spam']);
        self::assertSame([], $result['reasons']);
    }
}
