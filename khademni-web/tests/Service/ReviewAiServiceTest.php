<?php

namespace App\Tests\Service;

use App\Service\PromptSafetyService;
use App\Service\ReviewAiService;
use PHPUnit\Framework\TestCase;
use Symfony\Contracts\HttpClient\HttpClientInterface;
use Symfony\Contracts\HttpClient\ResponseInterface;

class ReviewAiServiceTest extends TestCase
{
    public function testGenerateUsesExtendedLegacyTimeoutWhenOpenRouterIsDisabled(): void
    {
        $response = $this->createMock(ResponseInterface::class);
        $response->expects(self::once())
            ->method('toArray')
            ->with(false)
            ->willReturn([
                'response' => 'Clear communication and on-time delivery made the whole project smooth from start to finish.',
            ]);

        $httpClient = $this->createMock(HttpClientInterface::class);
        $httpClient->expects(self::once())
            ->method('request')
            ->with(
                'POST',
                'http://208.115.212.179:11434/api/generate',
                self::callback(static function (array $options): bool {
                    return 60 === ($options['timeout'] ?? null)
                        && 'gemma4:e4b' === ($options['json']['model'] ?? null)
                        && false === ($options['json']['stream'] ?? null)
                        && false === ($options['json']['think'] ?? null);
                })
            )
            ->willReturn($response);

        $promptSafetyService = $this->createMock(PromptSafetyService::class);
        $promptSafetyService->expects(self::once())
            ->method('assertSafe')
            ->with('Write a positive review about clear communication.', '203.0.113.9');

        $service = new ReviewAiService(
            $httpClient,
            $promptSafetyService,
            '',
            'https://example.test/openrouter'
        );

        self::assertSame(
            'Clear communication and on-time delivery made the whole project smooth from start to finish.',
            $service->generate('Write a positive review about clear communication.', '203.0.113.9')
        );
    }
}
