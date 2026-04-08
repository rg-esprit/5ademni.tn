<?php

namespace App\Service;

use Symfony\Contracts\HttpClient\Exception\TransportExceptionInterface;
use Symfony\Contracts\HttpClient\HttpClientInterface;

class PriceSuggestionService
{
    private const PRICE_API_ENDPOINT = 'http://127.0.0.1:5001/predict_price';

    public function __construct(
        private readonly HttpClientInterface $httpClient,
    ) {
    }

    /**
     * @return array{price:float, source:string, delivery_days:int, category:string}
     */
    public function suggest(string $categoryName, string $description, ?\DateTimeInterface $deliveryTime): array
    {
        $mlCategory = $this->normalizeCategory($categoryName);
        $descriptionLength = max(0, mb_strlen(trim($description)));
        $deliveryDays = $this->resolveDeliveryDays($deliveryTime);

        $localPrice = $this->computeLocal($mlCategory, $descriptionLength, $deliveryDays);

        try {
            $response = $this->httpClient->request('POST', self::PRICE_API_ENDPOINT, [
                'timeout' => 2,
                'json' => [
                    'category' => $mlCategory,
                    'description_length' => $descriptionLength,
                    'delivery_time' => $deliveryDays,
                ],
            ]);

            $data = $response->toArray(false);
            if (isset($data['recommended_price']) && is_numeric($data['recommended_price'])) {
                $predicted = max(10.0, (float) $data['recommended_price']);
                if (abs($predicted - 1523.55) > 0.5) {
                    return [
                        'price' => round($predicted, 2),
                        'source' => 'AI (ML model)',
                        'delivery_days' => $deliveryDays,
                        'category' => $mlCategory,
                    ];
                }
            }
        } catch (TransportExceptionInterface) {
            // Fall through to local estimate.
        } catch (\Throwable) {
            // Fall through to local estimate.
        }

        return [
            'price' => round($localPrice, 2),
            'source' => 'AI (local estimate)',
            'delivery_days' => $deliveryDays,
            'category' => $mlCategory,
        ];
    }

    private function normalizeCategory(string $categoryName): string
    {
        $name = mb_strtolower($categoryName);

        return match (true) {
            str_contains($name, 'design'),
            str_contains($name, 'graphic'),
            str_contains($name, 'logo') => 'Design',
            str_contains($name, 'marketing'),
            str_contains($name, 'seo'),
            str_contains($name, 'social') => 'Marketing',
            str_contains($name, 'writing'),
            str_contains($name, 'content'),
            str_contains($name, 'copy') => 'Writing',
            str_contains($name, 'video'),
            str_contains($name, 'animation'),
            str_contains($name, 'edit') => 'Video',
            default => 'Development',
        };
    }

    private function resolveDeliveryDays(?\DateTimeInterface $deliveryTime): int
    {
        if (!$deliveryTime instanceof \DateTimeInterface) {
            return 7;
        }

        $today = new \DateTimeImmutable('today');
        $deliveryDay = \DateTimeImmutable::createFromInterface($deliveryTime)->setTime(0, 0);
        $days = (int) $today->diff($deliveryDay)->format('%r%a');

        return max(1, $days);
    }

    private function computeLocal(string $category, int $descriptionLength, int $deliveryDays): float
    {
        [$base, $perChar, $perDay] = match ($category) {
            'Development' => [300.0, 2.5, 40.0],
            'Design' => [100.0, 1.2, 25.0],
            'Marketing' => [150.0, 1.5, 30.0],
            'Video' => [120.0, 1.3, 35.0],
            'Writing' => [50.0, 0.8, 10.0],
            default => [200.0, 1.5, 30.0],
        };

        $price = $base + ($perChar * $descriptionLength) + ($perDay * $deliveryDays);

        return max(10.0, round($price, 2));
    }
}
