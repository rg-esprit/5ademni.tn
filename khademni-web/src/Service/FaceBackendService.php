<?php

namespace App\Service;

use Symfony\Component\DependencyInjection\Attribute\Autowire;
use Symfony\Component\HttpFoundation\File\UploadedFile;
use Symfony\Component\Mime\Part\DataPart;
use Symfony\Component\Mime\Part\Multipart\FormDataPart;
use Symfony\Contracts\HttpClient\Exception\TransportExceptionInterface;
use Symfony\Contracts\HttpClient\HttpClientInterface;

class FaceBackendService
{
    public function __construct(
        private readonly HttpClientInterface $httpClient,
        #[Autowire('%env(string:FACE_BACKEND_URL)%')]
        private readonly string $baseUrl,
    ) {
    }

    /**
     * @param UploadedFile[] $frames
     *
     * @return array<string, mixed>
     */
    public function login(array $frames): array
    {
        return $this->sendMultipart('/face-login', $frames);
    }

    /**
     * @param UploadedFile[] $frames
     *
     * @return array<string, mixed>
     */
    public function enroll(int $userId, array $frames): array
    {
        return $this->sendMultipart('/enroll', $frames, [
            'user_id' => (string) $userId,
        ]);
    }

    /**
     * @param UploadedFile[] $frames
     * @param array<string, string> $fields
     *
     * @return array<string, mixed>
     */
    private function sendMultipart(string $path, array $frames, array $fields = []): array
    {
        $multipartFields = $fields;

        foreach ($frames as $index => $frame) {
            if (!$frame instanceof UploadedFile) {
                continue;
            }

            $multipartFields[] = [
                'frames' => DataPart::fromPath(
                    $frame->getPathname(),
                    $frame->getClientOriginalName() ?: sprintf('frame-%d.jpg', $index + 1),
                    $frame->getMimeType() ?: 'image/jpeg',
                ),
            ];
        }

        $formData = new FormDataPart($multipartFields);

        try {
            $response = $this->httpClient->request('POST', rtrim($this->baseUrl, '/').$path, [
                'headers' => $formData->getPreparedHeaders()->toArray(),
                'body' => $formData->bodyToIterable(),
            ]);
        } catch (TransportExceptionInterface $exception) {
            return [
                'success' => false,
                'detail' => 'The Face ID backend is unreachable right now.',
            ];
        }

        $data = json_decode($response->getContent(false), true);

        if (!is_array($data)) {
            return [
                'success' => false,
                'detail' => 'The Face ID backend returned an invalid response.',
            ];
        }

        return $data;
    }
}
