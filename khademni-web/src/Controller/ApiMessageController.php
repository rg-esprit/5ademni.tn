<?php

namespace App\Controller;

use App\Entity\Conversation;
use App\Entity\User;
use App\Repository\ConversationRepository;
use App\Repository\MessageRepository;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

#[Route('/api')]
class ApiMessageController extends AbstractController
{
    #[Route('/messages/{conversationId}', name: 'api_messages_list', methods: ['GET'])]
    public function getMessages(int $conversationId, Request $request, ConversationRepository $conversationRepository, MessageRepository $messageRepository): JsonResponse
    {
        $this->denyAccessUnlessGranted('ROLE_USER');

        /** @var User $user */
        $user = $this->getUser();
        $conversation = $conversationRepository->findOneForUser($conversationId, $user);

        if (!$conversation instanceof Conversation) {
            return new JsonResponse(['success' => false, 'detail' => 'Conversation not found'], Response::HTTP_NOT_FOUND);
        }

        $since = max(0, (int) $request->query->get('since', 0));
        $messages = $messageRepository->findByConversationSinceId($conversation, $since);

        $messagesData = [];
        foreach ($messages as $message) {
            $messagesData[] = [
                'id' => $message->getId(),
                'contenu' => $message->getContenu(),
                'typeMessage' => $message->getTypeMessage(),
                'pieceJointeUrl' => $this->normalizeAttachmentUrl($message->getPieceJointeUrl()),
                'senderId' => $message->getSenderUserId(),
                'senderName' => $message->getSenderDisplayName($conversation),
                'dateEnvoi' => $message->getDateEnvoi()?->format('c'),
                'dureeAudio' => $message->getDureeAudio(),
            ];
        }

        return new JsonResponse([
            'success' => true,
            'messages' => $messagesData,
        ]);
    }

    private function normalizeAttachmentUrl(?string $url): ?string
    {
        if (null === $url || '' === $url) {
            return null;
        }

        if (str_contains($url, 'private.blob.vercel-storage.com')) {
            return '/blob-proxy?url='.urlencode($url);
        }

        return $url;
    }
}
