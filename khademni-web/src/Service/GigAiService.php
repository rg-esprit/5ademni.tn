<?php

namespace App\Service;

use Symfony\Component\DependencyInjection\Attribute\Autowire;
use Symfony\Contracts\HttpClient\Exception\TransportExceptionInterface;
use Symfony\Contracts\HttpClient\HttpClientInterface;

class GigAiService
{
    private const MODELS = [
        'mistralai/mistral-small-3.1-24b-instruct:free',
        'google/gemma-3-27b-it:free',
        'meta-llama/llama-3.3-70b-instruct:free',
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
     * @param string[] $availableCategories
     *
     * @return array{title:string,description:string,price:float,category:string,delivery_days:int}
     */
    public function generateGig(string $prompt, array $availableCategories = []): array
    {
        $normalizedPrompt = trim($prompt);
        if ('' === $normalizedPrompt) {
            throw new \RuntimeException('Please describe your skill first.');
        }

        if ('' !== trim($this->apiKey)) {
            $categoryHint = [] !== $availableCategories ? implode(', ', $availableCategories) : 'Development, Design, Marketing, Writing, Video, Data, Music, Business';
            $systemPrompt = 'You generate freelance gigs for 5ademni.tn. Return ONLY valid JSON: {"title":"...","description":"...","price":123.45,"category":"...","delivery_days":7}. Rules: title starts with "I will", description 4-6 professional sentences, realistic TND price >= 10, delivery_days between 1 and 30, category must fit: '.$categoryHint.'.';

            $raw = $this->chat($systemPrompt, 'User prompt: '.$normalizedPrompt);
            if (null !== $raw) {
                $parsed = $this->parseGigPayload($raw);
                if (null !== $parsed) {
                    if ([] !== $availableCategories) {
                        $parsed['category'] = $this->closestCategory($parsed['category'], $availableCategories);
                    }

                    return $parsed;
                }
            }
        }

        return $this->fallbackGig($normalizedPrompt, $availableCategories);
    }

    public function generateGigDescription(string $title, string $category): string
    {
        $cleanTitle = trim($title);
        if ('' === $cleanTitle) {
            throw new \RuntimeException('Enter a gig title first.');
        }

        if ('' !== trim($this->apiKey)) {
            $systemPrompt = 'Write a professional freelance gig description in 3-4 sentences. Return plain text only.';
            $raw = $this->chat($systemPrompt, sprintf('Title: %s\nCategory: %s', $cleanTitle, $category));
            if (null !== $raw) {
                $candidate = trim($this->stripCodeFences($raw));
                if ('' !== $candidate) {
                    return $candidate;
                }
            }
        }

        return sprintf(
            'I will deliver high-quality %s services tailored to your project goals. You will get clear communication, structured milestones, and reliable delivery with professional quality standards. I focus on practical results, clean execution, and revisions to make sure the final output matches your expectations. Let us turn your idea into a polished outcome you can use immediately.',
            '' !== trim($category) ? trim($category) : 'freelance'
        );
    }

    public function generateCategoryDescription(string $name): string
    {
        $cleanName = trim($name);
        if ('' === $cleanName) {
            throw new \RuntimeException('Enter a category name first.');
        }

        if ('' !== trim($this->apiKey)) {
            $systemPrompt = 'Write a concise marketplace category description in 2-3 sentences. Return plain text only.';
            $raw = $this->chat($systemPrompt, sprintf('Category name: %s', $cleanName));
            if (null !== $raw) {
                $candidate = trim($this->stripCodeFences($raw));
                if ('' !== $candidate) {
                    return $candidate;
                }
            }
        }

        return sprintf(
            'This category groups freelance services related to %s. It helps clients quickly discover specialists and compare offers with clear deliverables, pricing, and timelines. Choose this category when your project needs focused expertise and professional execution.',
            $cleanName
        );
    }

    /**
     * @param string[] $availableCategories
     */
    public function suggestCategory(string $title, string $description, array $availableCategories): ?string
    {
        if ([] === $availableCategories) {
            return null;
        }

        $cleanTitle = trim($title);
        $cleanDescription = trim($description);
        if ('' === $cleanTitle && '' === $cleanDescription) {
            return null;
        }

        if ('' !== trim($this->apiKey)) {
            $systemPrompt = 'Choose the best category from this list and return ONLY the category name exactly as-is: '.implode(', ', $availableCategories).'.';
            $raw = $this->chat($systemPrompt, sprintf("Title: %s\nDescription: %s", $cleanTitle, $cleanDescription));
            if (null !== $raw) {
                $candidate = trim($this->stripCodeFences($raw));
                if ('' !== $candidate) {
                    return $this->closestCategory($candidate, $availableCategories);
                }
            }
        }

        return $this->keywordCategoryFallback($cleanTitle.' '.$cleanDescription, $availableCategories);
    }

    private function chat(string $systemPrompt, string $userPrompt): ?string
    {
        foreach (self::MODELS as $model) {
            try {
                $response = $this->httpClient->request('POST', $this->endpoint, [
                    'headers' => [
                        'Authorization' => 'Bearer '.$this->apiKey,
                        'Content-Type' => 'application/json',
                        'HTTP-Referer' => 'https://5ademni.tn',
                        'X-Title' => '5ademni.tn Gig AI',
                    ],
                    'json' => [
                        'model' => $model,
                        'messages' => [
                            ['role' => 'system', 'content' => $systemPrompt],
                            ['role' => 'user', 'content' => $userPrompt],
                        ],
                        'temperature' => 0.6,
                    ],
                ]);
            } catch (TransportExceptionInterface) {
                return null;
            } catch (\Throwable) {
                continue;
            }

            $data = $response->toArray(false);
            if (isset($data['choices'][0]['message']['content']) && is_string($data['choices'][0]['message']['content'])) {
                $content = trim($data['choices'][0]['message']['content']);
                if ('' !== $content) {
                    return $content;
                }
            }
        }

        return null;
    }

    /**
     * @return array{title:string,description:string,price:float,category:string,delivery_days:int}|null
     */
    private function parseGigPayload(string $raw): ?array
    {
        $json = $this->stripCodeFences($raw);

        try {
            /** @var mixed $decoded */
            $decoded = json_decode($json, true, 512, JSON_THROW_ON_ERROR);
        } catch (\Throwable) {
            return null;
        }

        if (!is_array($decoded)) {
            return null;
        }

        $title = trim((string) ($decoded['title'] ?? ''));
        $description = trim((string) ($decoded['description'] ?? ''));
        $category = trim((string) ($decoded['category'] ?? ''));
        $price = (float) ($decoded['price'] ?? 0);
        $deliveryDays = (int) ($decoded['delivery_days'] ?? 0);

        if ('' === $title || '' === $description || '' === $category) {
            return null;
        }

        if (!str_starts_with(mb_strtolower($title), 'i will')) {
            $title = 'I will '.$title;
        }

        if ($price < 10) {
            $price = 50.0;
        }

        if ($deliveryDays < 1 || $deliveryDays > 30) {
            $deliveryDays = 7;
        }

        return [
            'title' => $title,
            'description' => $description,
            'price' => round($price, 2),
            'category' => $category,
            'delivery_days' => $deliveryDays,
        ];
    }

    /**
     * @param string[] $availableCategories
     *
     * @return array{title:string,description:string,price:float,category:string,delivery_days:int}
     */
    private function fallbackGig(string $prompt, array $availableCategories): array
    {
        $text = mb_strtolower($prompt);

        $category = $this->keywordCategoryFallback($text, $availableCategories)
            ?? ($availableCategories[0] ?? 'Development');

        $titleTopic = $this->extractTopic($text);
        $title = 'I will deliver professional '.$titleTopic.' services';

        $description = sprintf(
            'I will provide %s services with a clear scope, strong quality standards, and reliable communication from start to finish. You will receive practical deliverables tailored to your goals and timeline. I focus on clean execution, measurable outcomes, and revisions where needed to ensure the final output fits your expectations. Share your project details and I will prepare a solution-oriented plan for fast delivery.',
            mb_strtolower($category)
        );

        $basePrice = match (mb_strtolower($category)) {
            'development' => 300.0,
            'design' => 100.0,
            'marketing' => 150.0,
            'writing' => 70.0,
            'video' => 130.0,
            default => 120.0,
        };

        return [
            'title' => $title,
            'description' => $description,
            'price' => round($basePrice, 2),
            'category' => $category,
            'delivery_days' => 7,
        ];
    }

    private function extractTopic(string $prompt): string
    {
        $tokens = preg_split('/\W+/u', $prompt, -1, PREG_SPLIT_NO_EMPTY) ?: [];
        $topic = implode(' ', array_slice($tokens, 0, 4));

        return '' === $topic ? 'freelance' : $topic;
    }

    /**
     * @param string[] $availableCategories
     */
    private function closestCategory(string $candidate, array $availableCategories): string
    {
        $normalizedCandidate = mb_strtolower(trim($candidate));

        foreach ($availableCategories as $category) {
            if (mb_strtolower($category) === $normalizedCandidate) {
                return $category;
            }
        }

        foreach ($availableCategories as $category) {
            $hay = mb_strtolower($category);
            if (str_contains($hay, $normalizedCandidate) || str_contains($normalizedCandidate, $hay)) {
                return $category;
            }
        }

        return $this->keywordCategoryFallback($candidate, $availableCategories) ?? $availableCategories[0];
    }

    /**
     * @param string[] $availableCategories
     */
    private function keywordCategoryFallback(string $text, array $availableCategories): ?string
    {
        $lower = mb_strtolower($text);

        $weights = [
            'design' => ['design', 'logo', 'brand', 'ui', 'ux', 'graphic', 'illustration'],
            'development' => ['development', 'web', 'app', 'api', 'react', 'java', 'python', 'mobile', 'backend', 'frontend'],
            'marketing' => ['marketing', 'seo', 'social', 'ads', 'campaign'],
            'writing' => ['writing', 'content', 'copy', 'article', 'blog'],
            'video' => ['video', 'animation', 'editing', 'motion', 'premiere'],
            'data' => ['data', 'analysis', 'analytics', 'dashboard'],
            'music' => ['music', 'audio', 'mixing', 'voice'],
            'business' => ['business', 'strategy', 'plan', 'consulting'],
        ];

        $bestLabel = null;
        $bestScore = 0;

        foreach ($availableCategories as $category) {
            $categoryLower = mb_strtolower($category);
            $score = 0;

            foreach ($weights as $bucket => $keywords) {
                if (!str_contains($categoryLower, $bucket)) {
                    continue;
                }

                foreach ($keywords as $keyword) {
                    if (str_contains($lower, $keyword)) {
                        ++$score;
                    }
                }
            }

            if ($score > $bestScore) {
                $bestScore = $score;
                $bestLabel = $category;
            }
        }

        return $bestLabel;
    }

    private function stripCodeFences(string $value): string
    {
        $trimmed = trim($value);
        if (!str_contains($trimmed, '```')) {
            return $trimmed;
        }

        $trimmed = preg_replace('/^```(?:json)?/i', '', $trimmed) ?? $trimmed;
        $trimmed = preg_replace('/```$/', '', $trimmed) ?? $trimmed;

        return trim($trimmed);
    }
}
