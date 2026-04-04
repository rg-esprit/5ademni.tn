<?php

namespace App\Service;

use Symfony\Component\DependencyInjection\Attribute\Autowire;
use Symfony\Contracts\HttpClient\Exception\TransportExceptionInterface;
use Symfony\Contracts\HttpClient\HttpClientInterface;

class ReviewAiService
{
    private const CHAT_MODELS = [
        'mistralai/mistral-small-3.1-24b-instruct:free',
        'google/gemma-3-27b-it:free',
        'meta-llama/llama-3.3-70b-instruct:free',
        'qwen/qwen3-4b:free',
        'cognitivecomputations/dolphin-mistral-24b-venice-edition:free',
        'liquid/lfm-2.5-1.2b-instruct:free',
        'arcee-ai/trinity-mini:free',
    ];

    private const SYSTEM_PROMPT = 'You are a helpful assistant that writes freelancer reviews for a job marketplace called 5ademni.tn. Write a concise, professional, and natural-sounding review in 2 to 4 sentences based on the user description. Do not include any greeting, sign-off, rating, or stars. Return only the review text.';

    public function __construct(
        private readonly HttpClientInterface $httpClient,
        #[Autowire('%env(string:OPENROUTER_API_KEY)%')]
        private readonly string $apiKey,
        #[Autowire('%env(string:OPENROUTER_URL)%')]
        private readonly string $endpoint,
    ) {
    }

    public function generate(string $prompt): string
    {
        if ('' === trim($this->apiKey)) {
            throw new \RuntimeException('The AI review generator is not configured yet.');
        }

        $lastError = 'All AI models failed. Please try again in a moment.';

        foreach (self::CHAT_MODELS as $model) {
            try {
                $response = $this->httpClient->request('POST', $this->endpoint, [
                    'headers' => [
                        'Authorization' => 'Bearer '.$this->apiKey,
                        'Content-Type' => 'application/json',
                        'HTTP-Referer' => 'https://5ademni.tn',
                        'X-Title' => '5ademni.tn Reviews',
                    ],
                    'json' => [
                        'model' => $model,
                        'messages' => [
                            [
                                'role' => 'system',
                                'content' => self::SYSTEM_PROMPT,
                            ],
                            [
                                'role' => 'user',
                                'content' => 'Write a review based on this: '.$prompt,
                            ],
                        ],
                    ],
                ]);
            } catch (TransportExceptionInterface $exception) {
                $lastError = 'The AI review service is unreachable right now.';
                continue;
            }

            $data = $response->toArray(false);

            if (isset($data['choices'][0]['message']['content']) && is_string($data['choices'][0]['message']['content'])) {
                $content = trim($data['choices'][0]['message']['content']);

                if ('' !== $content) {
                    return $content;
                }
            }

            if (isset($data['error']['message']) && is_string($data['error']['message'])) {
                $lastError = $data['error']['message'];
            } else {
                $lastError = sprintf('Model %s returned no content.', $model);
            }
        }

        throw new \RuntimeException($lastError);
    }
}
