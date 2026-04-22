<?php

namespace App\Service;

use Symfony\Contracts\HttpClient\HttpClientInterface;
use Symfony\Contracts\Cache\CacheInterface;
use Symfony\Contracts\Cache\ItemInterface;

class CurrencyConverterService
{
    private const API_URL = 'https://api.api-ninjas.com/v1/convertcurrency';

    public function __construct(
        private HttpClientInterface $httpClient,
        private CacheInterface $cache,
        private string $apiNinjasKey
    ) {
    }

    /**
     * Converts an amount from one currency to another using API Ninjas.
     * Results are cached for 1 hour to optimize performance and respect rate limits.
     */
    public function convert(float $amount, string $have = 'TND', string $want = 'USD'): float
    {
        if ($have === $want) {
            return $amount;
        }

        $cacheKey = sprintf('currency_rate_%s_%s', $have, $want);

        // Fetch the exchange rate for 1 unit of the "have" currency
        $rate = $this->cache->get($cacheKey, function (ItemInterface $item) use ($have, $want) {
            $item->expiresAfter(3600); // 1 hour cache

            try {
                $response = $this->httpClient->request('GET', self::API_URL, [
                    'headers' => [
                        'X-Api-Key' => $this->apiNinjasKey,
                    ],
                    'query' => [
                        'have' => $have,
                        'want' => $want,
                        'amount' => 1,
                    ],
                ]);

                $data = $response->toArray();
                return (float) ($data['new_amount'] ?? 0);
            } catch (\Throwable $e) {
                // Return 0 or a fallback if API fails
                return 0.0;
            }
        });

        if ($rate <= 0) {
            // Fallback rates if API is down (approximate)
            if ($have === 'TND' && $want === 'USD') return $amount * 0.32;
            if ($have === 'TND' && $want === 'EUR') return $amount * 0.30;
            return $amount;
        }

        return round($amount * $rate, 2);
    }
}
