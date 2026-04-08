<?php

namespace App\Controller;

use App\Entity\Conversation;
use App\Entity\User;
use App\Repository\ConversationRepository;
use App\Repository\MessageRepository;
use App\Service\SummarizeService;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

#[Route('/api')]
class ApiSummarizeController extends AbstractController
{
    #[Route('/conversations/{conversationId}/summarize', name: 'api_summarize_conversation', methods: ['GET'])]
    public function summarizeConversation(int $conversationId, ConversationRepository $conversationRepository, MessageRepository $messageRepository, SummarizeService $summarizeService): JsonResponse
    {
        $this->denyAccessUnlessGranted('ROLE_USER');

        /** @var User $user */
        $user = $this->getUser();
        $conversation = $conversationRepository->findOneForUser($conversationId, $user);

        if (!$conversation instanceof Conversation) {
            return new JsonResponse(['success' => false, 'detail' => 'Conversation not found'], Response::HTTP_NOT_FOUND);
        }

        try {
            $messages = array_reverse($messageRepository->findRecentByConversation($conversation, 40));
            $messagesData = [];

            foreach ($messages as $message) {
                $messagesData[] = [
                    'contenu' => $message->getContenu(),
                    'senderName' => $message->getSenderDisplayName($conversation),
                    'dateEnvoi' => $message->getDateEnvoi()?->format('d/m/Y H:i') ?? '',
                ];
            }

            return new JsonResponse([
                'success' => true,
                'summary' => $summarizeService->summarizeMessages($messagesData),
            ]);
        } catch (\Throwable $exception) {
            return new JsonResponse([
                'success' => false,
                'detail' => 'Error generating summary: '.$exception->getMessage(),
            ], Response::HTTP_INTERNAL_SERVER_ERROR);
        }
    }
}
