<?php

namespace App\Controller;

use App\Service\BlobStorageService;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

class BlobProxyController extends AbstractController
{
    #[Route('/blob-proxy', name: 'app_blob_proxy', methods: ['GET'])]
    public function proxy(Request $request, BlobStorageService $blobStorageService): Response
    {
        $blobUrl = (string) $request->query->get('url', '');

        if (!$blobUrl || !str_starts_with($blobUrl, 'https://wse98pp6r5irr1e7.private.blob.vercel-storage.com/')) {
            return new Response('Invalid blob URL', Response::HTTP_BAD_REQUEST);
        }

        try {
            $blobData = $blobStorageService->download($blobUrl);

            return new Response(
                $blobData['content'],
                Response::HTTP_OK,
                ['Content-Type' => $blobData['contentType']]
            );
        } catch (\Throwable $exception) {
            $this->addFlash('error', 'Unable to load blob: '.$exception->getMessage());

            return new Response('Proxy Error: '.$exception->getMessage(), Response::HTTP_INTERNAL_SERVER_ERROR);
        }
    }
}

