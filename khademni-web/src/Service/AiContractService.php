<?php

namespace App\Service;

use Psr\Log\LoggerInterface;
use Symfony\Component\DependencyInjection\Attribute\Autowire;
use Symfony\Contracts\HttpClient\HttpClientInterface;

/**
 * Hybrid AI Contract Service.
 * Priority 1: Gemini Native
 * Priority 2: OpenRouter Failover
 */
class AiContractService
{
    public function __construct(
        private readonly HttpClientInterface $httpClient,
        #[Autowire('%env(string:GEMINI_API_KEY)%')]
        private readonly string $geminiKey,
        #[Autowire('%env(string:GEMINI_URL)%')]
        private readonly string $geminiUrl,
        #[Autowire('%env(string:OPENROUTER_API_KEY)%')]
        private readonly string $openRouterKey,
        #[Autowire('%env(string:OPENROUTER_URL)%')]
        private readonly string $openRouterUrl,
        private readonly LoggerInterface $logger
    ) {
    }

    public function generateDescription(string $hint, string $title = '', float $price = 0): string
    {
        $prompt = <<<PROMPT
Tu es un expert juridique freelance. Génère une description de contrat professionnelle en français.
Titre: {$title} | Indications: {$hint} | Budget: {$price} TND
Sections: 1. PÉRIMÈTRE, 2. LIVRABLES, 3. DÉLAIS, 4. CONDITIONS.
Concise, sans préambule.
PROMPT;

        return $this->callHybridAi($prompt, 1000);
    }

    public function analyzePaymentRisk(float $amount, float $platformAvg, float $platformMax, string $title = ''): array {
        if ($amount <= 0) return ['suspicious' => true, 'reason' => 'Montant invalide.'];
        if ($amount > 1_000_000 || ($platformMax > 0 && $amount > $platformMax * 10)) {
            return ['suspicious' => true, 'reason' => 'Montant anormalement élevé.', 'suggestion' => $this->suggestCorrectAmount($amount)];
        }
        if ($platformAvg > 0 && $amount > $platformAvg * 20) {
            $prompt = "Analyse ce paiement suspect (Montant: {$amount}, Moyenne: {$platformAvg}, Titre: {$title}). Explique le risque en une phrase en français.";
            return ['suspicious' => true, 'reason' => $this->callHybridAi($prompt, 150), 'suggestion' => $this->suggestCorrectAmount($amount)];
        }
        return ['suspicious' => false];
    }

    private function suggestCorrectAmount(float $amount): ?string {
        $s = (string)(int)$amount;
        if (strlen($s) > 6) {
            $suggested = substr($s, 0, (int)ceil(strlen($s)/2));
            return "Vouliez-vous dire ".number_format((float)$suggested, 2)." TND ?";
        }
        return null;
    }

    private function callHybridAi(string $prompt, int $maxTokens): string
    {
        // 1. Try Gemini
        if (trim($this->geminiKey) !== '') {
            try {
                $response = $this->httpClient->request('POST', $this->geminiUrl . '?key=' . $this->geminiKey, [
                    'json' => ['contents' => [['parts' => [['text' => $prompt]]]]],
                    'timeout' => 10,
                ]);
                if ($response->getStatusCode() === 200) {
                    $text = $response->toArray(false)['candidates'][0]['content']['parts'][0]['text'] ?? '';
                    if (trim($text) !== '') return trim($text);
                }
            } catch (\Throwable $e) {}
        }

        // 2. Try OpenRouter Fallback Logic
        $models = [
            'openrouter/free', // Auto-router for free models (Best chance)
            'google/gemma-2-9b-it:free',
            'mistralai/mistral-7b-instruct:free',
            'microsoft/phi-3-mini-128k-instruct:free',
            'meta-llama/llama-3-8b-instruct:free'
        ];
        
        foreach ($models as $model) {
            try {
                // Throttle slightly to avoid aggressive 429s from the provider
                usleep(500000); // 0.5s

                $response = $this->httpClient->request('POST', $this->openRouterUrl, [
                    'headers' => [
                        'Authorization' => "Bearer {$this->openRouterKey}",
                        'X-Title' => 'Khademni Platform',
                    ],
                    'json' => [
                        'model' => $model,
                        'messages' => [['role' => 'user', 'content' => $prompt]],
                        'max_tokens' => $maxTokens
                    ],
                    'timeout' => 15,
                ]);

                if ($response->getStatusCode() === 200) {
                    $resContent = $response->toArray(false);
                    $text = $resContent['choices'][0]['message']['content'] ?? '';
                    if (trim($text) !== '') return trim($text);
                }
            } catch (\Throwable $e) {
                // Continue to next model if this one fails
                continue;
            }
        }

        return "Erreur: Service IA indisponible (Surcharge).";
    }
}
