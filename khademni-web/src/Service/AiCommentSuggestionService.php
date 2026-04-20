<?php

namespace App\Service;

use Symfony\Component\DependencyInjection\Attribute\Autowire;
use Symfony\Contracts\HttpClient\Exception\TransportExceptionInterface;
use Symfony\Contracts\HttpClient\HttpClientInterface;

class AiCommentSuggestionService
{
    // Exactement les mêmes modèles gratuits que ReviewAiService (qui fonctionne déjà)
    private const CHAT_MODELS = [
        'mistralai/mistral-small-3.1-24b-instruct:free',
        'google/gemma-3-27b-it:free',
        'meta-llama/llama-3.3-70b-instruct:free',
        'qwen/qwen3-4b:free',
        'cognitivecomputations/dolphin-mistral-24b-venice-edition:free',
        'liquid/lfm-2.5-1.2b-instruct:free',
    ];

    public function __construct(
        private readonly HttpClientInterface $httpClient,
        #[Autowire('%env(string:OPENROUTER_API_KEY)%')]
        private readonly string $apiKey,
        #[Autowire('%env(string:OPENROUTER_URL)%')]
        private readonly string $endpoint,
    ) {
    }

    public function suggest(string $articleTitle, string $articleContent, string $keyword = ''): array
    {
        if ('' === trim($this->apiKey)) {
            throw new \RuntimeException('Le générateur de commentaires IA n\'est pas configuré.');
        }

        $systemPrompt = 'Tu es un assistant qui aide à écrire des commentaires pour des articles. '
            . 'Tu DOIS répondre avec exactement 3 lignes. Chaque ligne est un commentaire séparé. '
            . 'Format OBLIGATOIRE (une suggestion par ligne, numérotée) :' . "\n"
            . '1. Premier commentaire ici' . "\n"
            . '2. Deuxième commentaire ici' . "\n"
            . '3. Troisième commentaire ici' . "\n"
            . 'Chaque commentaire fait 1 à 2 phrases maximum. Pas d\'introduction ni de conclusion.';

        $userPrompt = "Article: \"{$articleTitle}\".";
        if (!empty($keyword)) {
            $userPrompt .= " L'utilisateur a commencé à écrire: \"{$keyword}\". Complète cette idée dans chaque suggestion.";
        }

        $lastError = 'Tous les modèles IA ont échoué. Réessayez dans un moment.';

        foreach (self::CHAT_MODELS as $model) {
            try {
                $response = $this->httpClient->request('POST', $this->endpoint, [
                    'headers' => [
                        'Authorization' => 'Bearer ' . $this->apiKey,
                        'Content-Type' => 'application/json',
                        'HTTP-Referer' => 'https://5ademni.tn',
                        'X-Title' => '5ademni.tn Comments',
                    ],
                    'json' => [
                        'model' => $model,
                        'messages' => [
                            [
                                'role' => 'system',
                                'content' => $systemPrompt,
                            ],
                            [
                                'role' => 'user',
                                'content' => $userPrompt,
                            ],
                        ],
                    ],
                ]);
            } catch (TransportExceptionInterface $exception) {
                $lastError = 'Le service IA est inaccessible.';
                continue;
            }

            $data = $response->toArray(false);

            if (isset($data['choices'][0]['message']['content']) && is_string($data['choices'][0]['message']['content'])) {
                $content = trim($data['choices'][0]['message']['content']);

                if ('' !== $content) {
                    $suggestions = $this->parseResponse($content);

                    if (count($suggestions) > 0) {
                        return array_slice($suggestions, 0, 3);
                    }
                }
            }

            // Log model error and try next model
            if (isset($data['error']['message']) && is_string($data['error']['message'])) {
                $lastError = $data['error']['message'];
            } else {
                $lastError = sprintf('Le modèle %s n\'a retourné aucun contenu.', $model);
            }
        }

        throw new \RuntimeException($lastError);
    }

    /**
     * Parse la réponse de l'IA en suggestions séparées.
     * Essaye plusieurs méthodes de parsing pour gérer les différents formats de réponse.
     */
    private function parseResponse(string $content): array
    {
        $suggestions = [];

        // Méthode 1 : Séparer par lignes numérotées (1. ... 2. ... 3. ...)
        if (preg_match_all('/^\s*\d+[\.\)]\s*(.+)$/m', $content, $matches)) {
            $suggestions = $matches[1];
        }

        // Méthode 2 : Si pas de numérotation, séparer par "|||"
        if (count($suggestions) < 2 && str_contains($content, '|||')) {
            $suggestions = explode('|||', $content);
        }

        // Méthode 3 : Séparer par lignes non vides
        if (count($suggestions) < 2) {
            $lines = explode("\n", $content);
            $suggestions = array_filter($lines, fn($line) => !empty(trim($line)));
        }

        // Nettoyage final de chaque suggestion
        $suggestions = array_map(function ($s) {
            $s = trim($s);
            // Supprimer les numéros, tirets, puces, pipes au début
            $s = preg_replace('/^[\d\.\)\-\•\*\|\s]+/', '', $s);
            // Supprimer les pipes à la fin
            $s = rtrim($s, '| ');
            return trim($s);
        }, $suggestions);

        // Filtrer les entrées vides
        $suggestions = array_filter($suggestions, fn($s) => !empty($s) && strlen($s) > 5);

        return array_values($suggestions);
    }
}
