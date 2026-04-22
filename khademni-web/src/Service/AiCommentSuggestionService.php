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

        $keyword = trim($keyword);
        
        $systemPrompt = "Tu es un assistant d'écriture expert. Ton rôle est de générer 3 suggestions de commentaires. \n"
            . "Article : {$articleTitle}\n"
            . "CONSIGNE CRITIQUE : Tu DOIS générer exactement 3 lignes. \n";

        if (!empty($keyword)) {
            $systemPrompt .= "Chaque ligne DOIT impérativement COMMENCER par le texte exactement comme ceci : \"{$keyword}\". \n"
                . "Tu complètes la suite de la pensée de 3 façons différentes et originales.";
        } else {
            $systemPrompt .= "Génère 3 commentaires variés, enthousiastes, commençant par 'Bonjour', 'Super' ou 'Merci'.";
        }

        $userPrompt = "Génère 3 suggestions pour l'article \"{$articleTitle}\". " . (!empty($keyword) ? "Rappel : commence par \"{$keyword}\"." : "");

        $lastError = 'Pas de réponse de l\'IA.';

        foreach (self::CHAT_MODELS as $model) {
            try {
                $response = $this->httpClient->request('POST', $this->endpoint, [
                    'headers' => [
                        'Authorization' => 'Bearer ' . $this->apiKey,
                        'Content-Type' => 'application/json',
                    ],
                    'json' => [
                        'model' => $model,
                        'messages' => [
                            ['role' => 'system', 'content' => $systemPrompt],
                            ['role' => 'user', 'content' => $userPrompt],
                        ],
                        'temperature' => 0.7,
                    ],
                ]);
                
                $data = $response->toArray(false);
                $content = $data['choices'][0]['message']['content'] ?? '';

                if ('' !== trim($content)) {
                    $suggestions = $this->parseResponse($content, $keyword);
                    if (count($suggestions) >= 2) {
                        return array_slice($suggestions, 0, 3);
                    }
                }
            } catch (\Exception $e) {
                $lastError = $e->getMessage();
            }
        }

        throw new \RuntimeException($lastError);
    }

    private function parseResponse(string $content, string $keyword): array
    {
        // Nettoyer les blocs de code markdown si présents
        $content = preg_replace('/```[a-z]*|```/i', '', $content);
        
        $lines = explode("\n", $content);
        $results = [];

        foreach ($lines as $line) {
            $line = trim($line);
            if (empty($line)) continue;

            // Enlever la numérotation "1. ", "2) ", etc.
            $cleanLine = preg_replace('/^\s*\d+[\.\)]\s*/', '', $line);
            $cleanLine = trim($cleanLine, "\" \t\n\r\0\x0B");

            if (strlen($cleanLine) > 5) {
                // Si on a un keyword, s'assurer que la ligne commence par celui-ci
                if (!empty($keyword)) {
                    if (!str_starts_with(strtolower($cleanLine), strtolower($keyword))) {
                        // Si l'IA a oublié le prefix, on le rajoute proprement
                        $results[] = $keyword . ' ' . ltrim($cleanLine);
                    } else {
                        $results[] = $cleanLine;
                    }
                } else {
                    $results[] = $cleanLine;
                }
            }
        }

        return array_values(array_unique($results));
    }
}
