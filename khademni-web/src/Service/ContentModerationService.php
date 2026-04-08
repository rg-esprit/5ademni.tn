<?php

namespace App\Service;

use Symfony\Component\DependencyInjection\Attribute\Autowire;
use Symfony\Contracts\HttpClient\Exception\TransportExceptionInterface;
use Symfony\Contracts\HttpClient\HttpClientInterface;

class ContentModerationService
{
    private const MODELS = [
        'google/gemma-3-27b-it:free',
        'meta-llama/llama-3.3-70b-instruct:free',
    ];

    private const SYSTEM_PROMPT = 'You are a strict content moderation model for marketplace listings. Return ONLY valid JSON: {"hasBadWords":true|false,"detectedWords":["..."],"severity":"NONE|LOW|MEDIUM|HIGH","explanation":"..."}. Mark MEDIUM/HIGH for profanity, insults, hate, threats, sexual slurs. Mark LOW for mild swearing.';

    /**
     * @var string[]
     */
    private const LOCAL_BAD_WORDS = [
        'fuck', 'shit', 'asshole', 'bitch', 'bastard', 'dick', 'cunt', 'whore', 'slut',
        'nigger', 'nigga', 'faggot', 'retard', 'motherfucker',
        'kill you', 'i will kill', 'rape', 'murder you',
        'merde', 'putain', 'connard', 'connasse', 'encule', 'salaud', 'salope', 'fils de pute',
        'kuss', 'sharmouta', 'zamel', 'manyak', 'kol khara', 'zebi', 'kahba',
    ];

    public function __construct(
        private readonly HttpClientInterface $httpClient,
        #[Autowire('%env(string:OPENROUTER_API_KEY)%')]
        private readonly string $apiKey,
        #[Autowire('%env(string:OPENROUTER_URL)%')]
        private readonly string $endpoint,
    ) {
    }

    /**
     * @return array{has_bad_words:bool, detected_words:string[], severity:string, explanation:string}
     */
    public function analyze(string $title, string $description): array
    {
        $content = trim($title.' '.$description);
        if ('' === $content) {
            return [
                'has_bad_words' => false,
                'detected_words' => [],
                'severity' => 'NONE',
                'explanation' => 'No content to analyze.',
            ];
        }

        if ('' !== trim($this->apiKey)) {
            foreach (self::MODELS as $model) {
                try {
                    $response = $this->httpClient->request('POST', $this->endpoint, [
                        'headers' => [
                            'Authorization' => 'Bearer '.$this->apiKey,
                            'Content-Type' => 'application/json',
                            'HTTP-Referer' => 'https://5ademni.tn',
                            'X-Title' => '5ademni.tn Moderation',
                        ],
                        'json' => [
                            'model' => $model,
                            'messages' => [
                                ['role' => 'system', 'content' => self::SYSTEM_PROMPT],
                                ['role' => 'user', 'content' => sprintf('Title: %s\nDescription: %s', $title, $description)],
                            ],
                            'temperature' => 0.1,
                        ],
                    ]);

                    $data = $response->toArray(false);
                    if (isset($data['choices'][0]['message']['content']) && is_string($data['choices'][0]['message']['content'])) {
                        $parsed = $this->parseJsonResponse($data['choices'][0]['message']['content']);
                        if (null !== $parsed) {
                            return $parsed;
                        }
                    }
                } catch (TransportExceptionInterface) {
                    break;
                } catch (\Throwable) {
                    continue;
                }
            }
        }

        return $this->localScan($content);
    }

    /**
     * @return array{has_bad_words:bool, detected_words:string[], severity:string, explanation:string}|null
     */
    private function parseJsonResponse(string $content): ?array
    {
        $trimmed = trim($content);
        if (str_contains($trimmed, '```')) {
            $trimmed = preg_replace('/^```(?:json)?/i', '', $trimmed) ?? $trimmed;
            $trimmed = preg_replace('/```$/', '', $trimmed) ?? $trimmed;
            $trimmed = trim($trimmed);
        }

        try {
            /** @var mixed $decoded */
            $decoded = json_decode($trimmed, true, 512, JSON_THROW_ON_ERROR);
        } catch (\Throwable) {
            return null;
        }

        if (!is_array($decoded)) {
            return null;
        }

        $severity = strtoupper((string) ($decoded['severity'] ?? 'NONE'));
        if (!in_array($severity, ['NONE', 'LOW', 'MEDIUM', 'HIGH'], true)) {
            $severity = 'NONE';
        }

        $detected = [];
        if (isset($decoded['detectedWords']) && is_array($decoded['detectedWords'])) {
            foreach ($decoded['detectedWords'] as $word) {
                if (is_string($word) && '' !== trim($word)) {
                    $detected[] = trim($word);
                }
            }
        }

        return [
            'has_bad_words' => (bool) ($decoded['hasBadWords'] ?? false),
            'detected_words' => $detected,
            'severity' => $severity,
            'explanation' => trim((string) ($decoded['explanation'] ?? 'AI moderation completed.')),
        ];
    }

    /**
     * @return array{has_bad_words:bool, detected_words:string[], severity:string, explanation:string}
     */
    private function localScan(string $content): array
    {
        $text = mb_strtolower($content);
        $detected = [];

        foreach (self::LOCAL_BAD_WORDS as $word) {
            if (preg_match('/\b'.preg_quote($word, '/').'\b/u', $text)) {
                $detected[] = $word;
            }
        }

        if ([] === $detected) {
            return [
                'has_bad_words' => false,
                'detected_words' => [],
                'severity' => 'NONE',
                'explanation' => 'No inappropriate language detected.',
            ];
        }

        $severity = count($detected) > 2 ? 'HIGH' : 'MEDIUM';

        return [
            'has_bad_words' => true,
            'detected_words' => $detected,
            'severity' => $severity,
            'explanation' => 'Inappropriate language detected by local moderation rules.',
        ];
    }
}
