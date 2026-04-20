<?php

namespace App\Service;

use Symfony\Contracts\HttpClient\HttpClientInterface;

class SentimentAnalysisService
{
    private const API_URL = 'https://router.huggingface.co/hf-inference/models/lxyuan/distilbert-base-multilingual-cased-sentiments-student';

    public function __construct(
        private HttpClientInterface $httpClient, 
        private string $apiKey // La même api_key Hugging Face que vous avez déjà configurée
    ) {}

    public function isPositif(string $text): bool
    {
        try {
            $response = $this->httpClient->request('POST', self::API_URL, [
                'headers' => [
                    'Authorization' => 'Bearer ' . $this->apiKey,
                ],
                'json' => ['inputs' => $text],
            ]);

            $data = $response->toArray();
            $sentimentLabel = $data[0][0]['label'] ?? 'neutral';

            // Les labels du modèle peuvent être 'positive', 'negative' ou 'neutral' (ou des étoiles)
            return in_array($sentimentLabel, ['positive', '5 stars', '4 stars']);
            
        } catch (\Exception $e) {
            // En cas d'erreur de l'API, on considère que ce n'est pas "positif"
            return false;
        }
    }
}
