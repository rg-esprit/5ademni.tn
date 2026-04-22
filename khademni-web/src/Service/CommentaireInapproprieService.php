<?php

namespace App\Service;

use Symfony\Contracts\HttpClient\HttpClientInterface;

class CommentaireInapproprieService
{
    private const API_URL = 'https://router.huggingface.co/hf-inference/models/unitary/toxic-bert';
    private const TOXIC_THRESHOLD = 0.05;

    private HttpClientInterface $httpClient;
    private string $apiKey;

    public function __construct(HttpClientInterface $httpClient, string $apiKey)
    {
        $this->httpClient = $httpClient;
        $this->apiKey = $apiKey;
    }

    public function isCommentAcceptable(string $comment): bool
    {
        try {
            $response = $this->httpClient->request('POST', self::API_URL, [
                'headers' => [
                    'Authorization' => 'Bearer ' . $this->apiKey,
                    'Content-Type' => 'application/json',
                    'Accept' => 'application/json',
                    'X-Use-Cache' => 'false',
                ],
                'json' => [
                    'inputs' => $comment,
                ],
            ]);

            if (200 !== $response->getStatusCode()) {
                $error = $response->getContent(false);
                throw new \RuntimeException('Erreur API : ' . $error);
            }

            $data = $response->toArray();

            foreach ($data[0] as $prediction) {
                $label = $prediction['label'];
                $score = $prediction['score'];

                if (in_array($label, ['toxic', 'obscene', 'insult'], true) && $score >= self::TOXIC_THRESHOLD) {
                    return false; // Le commentaire est inacceptable
                }
            }

            return true; // Le commentaire est acceptable
        } catch (\Exception $e) {
            // Gérer les erreurs
            throw new \RuntimeException('Erreur lors de la vérification du commentaire : ' . $e->getMessage());
        }
    }
}