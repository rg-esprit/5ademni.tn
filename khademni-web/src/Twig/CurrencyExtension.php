<?php

namespace App\Twig;

use App\Service\CurrencyConverterService;
use Twig\Extension\AbstractExtension;
use Twig\TwigFilter;

class CurrencyExtension extends AbstractExtension
{
    public function __construct(
        private CurrencyConverterService $currencyConverter
    ) {
    }

    public function getFilters(): array
    {
        return [
            new TwigFilter('convert_currency', [$this, 'convertCurrency']),
        ];
    }

    public function convertCurrency(float $amount, string $have = 'TND', string $want = 'USD'): string
    {
        $converted = $this->currencyConverter->convert($amount, $have, $want);
        return number_format($converted, 2, '.', ',');
    }
}
