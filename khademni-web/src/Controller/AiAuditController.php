<?php

namespace App\Controller;

use App\Service\AiAuditService;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\Routing\Annotation\Route;

#[Route('/api/ai')]
class AiAuditController extends AbstractController
{
    #[Route('/audit', name: 'api_ai_contract_audit', methods: ['POST'])]
    public function audit(Request $request, AiAuditService $auditService): JsonResponse
    {
        $data = json_decode($request->getContent(), true);
        
        $titre = (string) ($data['titre'] ?? '');
        $description = (string) ($data['description'] ?? '');
        $prix = isset($data['prix']) ? (float) $data['prix'] : null;

        if ('' === trim($titre) && '' === trim($description)) {
            return new JsonResponse([
                'score' => 0,
                'advise' => 'Veuillez saisir un titre ou une description pour lancer l\'audit.',
                'status' => 'WARNING'
            ]);
        }

        $result = $auditService->auditContract($titre, $description, $prix);

        return new JsonResponse($result);
    }
}
