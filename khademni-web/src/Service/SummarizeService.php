<?php

namespace App\Service;

use Symfony\Component\DependencyInjection\Attribute\Autowire;
use Symfony\Contracts\HttpClient\HttpClientInterface;

class SummarizeService
{
    public function __construct(
        private readonly HttpClientInterface $httpClient,
        #[Autowire('%env(string:OPENROUTER_API_KEY)%')]
        private readonly string $apiKey,
        #[Autowire('%env(string:OPENROUTER_URL)%')]
        private readonly string $apiUrl,
    ) {
    }

    public function summarizeMessages(array $messages): string
    {
        if ([] === $messages) {
            return 'Aucun message à résumer.';
        }

        if ('' === trim($this->apiKey)) {
            return 'Le résumé IA n’est pas configuré pour le moment.';
        }

        $conversationText = '';
        foreach ($messages as $message) {
            $conversationText .= sprintf("[%s] %s: %s\n", $message['dateEnvoi'], $message['senderName'], $message['contenu']);
        }

        $prompt = sprintf("Résume cette conversation en français de manière concise et claire, sous forme de court paragraphe puis 3 points clés maximum:\n\n%s", $conversationText);

        try {
            $response = $this->httpClient->request('POST', $this->apiUrl, [
                'headers' => [
                    'Authorization' => 'Bearer '.$this->apiKey,
                    'Content-Type' => 'application/json',
                    'HTTP-Referer' => 'https://5ademni.tn',
                    'X-Title' => '5ademni.tn Messaging',
                ],
                'json' => [
                    'model' => 'google/gemma-3-27b-it:free',
                    'messages' => [
                        [
                            'role' => 'user',
                            'content' => $prompt,
                        ],
                    ],
                    'temperature' => 0.4,
                    'max_tokens' => 450,
                ],
            ]);

            $data = $response->toArray(false);

            if (isset($data['choices'][0]['message']['content']) && is_string($data['choices'][0]['message']['content'])) {
                $content = trim($data['choices'][0]['message']['content']);

                if ('' !== $content) {
                    return $content;
                }
            }

            return 'Impossible de générer un résumé exploitable pour cette conversation.';
        } catch (\Throwable $exception) {
            return 'Erreur lors de la génération du résumé: '.$exception->getMessage();
        }
    }
}
