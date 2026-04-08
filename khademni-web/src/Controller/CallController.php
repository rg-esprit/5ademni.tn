<?php

namespace App\Controller;

use App\Entity\Conversation;
use App\Entity\User;
use App\Repository\ConversationRepository;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\RequestStack;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

class CallController extends AbstractController
{
    #[Route('/messages/{id}/call', name: 'app_call_start', methods: ['POST'])]
    public function startCall(int $id, Request $request, ConversationRepository $conversationRepository, RequestStack $requestStack): JsonResponse
    {
        $this->denyAccessUnlessGranted('ROLE_USER');

        /** @var User $user */
        $user = $this->getUser();
        $conversation = $conversationRepository->findOneForUser($id, $user);

        if (!$conversation instanceof Conversation) {
            return new JsonResponse(['success' => false, 'detail' => 'Conversation not found'], Response::HTTP_NOT_FOUND);
        }

        $callType = (string) $request->request->get('type', 'audio');

        // Créer une session d'appel
        $sessionId = bin2hex(random_bytes(16));
        $callData = [
            'sessionId' => $sessionId,
            'initiatorId' => $user->getId(),
            'conversationId' => $conversation->getId(),
            'type' => $callType,
            'startedAt' => date('Y-m-d H:i:s'),
        ];

        $requestStack->getSession()->set('call_'.$sessionId, $callData);

        $otherUser = $conversation->getOtherParticipant($user);

        return new JsonResponse([
            'success' => true,
            'sessionId' => $sessionId,
            'otherUserId' => $otherUser?->getId(),
        ]);
    }

    #[Route('/messages/{id}/call/{sessionId}/join', name: 'app_call_join', methods: ['GET'])]
    public function joinCall(int $id, string $sessionId, ConversationRepository $conversationRepository, RequestStack $requestStack): Response
    {
        $this->denyAccessUnlessGranted('ROLE_USER');

        /** @var User $user */
        $user = $this->getUser();
        $conversation = $conversationRepository->findOneForUser($id, $user);

        if (!$conversation instanceof Conversation) {
            $this->addFlash('error', 'Conversation not found');
            return $this->redirectToRoute('app_conversations');
        }

        return $this->render('call/meeting.html.twig', [
            'conversation' => $conversation,
            'currentUser' => $user,
            'sessionId' => $sessionId,
            'callType' => $requestStack->getSession()->get('call_'.$sessionId)['type'] ?? 'audio',
        ]);
    }

    #[Route('/api/call/{sessionId}/status', name: 'api_call_status', methods: ['GET'])]
    public function getCallStatus(string $sessionId, RequestStack $requestStack): JsonResponse
    {
        $call = $requestStack->getSession()->get('call_'.$sessionId);

        if (!is_array($call)) {
            return new JsonResponse(['success' => false, 'detail' => 'Call session not found'], Response::HTTP_NOT_FOUND);
        }

        return new JsonResponse([
            'success' => true,
            'call' => $call,
        ]);
    }

    #[Route('/api/call/{sessionId}/end', name: 'api_call_end', methods: ['POST'])]
    public function endCall(string $sessionId, RequestStack $requestStack): JsonResponse
    {
        $requestStack->getSession()->remove('call_'.$sessionId);

        return new JsonResponse(['success' => true]);
    }
}
