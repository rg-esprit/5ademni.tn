<?php

namespace App\Service;

use Symfony\Component\DependencyInjection\Attribute\Autowire;
use Symfony\Contracts\HttpClient\Exception\TransportExceptionInterface;
use Symfony\Contracts\HttpClient\HttpClientInterface;

class PromptSafetyService
{
    private const DEFAULT_USER_IP = '127.0.0.1';
    private const REQUEST_TIMEOUT = 15;

    public function __construct(
        private readonly HttpClientInterface $httpClient,
        #[Autowire('%env(string:SAFEPROMPT_API_KEY)%')]
        private readonly string $apiKey,
        #[Autowire('%env(string:SAFEPROMPT_URL)%')]
        private readonly string $endpoint,
    ) {
    }

    public function assertSafe(string $prompt, ?string $userIp = null): void
    {
        if ('' === trim($this->apiKey) || '' === trim($this->endpoint)) {
            throw new \RuntimeException('AI prompt validation is not configured right now.');
        }

        $resolvedIp = is_string($userIp) && false !== filter_var($userIp, FILTER_VALIDATE_IP)
            ? $userIp
            : self::DEFAULT_USER_IP;

        try {
            $response = $this->httpClient->request('POST', $this->endpoint, [
                'headers' => [
                    'X-API-Key' => $this->apiKey,
                    'X-User-IP' => $resolvedIp,
                    'Content-Type' => 'application/json',
                ],
                'json' => [
                    'prompt' => trim($prompt),
                ],
                'timeout' => self::REQUEST_TIMEOUT,
            ]);
        } catch (TransportExceptionInterface $exception) {
            throw new \RuntimeException('The prompt safety service is unreachable right now.', 0, $exception);
        }

        try {
            $data = $response->toArray(false);
        } catch (\Throwable $exception) {
            throw new \RuntimeException('The prompt safety service returned an invalid response.', 0, $exception);
        }

        if (false === ($data['safe'] ?? null)) {
            throw new \RuntimeException('That AI prompt looks unsafe and was blocked. Please describe the review content directly.');
        }

        if (true !== ($data['safe'] ?? null)) {
            throw new \RuntimeException('The prompt safety service could not verify this request. Please try again.');
        }
    }
}
