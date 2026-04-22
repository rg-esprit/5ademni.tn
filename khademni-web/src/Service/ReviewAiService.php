<?php

namespace App\Service;

use Symfony\Contracts\HttpClient\Exception\TransportExceptionInterface;
use Symfony\Contracts\HttpClient\HttpClientInterface;

class ReviewAiService
{
    private const ENDPOINT = 'http://208.115.212.179:11434/api/generate';
    private const MODEL = 'gemma4:e4b';
    private const KEEP_ALIVE = '30m';
    private const MAX_EXECUTION_TIME = 120;

    private const SYSTEM_PROMPT = 'Write a concise, professional, natural freelancer review for 5ademni.tn in 2 to 4 sentences. Do not include any greeting, sign-off, rating, or stars. Return only the review text.';

    public function __construct(
        private readonly HttpClientInterface $httpClient,
        private readonly PromptSafetyService $promptSafetyService,
    ) {
    }

    public function generate(string $prompt, ?string $userIp = null): string
    {
        @ini_set('max_execution_time', (string) self::MAX_EXECUTION_TIME);
        @set_time_limit(self::MAX_EXECUTION_TIME);

        $this->promptSafetyService->assertSafe($prompt, $userIp);

        try {
            $response = $this->httpClient->request('POST', self::ENDPOINT, [
                'json' => [
                    'model' => self::MODEL,
                    'system' => self::SYSTEM_PROMPT,
                    'prompt' => $prompt,
                    'stream' => false,
                    'think' => false,
                    'keep_alive' => self::KEEP_ALIVE,
                    'options' => [
                        'num_predict' => 120,
                        'temperature' => 0.4,
                    ],
                ],
                'timeout' => self::MAX_EXECUTION_TIME,
            ]);
        } catch (TransportExceptionInterface $exception) {
            throw new \RuntimeException('The AI review service is unreachable right now.', 0, $exception);
        }

        $data = $response->toArray(false);

        if (isset($data['response']) && is_string($data['response'])) {
            $content = trim($data['response']);

            if ('' !== $content) {
                return $content;
            }
        }

        if (isset($data['error']) && is_string($data['error'])) {
            throw new \RuntimeException($data['error']);
        }

        throw new \RuntimeException('The AI review service returned no content.');
    }
}
