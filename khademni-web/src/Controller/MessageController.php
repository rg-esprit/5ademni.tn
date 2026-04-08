<?php

namespace App\Controller;

use App\Entity\Conversation;
use App\Entity\Message;
use App\Entity\User;
use App\Repository\ConversationRepository;
use App\Repository\MessageRepository;
use App\Service\BlobStorageService;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\File\UploadedFile;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

class MessageController extends AbstractController
{
    #[Route('/messages/{id}', name: 'app_message_thread', methods: ['GET', 'POST'])]
    public function thread(int $id, Request $request, ConversationRepository $conversationRepository, MessageRepository $messageRepository, EntityManagerInterface $entityManager, ?BlobStorageService $blobStorageService = null): Response
    {
        $this->denyAccessUnlessGranted('ROLE_USER');

        /** @var User $user */
        $user = $this->getUser();
        $conversation = $conversationRepository->findOneForUser($id, $user);

        if (!$conversation instanceof Conversation) {
            $this->addFlash('error', 'Conversation introuvable ou accès refusé.');

            return $this->redirectToRoute('app_conversations');
        }

        if ($request->isMethod('POST')) {
            if (!$this->isCsrfTokenValid('send_message_'.$conversation->getId(), (string) $request->request->get('_token'))) {
                $this->addFlash('error', 'Jeton CSRF invalide. Le message n’a pas été envoyé.');
            } else {
                $action = (string) $request->request->get('action', 'send_message');
                $content = '';
                $type = 'TEXTE';
                $attachmentUrl = null;
                $audioDuration = null;
                $hasError = false;

                if ('schedule_meeting' === $action) {
                    $subject = trim((string) $request->request->get('meeting_subject', ''));
                    $datetime = trim((string) $request->request->get('meeting_datetime', ''));
                    $details = trim((string) $request->request->get('meeting_details', ''));

                    if ('' === $subject || '' === $datetime) {
                        $this->addFlash('error', 'Veuillez renseigner le sujet et la date de la réunion.');
                        $hasError = true;
                    } else {
                        $type = 'MEETING';
                        $content = sprintf("Réunion planifiée : %s\nQuand : %s\n%s", $subject, $datetime, $details ?: '');
                    }
                } else {
                    $content = trim((string) $request->request->get('content', ''));
                    $attachment = $request->files->get('attachment');

                    if ($attachment instanceof UploadedFile && UPLOAD_ERR_OK === $attachment->getError()) {
                        try {
                            if (null !== $blobStorageService && $blobStorageService->isConfigured()) {
                                $attachmentUrl = $blobStorageService->upload($attachment);
                            } else {
                                $attachmentUrl = $this->saveAttachmentLocally($attachment);
                            }

                            $mimeType = $attachment->getMimeType() ?? '';
                            if (str_starts_with($mimeType, 'image/')) {
                                $type = 'IMAGE';
                            } elseif (str_starts_with($mimeType, 'audio/')) {
                                $type = 'AUDIO';
                                $audioDuration = trim((string) $request->request->get('audio_duration', '')) ?: null;
                            } else {
                                $type = 'FILE';
                            }

                            if ('' === $content) {
                                $content = $attachment->getClientOriginalName();
                            }
                        } catch (\Throwable) {
                            $this->addFlash('error', 'Erreur lors de l’upload de la pièce jointe. Le fichier n’a pas été envoyé.');
                            $hasError = true;
                        }
                    }
                }

                if ('' === $content && null === $attachmentUrl) {
                    $this->addFlash('error', 'Le message ne peut pas être vide.');
                    $hasError = true;
                }

                if (!$hasError) {
                    $message = new Message();
                    $message->setConversation($conversation);
                    $message->markSentBy($user);
                    $message->setContenu($content);
                    $message->setTypeMessage($type);
                    $message->setDateEnvoi(new \DateTimeImmutable());
                    $message->setPieceJointeUrl($attachmentUrl);

                    if (null !== $audioDuration) {
                        $message->setDureeAudio($audioDuration);
                    }

                    if (null !== $conversation->getClient() && $conversation->getClient()->getId() === $user->getId()) {
                        $conversation->setNonLusFreelance($conversation->getNonLusFreelance() + 1);
                    } else {
                        $conversation->setNonLusClient($conversation->getNonLusClient() + 1);
                    }

                    $entityManager->persist($message);
                    $entityManager->flush();

                    $this->addFlash('success', 'Message envoyé.');

                    return $this->redirectToRoute('app_message_thread', ['id' => $conversation->getId()]);
                }
            }
        }

        $messages = $messageRepository->findByConversation($conversation);
        $this->markConversationAsRead($conversation, $user, $entityManager);

        return $this->render('modules/message_thread.html.twig', [
            'conversation' => $conversation,
            'messages' => $messages,
            'currentUser' => $user,
        ]);
    }

