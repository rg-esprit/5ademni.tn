<?php

namespace App\Service;

use Symfony\Component\DependencyInjection\Attribute\Autowire;
use Symfony\Contracts\HttpClient\Exception\TransportExceptionInterface;
use Symfony\Contracts\HttpClient\HttpClientInterface;

class ReviewAiService
{
    private const LEGACY_ENDPOINT = 'http://208.115.212.179:11434/api/generate';
    private const LEGACY_MODEL = 'gemma4:e4b';
    private const LEGACY_KEEP_ALIVE = '30m';
    private const MAX_EXECUTION_TIME = 60;
    private const OPENROUTER_MODELS = [
        'google/gemma-3-27b-it:free',
        'mistralai/mistral-small-3.1-24b-instruct:free',
        'meta-llama/llama-3.3-70b-instruct:free',
    ];

    private const SYSTEM_PROMPT = 'Write a concise, professional, natural freelancer review for 5ademni.tn in 2 to 4 sentences. Do not include any greeting, sign-off, rating, or stars. Return only the review text.';

    public function __construct(
        private readonly HttpClientInterface $httpClient,
        private readonly PromptSafetyService $promptSafetyService,
        #[Autowire('%env(string:OPENROUTER_API_KEY)%')]
        private readonly string $openRouterApiKey,
        #[Autowire('%env(string:OPENROUTER_URL)%')]
        private readonly string $openRouterEndpoint,
    ) {
    }

    public function generate(string $prompt, ?string $userIp = null): string
    {
        $normalizedPrompt = trim($prompt);
        if ('' === $normalizedPrompt) {
            throw new \RuntimeException('Describe what you want the review to say first.');
        }

        $this->promptSafetyService->assertSafe($normalizedPrompt, $userIp);

        // Give the model call its full timeout window after prompt validation finishes.
        @ini_set('max_execution_time', (string) self::MAX_EXECUTION_TIME);
        @set_time_limit(self::MAX_EXECUTION_TIME);

        if ('' !== trim($this->openRouterApiKey)) {
            $content = $this->generateWithOpenRouter($normalizedPrompt);
            if (null !== $content) {
                return $content;
            }
        }

        $content = $this->generateWithLegacyEndpoint($normalizedPrompt);
        if (null !== $content) {
            return $content;
        }

        throw new \RuntimeException('The AI review service is unreachable right now.');
    }

    private function generateWithOpenRouter(string $prompt): ?string
    {
        foreach (self::OPENROUTER_MODELS as $model) {
            try {
                $response = $this->httpClient->request('POST', $this->openRouterEndpoint, [
                    'headers' => [
                        'Authorization' => 'Bearer '.$this->openRouterApiKey,
                        'Content-Type' => 'application/json',
                        'HTTP-Referer' => 'https://5ademni.tn',
                        'X-Title' => '5ademni.tn Review AI',
                    ],
                    'json' => [
                        'model' => $model,
                        'messages' => [
                            ['role' => 'system', 'content' => self::SYSTEM_PROMPT],
                            ['role' => 'user', 'content' => $prompt],
                        ],
                        'temperature' => 0.4,
                    ],
                    'timeout' => self::MAX_EXECUTION_TIME,
                ]);
            } catch (TransportExceptionInterface) {
                return null;
            } catch (\Throwable) {
                continue;
            }

            $data = $response->toArray(false);
            $content = trim((string) ($data['choices'][0]['message']['content'] ?? ''));
            if ('' !== $content) {
                return $this->sanitizeResponse($content);
            }
        }

        return null;
    }

    private function generateWithLegacyEndpoint(string $prompt): ?string
    {

        try {
            $response = $this->httpClient->request('POST', self::LEGACY_ENDPOINT, [
                'json' => [
                    'model' => self::LEGACY_MODEL,
                    'system' => self::SYSTEM_PROMPT,
                    'prompt' => $prompt,
                    'stream' => false,
                    'think' => false,
                    'keep_alive' => self::LEGACY_KEEP_ALIVE,
                    'options' => [
                        'num_predict' => 120,
                        'temperature' => 0.4,
                    ],
                ],
                'timeout' => self::MAX_EXECUTION_TIME,
            ]);
        } catch (TransportExceptionInterface) {
            return null;
        }

        $data = $response->toArray(false);

        if (isset($data['response']) && is_string($data['response'])) {
            $content = trim($data['response']);

            if ('' !== $content) {
                return $this->sanitizeResponse($content);
            }
        }

        return null;
    }

    private function sanitizeResponse(string $content): string
    {
        $clean = preg_replace('/^```(?:[a-z]+)?\s*|```$/im', '', trim($content)) ?? trim($content);
        $clean = trim($clean, " \t\n\r\0\x0B\"");

        if ('' === $clean) {
            throw new \RuntimeException('The AI review service returned no content.');
        }

        return $clean;
    }
}
