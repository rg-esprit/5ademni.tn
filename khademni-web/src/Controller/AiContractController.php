<?php

namespace App\Controller;

use App\Service\AiContractService;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;

/**
 * API endpoints for AI-powered contract features.
 * These are called via AJAX from the contract form (no template changes needed).
 */
#[Route('/api/ai')]
class AiContractController extends AbstractController
{
    #[Route('/generate-description', name: 'api_ai_generate_description', methods: ['POST'])]
    public function generateDescription(Request $request, AiContractService $aiService): JsonResponse
    {
        $this->denyAccessUnlessGranted('ROLE_USER');

        $data = json_decode($request->getContent(), true);

        $hint = trim($data['hint'] ?? '');
        $title = trim($data['title'] ?? '');
        $price = (float) ($data['price'] ?? 0);

        if ('' === $hint) {
            return new JsonResponse([
                'success' => false,
                'error' => 'Veuillez fournir une description courte de ce que vous attendez.',
            ], Response::HTTP_BAD_REQUEST);
        }

        $generated = $aiService->generateDescription($hint, $title, $price);

        return new JsonResponse([
            'success' => !str_starts_with($generated, 'Erreur'),
            'description' => $generated,
        ]);
    }
}