    #[Route('/api/messages/{id}/edit', name: 'api_message_edit', methods: ['POST'])]
    public function editMessage(int $id, Request $request, EntityManagerInterface $entityManager): JsonResponse
    {
        $this->denyAccessUnlessGranted('ROLE_USER');

        /** @var User $user */
        $user = $this->getUser();
        $message = $entityManager->getRepository(Message::class)->find($id);

        if (!$message instanceof Message) {
            return new JsonResponse(['success' => false, 'detail' => 'Message not found'], Response::HTTP_NOT_FOUND);
        }

        if (!$message->isSentBy($user)) {
            return new JsonResponse(['success' => false, 'detail' => 'Unauthorized'], Response::HTTP_FORBIDDEN);
        }

        $content = trim((string) $request->request->get('content', ''));
        if ('' === $content) {
            return new JsonResponse(['success' => false, 'detail' => 'Message content cannot be empty'], Response::HTTP_BAD_REQUEST);
        }

        $message->setContenu($content);
        $entityManager->flush();

        return new JsonResponse([
            'success' => true,
            'message' => [
                'id' => $message->getId(),
                'contenu' => $message->getContenu(),
            ],
        ]);
    }

    #[Route('/api/messages/{id}/delete', name: 'api_message_delete', methods: ['POST'])]
    public function deleteMessage(int $id, EntityManagerInterface $entityManager): JsonResponse
    {
        $this->denyAccessUnlessGranted('ROLE_USER');

        /** @var User $user */
        $user = $this->getUser();
        $message = $entityManager->getRepository(Message::class)->find($id);

        if (!$message instanceof Message) {
            return new JsonResponse(['success' => false, 'detail' => 'Message not found'], Response::HTTP_NOT_FOUND);
        }

        if (!$message->isSentBy($user)) {
            return new JsonResponse(['success' => false, 'detail' => 'Unauthorized'], Response::HTTP_FORBIDDEN);
        }

        $entityManager->remove($message);
        $entityManager->flush();

        return new JsonResponse(['success' => true]);
    }

    private function saveAttachmentLocally(UploadedFile $attachment): string
    {
        $uploadDir = $this->getParameter('kernel.project_dir').'/public/uploads/messages';
        if (!is_dir($uploadDir)) {
            @mkdir($uploadDir, 0755, true);
        }

        $extension = $attachment->guessExtension() ?? 'bin';
        $filename = bin2hex(random_bytes(16)).'.'.$extension;
        $attachment->move($uploadDir, $filename);

        return '/uploads/messages/'.$filename;
    }

    private function markConversationAsRead(Conversation $conversation, User $user, EntityManagerInterface $entityManager): void
    {
        if (null !== $conversation->getClient() && $conversation->getClient()->getId() === $user->getId() && $conversation->getNonLusClient() > 0) {
            $conversation->setNonLusClient(0);
        }

        if (null !== $conversation->getFreelance() && $conversation->getFreelance()->getId() === $user->getId() && $conversation->getNonLusFreelance() > 0) {
            $conversation->setNonLusFreelance(0);
        }

        $entityManager->flush();
    }
}
