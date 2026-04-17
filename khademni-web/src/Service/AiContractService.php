<?php

namespace App\Service;

use Symfony\Component\DependencyInjection\Attribute\Autowire;
use Symfony\Contracts\HttpClient\HttpClientInterface;

/**
 * AI-powered service for generating professional contract descriptions
 * and detecting suspicious payment amounts.
 *
 * Uses OpenRouter (same provider as SummarizeService) with the free Gemma model.
 */
class AiContractService
{
    public function __construct(
        private readonly HttpClientInterface $httpClient,
        #[Autowire('%env(string:OPENROUTER_API_KEY)%')]
        private readonly string $apiKey,
        #[Autowire('%env(string:OPENROUTER_URL)%')]
        private readonly string $apiUrl,
    ) {
    }

    /**
     * Generates a professional, structured contract description from a short user hint.
     *
     * @param string $hint      Short user input like "website symfony 3 pages"
     * @param string $title     The contract title for additional context
     * @param float  $price     The contract price for context
     * @return string The generated professional description
     */
    public function generateDescription(string $hint, string $title = '', float $price = 0): string
    {
        if ('' === trim($this->apiKey)) {
            return 'Le service IA n\'est pas configuré.';
        }

        $prompt = <<<PROMPT
Tu es un expert juridique spécialisé dans la rédaction de contrats freelance.
Génère une description de contrat professionnelle, claire et structurée en français.

Informations fournies par l'utilisateur:
- Titre du contrat: {$title}
- Indications: {$hint}
- Budget: {$price} TND

Rédige la description avec les sections suivantes:
1. PÉRIMÈTRE DES TRAVAUX: Description détaillée des services à fournir
2. LIVRABLES: Liste numérotée des éléments à livrer
3. DÉLAIS: Conditions de livraison et délais
4. CONDITIONS: Conditions de paiement et de révisions

Important:
- Sois professionnel mais concis (max 200 mots)
- Utilise un ton formel et juridique
- Ne répète pas le titre
- Ne mets pas de prix dans la description
- Écris directement la description sans préambule
PROMPT;

        return $this->callAi($prompt, 600);
    }

    /**
     * Analyzes a payment amount and determines if it seems suspicious
     * compared to the platform average.
     *
     * @param float  $amount          The amount being paid
     * @param float  $platformAvg     Average contract price on the platform
     * @param float  $platformMax     Maximum contract price on the platform
     * @param string $contractTitle   Title for context
     * @return array{suspicious: bool, reason: string, suggestion: string|null}
     */
    public function analyzePaymentRisk(
        float $amount,
        float $platformAvg,
        float $platformMax,
        string $contractTitle = ''
    ): array {
        // Quick heuristic checks (no AI call needed for obvious cases)
        if ($amount <= 0) {
            return [
                'suspicious' => true,
                'reason' => 'Le montant est négatif ou nul.',
                'suggestion' => null,
            ];
        }

        if ($amount > 1_000_000) {
            return [
                'suspicious' => true,
                'reason' => sprintf(
                    'Le montant de %.2f TND semble extrêmement élevé. La moyenne sur la plateforme est de %.2f TND.',
                    $amount,
                    $platformAvg
                ),
                'suggestion' => $this->suggestCorrectAmount($amount),
            ];
        }

        // If the amount is more than 10x the platform max, flag it
        if ($platformMax > 0 && $amount > $platformMax * 10) {
            return [
                'suspicious' => true,
                'reason' => sprintf(
                    'Ce montant (%.2f TND) dépasse 10 fois le contrat le plus élevé sur la plateforme (%.2f TND).',
                    $amount,
                    $platformMax
                ),
                'suggestion' => $this->suggestCorrectAmount($amount),
            ];
        }

        // If the amount is more than 20x the average, use AI for deeper analysis
        if ($platformAvg > 0 && $amount > $platformAvg * 20) {
            $aiAnalysis = $this->aiAnalyzeAmount($amount, $platformAvg, $contractTitle);

            return [
                'suspicious' => true,
                'reason' => $aiAnalysis,
                'suggestion' => $this->suggestCorrectAmount($amount),
            ];
        }

        return [
            'suspicious' => false,
            'reason' => '',
            'suggestion' => null,
        ];
    }

    /**
     * Tries to detect if the user accidentally added too many digits
     * and suggests a corrected amount.
     */
    private function suggestCorrectAmount(float $amount): ?string
    {
        $amountStr = (string) (int) $amount;
        $len = strlen($amountStr);

        if ($len > 6) {
            // Likely extra digits — suggest removing some
            $suggested = substr($amountStr, 0, (int) ceil($len / 2));
            return sprintf('Vouliez-vous dire %s TND?', number_format((float) $suggested, 2, '.', ','));
        }

        return null;
    }

    private function aiAnalyzeAmount(float $amount, float $platformAvg, string $title): string
    {
        $prompt = <<<PROMPT
Tu es un système anti-fraude pour une plateforme freelance tunisienne.
Analyse ce paiement et explique en une phrase pourquoi il semble suspect:

- Montant: {$amount} TND
- Moyenne plateforme: {$platformAvg} TND
- Titre du contrat: {$title}

Réponds en une seule phrase d'avertissement en français, sans préambule.
PROMPT;

        $result = $this->callAi($prompt, 100);

        if (str_starts_with($result, 'Erreur')) {
            return sprintf(
                'Ce montant (%.2f TND) est %.0f fois supérieur à la moyenne de la plateforme (%.2f TND).',
                $amount,
                $amount / max($platformAvg, 1),
                $platformAvg
            );
        }

        return $result;
    }

    private function callAi(string $prompt, int $maxTokens): string
    {
        try {
            $response = $this->httpClient->request('POST', $this->apiUrl, [
                'headers' => [
                    'Authorization' => 'Bearer ' . $this->apiKey,
                    'Content-Type' => 'application/json',
                    'HTTP-Referer' => 'https://5ademni.tn',
                    'X-Title' => '5ademni.tn Contracts AI',
                ],
                'json' => [
                    'model' => 'google/gemma-3-27b-it:free',
                    'messages' => [
                        ['role' => 'user', 'content' => $prompt],
                    ],
                    'temperature' => 0.5,
                    'max_tokens' => $maxTokens,
                ],
            ]);

            $data = $response->toArray(false);

            if (isset($data['choices'][0]['message']['content']) && is_string($data['choices'][0]['message']['content'])) {
                $content = trim($data['choices'][0]['message']['content']);
                if ('' !== $content) {
                    return $content;
                }
            }

            return 'Erreur: Impossible de générer une réponse IA.';
        } catch (\Throwable $e) {
            return 'Erreur: ' . $e->getMessage();
        }
    }
}
