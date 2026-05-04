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

    #[Route('/api/call/{sessionId}/offer', name: 'api_call_offer', methods: ['POST'])]
    public function handleOffer(string $sessionId, Request $request, RequestStack $requestStack): JsonResponse
    {
        $this->denyAccessUnlessGranted('ROLE_USER');

        $callData = $requestStack->getSession()->get('call_'.$sessionId);
        if (!is_array($callData)) {
            return new JsonResponse(['success' => false, 'detail' => 'Call not found'], Response::HTTP_NOT_FOUND);
        }

        $offerData = [
            'sdp' => $request->getPayload()->get('sdp', ''),
            'type' => $request->getPayload()->get('type', 'offer'),
            'timestamp' => date('c'),
        ];

        $callData['offer'] = $offerData;
        $requestStack->getSession()->set('call_'.$sessionId, $callData);

        return new JsonResponse(['success' => true]);
    }

    #[Route('/api/call/{sessionId}/answer', name: 'api_call_answer', methods: ['POST'])]
    public function handleAnswer(string $sessionId, Request $request, RequestStack $requestStack): JsonResponse
    {
        $this->denyAccessUnlessGranted('ROLE_USER');

        $callData = $requestStack->getSession()->get('call_'.$sessionId);
        if (!is_array($callData)) {
            return new JsonResponse(['success' => false, 'detail' => 'Call not found'], Response::HTTP_NOT_FOUND);
        }

        $answerData = [
            'sdp' => $request->getPayload()->get('sdp', ''),
            'type' => $request->getPayload()->get('type', 'answer'),
            'timestamp' => date('c'),
        ];

        $callData['answer'] = $answerData;
        $requestStack->getSession()->set('call_'.$sessionId, $callData);

        return new JsonResponse(['success' => true]);
    }

    #[Route('/api/call/{sessionId}/candidate', name: 'api_call_candidate', methods: ['POST'])]
    public function handleCandidate(string $sessionId, Request $request, RequestStack $requestStack): JsonResponse
    {
        $this->denyAccessUnlessGranted('ROLE_USER');

        $callData = $requestStack->getSession()->get('call_'.$sessionId);
        if (!is_array($callData)) {
            return new JsonResponse(['success' => false, 'detail' => 'Call not found'], Response::HTTP_NOT_FOUND);
        }

        if (!isset($callData['candidates'])) {
            $callData['candidates'] = [];
        }

        $candidateData = [
            'candidate' => $request->getPayload()->get('candidate', ''),
            'sdpMLineIndex' => $request->getPayload()->get('sdpMLineIndex'),
            'sdpMid' => $request->getPayload()->get('sdpMid'),
            'timestamp' => date('c'),
        ];

        $callData['candidates'][] = $candidateData;
        $requestStack->getSession()->set('call_'.$sessionId, $callData);

        return new JsonResponse(['success' => true]);
    }

    #[Route('/api/call/{sessionId}/offer', name: 'api_call_get_offer', methods: ['GET'])]
    public function getOffer(string $sessionId, RequestStack $requestStack): JsonResponse
    {
        $callData = $requestStack->getSession()->get('call_'.$sessionId);
        if (!is_array($callData)) {
            return new JsonResponse(['success' => false, 'detail' => 'Call not found'], Response::HTTP_NOT_FOUND);
        }

        return new JsonResponse([
            'success' => true,
            'offer' => $callData['offer'] ?? null,
        ]);
    }

    #[Route('/api/call/{sessionId}/answer', name: 'api_call_get_answer', methods: ['GET'])]
    public function getAnswer(string $sessionId, RequestStack $requestStack): JsonResponse
    {
        $callData = $requestStack->getSession()->get('call_'.$sessionId);
        if (!is_array($callData)) {
            return new JsonResponse(['success' => false, 'detail' => 'Call not found'], Response::HTTP_NOT_FOUND);
        }

        return new JsonResponse([
            'success' => true,
            'answer' => $callData['answer'] ?? null,
        ]);
    }

    #[Route('/api/call/{sessionId}/candidates', name: 'api_call_get_candidates', methods: ['GET'])]
    public function getCandidates(string $sessionId, RequestStack $requestStack): JsonResponse
    {
        $callData = $requestStack->getSession()->get('call_'.$sessionId);
        if (!is_array($callData)) {
            return new JsonResponse(['success' => false, 'detail' => 'Call not found'], Response::HTTP_NOT_FOUND);
        }

        return new JsonResponse([
            'success' => true,
            'candidates' => $callData['candidates'] ?? [],
        ]);
    }
}
