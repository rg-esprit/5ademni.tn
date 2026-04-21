<?php

namespace App\Service;

use Psr\Log\LoggerInterface;
use Symfony\Component\DependencyInjection\Attribute\Autowire;
use Symfony\Contracts\HttpClient\HttpClientInterface;

/**
 * Hybrid AI Audit Service.
 * 
 * Strategy:
 * 1. Try Native Google Gemini (Fastest/Best)
 * 2. If Gemini fails (Quota/Error), fall back to OpenRouter multi-model loop.
 */
class AiAuditService
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

    public function auditContract(string $titre, string $description, ?float $prix): array
    {
        $prixFormat = $prix !== null ? round($prix, 2) . ' TND' : 'Non spécifié';
        $descriptionSafe = trim($description) === '' ? 'Aucune description fournie.' : $description;
        $titreSafe = trim($titre) === '' ? 'Aucun titre' : $titre;

        $prompt = <<<PROMPT
Tu es un avocat spécialisé en contrats freelance. Un utilisateur rédige un contrat sur notre plateforme Khademni.tn.
Analyse les données du contrat en cours de rédaction:
- Titre: {$titreSafe}
- Description: {$descriptionSafe}
- Prix demandé: {$prixFormat}

Ta mission est d'évaluer le risque de litige basé sur la clarté et l'exhaustivité de ces informations.
Réponds **UNIQUEMENT** avec un objet JSON strict.

Le format JSON doit être exactement celui-ci :
{
    "score": <entier de 1 à 10>,
    "advise": "<Une phrase de conseil direct en français>",
    "status": "<DANGER/WARNING/SAFE>"
}
PROMPT;

        // --- STEP 1: Try Native Gemini ---
        if (trim($this->geminiKey) !== '') {
            try {
                $url = $this->geminiUrl . '?key=' . $this->geminiKey;
                $response = $this->httpClient->request('POST', $url, [
                    'json' => ['contents' => [['parts' => [['text' => $prompt]]]]],
                    'timeout' => 8,
                ]);

                if ($response->getStatusCode() === 200) {
                    $res = $this->parseJsonResponse($response->toArray(false)['candidates'][0]['content']['parts'][0]['text'] ?? '');
                    if ($res) return $res;
                }
            } catch (\Throwable $e) {
                $this->logger->warning("AiAuditService: Gemini failed, falling back. " . $e->getMessage());
            }
        }

        // --- STEP 2: Fallback to OpenRouter ---
        return $this->callOpenRouterFailover($prompt);
    }

    private function callOpenRouterFailover(string $prompt): array
    {
        $models = [
            'openrouter/free',
            'google/gemma-2-9b-it:free',
            'mistralai/mistral-7b-instruct:free',
            'meta-llama/llama-3-8b-instruct:free'
        ];

        foreach ($models as $model) {
            try {
                usleep(500000); // 0.5s throttle

                $response = $this->httpClient->request('POST', $this->openRouterUrl, [
                    'headers' => [
                        'Authorization' => "Bearer {$this->openRouterKey}",
                        'X-Title' => 'Khademni Audit',
                    ],
                    'json' => [
                        'model' => $model,
                        'messages' => [['role' => 'user', 'content' => $prompt]],
                        'temperature' => 0.1,
                    ],
                    'timeout' => 12,
                ]);

                if ($response->getStatusCode() === 200) {
                    $resContent = $response->toArray(false);
                    $content = $resContent['choices'][0]['message']['content'] ?? '';
                    $res = $this->parseJsonResponse($content);
                    if ($res) return $res;
                }
            } catch (\Throwable $e) { continue; }
        }

        return ['score' => 5, 'advise' => 'Audit en mode dégradé (IA surchargée).', 'status' => 'WARNING'];
    }

    private function parseJsonResponse(string $content): ?array
    {
        $content = trim($content);
        $content = preg_replace('/^```json\s*/i', '', $content);
        $content = preg_replace('/^```\s*/', '', $content);
        $content = preg_replace('/\s*```$/', '', $content);
        $json = json_decode($content, true);

        if (json_last_error() === JSON_ERROR_NONE && isset($json['score'])) {
            return [
                'score' => (int) $json['score'],
                'advise' => (string) ($json['advise'] ?? ''),
                'status' => in_array($json['status'] ?? '', ['DANGER', 'WARNING', 'SAFE']) ? $json['status'] : 'WARNING',
            ];
        }
        return null;
    }
}
