<?php

namespace App\Service;

use Symfony\Contracts\HttpClient\HttpClientInterface;
use Symfony\Contracts\Cache\CacheInterface;
use Symfony\Contracts\Cache\ItemInterface;

class LogoService
{
    private const API_URL = 'https://api.api-ninjas.com/v1/logo';

    public function __construct(
        private HttpClientInterface $httpClient,
        private CacheInterface $cache,
        private string $apiNinjasKey
    ) {
    }

    /**
     * Fetches a company logo URL from API Ninjas.
     * Returns null if no logo is found (fallback to initials avatar).
     * Results are cached for 24 hours.
     */
    public function getLogoUrl(string $name): ?string
    {
        $name = trim($name);
        if ('' === $name) {
            return null;
        }

        $cacheKey = 'logo_' . md5(mb_strtolower($name));

        return $this->cache->get($cacheKey, function (ItemInterface $item) use ($name) {
            $item->expiresAfter(86400); // 24 hours

            // Helper function for the API call
            $fetchApi = function (string $queryName) {
                try {
                    $response = $this->httpClient->request('GET', self::API_URL, [
                        'headers' => [
                            'X-Api-Key' => $this->apiNinjasKey,
                        ],
                        'query' => [
                            'name' => $queryName,
                        ],
                    ]);

                    $data = $response->toArray();
                    if (!empty($data) && isset($data[0]['image'])) {
                        return $data[0]['image'];
                    }
                } catch (\Throwable) {
                    // Silently fail
                }
                return null;
            };

            // Attempt 1: Full name
            $logo = $fetchApi($name);
            if ($logo) {
                return $logo;
            }

            // Attempt 2: First word only (useful if user entered "Google Google" or "Tesla Inc")
            $firstWord = explode(' ', $name)[0];
            if (strlen($firstWord) >= 3 && strtolower($firstWord) !== strtolower($name)) {
                $logo = $fetchApi($firstWord);
                if ($logo) {
                    return $logo;
                }
            }

            return null;
        });
    }

    /**
     * Generates an SVG-based initials avatar as a data URI.
     * Used as a premium fallback when no brand logo is found.
     */
    public function getInitialsAvatar(string $name): string
    {
        $name = trim($name);
        $initials = $this->extractInitials($name);
        $colors = $this->getGradientColors($name);

        $svg = sprintf(
            '<svg xmlns="http://www.w3.org/2000/svg" width="60" height="60">'
            . '<defs><linearGradient id="g" x1="0%%" y1="0%%" x2="100%%" y2="100%%">'
            . '<stop offset="0%%" style="stop-color:%s"/>'
            . '<stop offset="100%%" style="stop-color:%s"/>'
            . '</linearGradient></defs>'
            . '<rect width="60" height="60" rx="14" fill="url(#g)"/>'
            . '<text x="50%%" y="54%%" dominant-baseline="middle" text-anchor="middle" '
            . 'fill="white" font-family="Arial,sans-serif" font-weight="700" font-size="22">%s</text>'
            . '</svg>',
            $colors[0],
            $colors[1],
            htmlspecialchars($initials, ENT_XML1)
        );

        return 'data:image/svg+xml;base64,' . base64_encode($svg);
    }

    private function extractInitials(string $name): string
    {
        $words = preg_split('/\s+/', $name, -1, PREG_SPLIT_NO_EMPTY);
        if (empty($words)) {
            return '?';
        }

        if (count($words) === 1) {
            return mb_strtoupper(mb_substr($words[0], 0, 2));
        }

        return mb_strtoupper(mb_substr($words[0], 0, 1) . mb_substr($words[1], 0, 1));
    }

    /**
     * Deterministic gradient colors based on the name hash.
     * Ensures the same name always gets the same color.
     *
     * @return string[] Two hex color strings
     */
    private function getGradientColors(string $name): array
    {
        $palettes = [
            ['#6c5ce7', '#a855f7'], // Purple
            ['#3b5bdb', '#228be6'], // Blue
            ['#0891b2', '#06b6d4'], // Cyan
            ['#059669', '#10b981'], // Emerald
            ['#d97706', '#f59e0b'], // Amber
            ['#dc2626', '#f43f5e'], // Rose
            ['#7c3aed', '#c084fc'], // Violet
            ['#0284c7', '#38bdf8'], // Sky
        ];

        $index = crc32(mb_strtolower($name)) % count($palettes);
        if ($index < 0) {
            $index += count($palettes);
        }

        return $palettes[$index];
    }
}
