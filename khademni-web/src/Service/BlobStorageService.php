<?php

namespace App\Service;

use Symfony\Component\DependencyInjection\Attribute\Autowire;
use Symfony\Component\HttpFoundation\File\UploadedFile;
use Symfony\Contracts\HttpClient\Exception\TransportExceptionInterface;
use Symfony\Contracts\HttpClient\HttpClientInterface;

class BlobStorageService
{
    private const API_URL = 'https://vercel.com/api/blob';

    public function __construct(
        private readonly HttpClientInterface $httpClient,
        #[Autowire('%env(string:VERCEL_BLOB_READ_WRITE_TOKEN)%')]
        private readonly string $blobReadWriteToken,
    ) {
    }

    public function isConfigured(): bool
    {
        return '' !== trim($this->blobReadWriteToken);
    }

    public function upload(UploadedFile $file): string
    {
        if (!$this->isConfigured()) {
            throw new \RuntimeException('Avatar uploads are not configured yet. Set VERCEL_BLOB_READ_WRITE_TOKEN first.');
        }

        $extension = $file->guessExtension();
        $pathname = 'avatars/'.bin2hex(random_bytes(16)).($extension ? '.'.$extension : '');

        $response = null;
        $statusCode = 0;

        for ($attempt = 1; $attempt <= 3; ++$attempt) {
            try {
                $response = $this->httpClient->request('PUT', self::API_URL.'/?pathname='.$pathname, [
                    'headers' => [
                        'Authorization' => 'Bearer '.$this->blobReadWriteToken,
                        'x-api-version' => '12',
                        'x-vercel-blob-access' => 'private',
                        'x-content-type' => $file->getMimeType() ?: 'application/octet-stream',
                        'Content-Type' => 'application/octet-stream',
                    ],
                    'body' => fopen($file->getPathname(), 'rb'),
                ]);

                $statusCode = $response->getStatusCode();
            } catch (TransportExceptionInterface $exception) {
                if ($attempt < 3) {
                    continue;
                }

                throw new \RuntimeException('Avatar upload failed because Vercel Blob is unreachable.', previous: $exception);
            }

            if (in_array($statusCode, [200, 201], true)) {
                break;
            }

            if ($attempt < 3 && (429 === $statusCode || $statusCode >= 500)) {
                continue;
            }

            throw new \RuntimeException('Avatar upload failed with HTTP '.$statusCode.'.');
        }

        if (null === $response || !in_array($statusCode, [200, 201], true)) {
            throw new \RuntimeException('Avatar upload failed with HTTP '.$statusCode.'.');
        }

        $data = $response->toArray(false);

        if (!isset($data['url']) || !is_string($data['url'])) {
            throw new \RuntimeException('Avatar upload did not return a blob URL.');
        }

        return $data['url'];
    }

    /**
     * @return array{content:string, contentType:string}
     */
    public function download(string $blobUrl): array
    {
        if (!$this->isConfigured()) {
            throw new \RuntimeException('Avatar proxying is not configured yet.');
        }

        try {
            $response = $this->httpClient->request('GET', $blobUrl, [
                'headers' => [
                    'Authorization' => 'Bearer '.$this->blobReadWriteToken,
                ],
            ]);
        } catch (TransportExceptionInterface $exception) {
            throw new \RuntimeException('Unable to reach Vercel Blob.', previous: $exception);
        }

        if (200 !== $response->getStatusCode()) {
            throw new \RuntimeException('Avatar download failed with HTTP '.$response->getStatusCode().'.');
        }

        $headers = $response->getHeaders(false);
        $contentType = $headers['content-type'][0] ?? 'application/octet-stream';

        return [
            'content' => $response->getContent(false),
            'contentType' => $contentType,
        ];
    }
}
