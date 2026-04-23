<?php

namespace App\Service;

use Symfony\Contracts\HttpClient\HttpClientInterface;

class GoogleCalendarService
{
    private ?string $accessToken = null;
    private int $accessTokenExpiration = 0;

    public function __construct(
        private readonly HttpClientInterface $httpClient,
        private readonly string $serviceAccountJsonPath,
        private readonly string $calendarId
    ) {
    }

    public function isConfigured(): bool
    {
        return '' !== trim($this->serviceAccountJsonPath)
            && file_exists($this->serviceAccountJsonPath)
            && is_readable($this->serviceAccountJsonPath);
    }

    public function createEvent(string $summary, string $description, \DateTimeImmutable $start, \DateTimeImmutable $end, array $attendeesEmails = []): bool
    {
        if (!$this->isConfigured()) {
            throw new \RuntimeException('Google Calendar service is not configured.');
        }

        $accessToken = $this->getAccessToken();
        $calendarId = rawurlencode($this->calendarId);
        $event = [
            'summary' => $summary,
            'description' => $description,
            'start' => [
                'dateTime' => $start->format('c'),
                'timeZone' => $start->getTimezone()->getName(),
            ],
            'end' => [
                'dateTime' => $end->format('c'),
                'timeZone' => $end->getTimezone()->getName(),
            ],
        ];

        if (!empty($attendeesEmails)) {
            $event['attendees'] = array_map(static function (string $email) {
                return ['email' => $email];
            }, array_values(array_unique($attendeesEmails)));
        }

        $response = $this->httpClient->request('POST', sprintf('https://www.googleapis.com/calendar/v3/calendars/%s/events?sendUpdates=all', $calendarId), [
            'headers' => [
                'Authorization' => 'Bearer '.$accessToken,
            ],
            'json' => $event,
        ]);

        $statusCode = $response->getStatusCode();
        if (201 === $statusCode || 200 === $statusCode) {
            return true;
        }

        $content = $response->getContent(false);
        throw new \RuntimeException(sprintf('Google Calendar event creation failed (%s): %s', $statusCode, $content));
    }

    private function getAccessToken(): string
    {
        if (null !== $this->accessToken && time() + 30 < $this->accessTokenExpiration) {
            return $this->accessToken;
        }

        $credentials = $this->loadServiceAccountCredentials();
        $privateKey = $this->getPrivateKey($credentials['private_key'] ?? '');
        $clientEmail = $credentials['client_email'] ?? '';
        $tokenUri = $credentials['token_uri'] ?? 'https://oauth2.googleapis.com/token';

        if ('' === $clientEmail || '' === $tokenUri) {
            throw new \RuntimeException('Google Calendar service account JSON is invalid.');
        }

        $now = time();
        $jwtHeader = $this->base64UrlEncode(json_encode(['alg' => 'RS256', 'typ' => 'JWT']));
        $jwtPayload = $this->base64UrlEncode(json_encode([
            'iss' => $clientEmail,
            'scope' => 'https://www.googleapis.com/auth/calendar.events',
            'aud' => $tokenUri,
            'exp' => $now + 3600,
            'iat' => $now,
        ]));

        $signature = '';
        if (!openssl_sign($jwtHeader.'.'.$jwtPayload, $signature, $privateKey, OPENSSL_ALGO_SHA256)) {
            throw new \RuntimeException('Unable to sign Google Calendar JWT assertion.');
        }

        $assertion = $jwtHeader.'.'.$jwtPayload.'.'.$this->base64UrlEncode($signature);
        $response = $this->httpClient->request('POST', $tokenUri, [
            'body' => [
                'grant_type' => 'urn:ietf:params:oauth:grant-type:jwt-bearer',
                'assertion' => $assertion,
            ],
        ]);

        $responseData = $response->toArray(false);
        if (!isset($responseData['access_token'])) {
            throw new \RuntimeException('Unable to get access token from Google OAuth response.');
        }

        $this->accessToken = $responseData['access_token'];
        $this->accessTokenExpiration = $now + (int) ($responseData['expires_in'] ?? 3600);

        return $this->accessToken;
    }

    private function loadServiceAccountCredentials(): array
    {
        $content = file_get_contents($this->serviceAccountJsonPath);
        if (false === $content) {
            throw new \RuntimeException(sprintf('Unable to read Google service account JSON from %s.', $this->serviceAccountJsonPath));
        }

        $data = json_decode($content, true);
        if (!is_array($data)) {
            throw new \RuntimeException('Google service account JSON is invalid.');
        }

        return $data;
    }

    private function getPrivateKey(string $privateKey)
    {
        $resource = openssl_pkey_get_private($privateKey);
        if (false === $resource) {
            throw new \RuntimeException('Invalid private key in Google service account JSON.');
        }

        return $resource;
    }

    private function base64UrlEncode(string $value): string
    {
        return rtrim(strtr(base64_encode($value), '+/', '-_'), '=');
    }
}
