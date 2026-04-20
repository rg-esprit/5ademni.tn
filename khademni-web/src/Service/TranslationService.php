<?php

namespace App\Service;

use Symfony\Contracts\HttpClient\HttpClientInterface;

class TranslationService
{
    private $client;

    public function __construct(HttpClientInterface $client)
    {
        $this->client = $client;
    }


    
public function translate(string $text, string $targetLang, string $sourceLang = 'en'): string
{
    // Liste des langues supportées (exemple)
    $supportedLanguages = ['en', 'fr', 'es', 'de', 'it', 'zh-CN', 'ar'];

    // Vérifiez si les langues source et cible sont supportées
    if (!in_array($sourceLang, $supportedLanguages)) {
        $sourceLang = 'en'; // Langue par défaut
    }
    if (!in_array($targetLang, $supportedLanguages)) {
        throw new \InvalidArgumentException('Langue cible non supportée.');
    }

    // Vérifiez si les langues source et cible sont identiques
    if ($sourceLang === $targetLang) {
        return $text; // Retournez le texte d'origine sans traduction
    }

    $url = 'https://api.mymemory.translated.net/get';

    $response = $this->client->request('GET', $url, [
        'query' => [
            'q' => $text,
            'langpair' => $sourceLang . '|' . $targetLang,
        ],
    ]);

    $data = $response->toArray();

    return $data['responseData']['translatedText'] ?? $text;
}
}