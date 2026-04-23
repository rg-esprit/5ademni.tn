<?php

namespace App\Controller;

use App\Entity\Conversation;
use App\Entity\Message;
use App\Entity\User;
use App\Repository\ConversationRepository;
use App\Repository\MessageRepository;
use App\Service\BlobStorageService;
use App\Service\GoogleCalendarService;
use App\Service\MeetingNotificationService;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\File\UploadedFile;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

class MessageController extends AbstractController
{
    private const MESSAGE_COOLDOWN_SECONDS = 10;

    #[Route('/messages/{id}', name: 'app_message_thread', methods: ['GET', 'POST'])]
    public function thread(int $id, Request $request, ConversationRepository $conversationRepository, MessageRepository $messageRepository, EntityManagerInterface $entityManager, MeetingNotificationService $meetingNotificationService, GoogleCalendarService $googleCalendarService, ?BlobStorageService $blobStorageService = null): Response
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
                $session = $request->getSession();
                $cooldownKey = 'last_message_sent_at_' . $conversation->getId();
                $now = new \DateTimeImmutable();
                $lastSent = $session->get($cooldownKey);
                $hasError = false;

                if ($lastSent instanceof \DateTimeInterface) {
                    $secondsSinceLastSend = $now->getTimestamp() - $lastSent->getTimestamp();
                    if ($secondsSinceLastSend < self::MESSAGE_COOLDOWN_SECONDS) {
                        $this->addFlash('error', sprintf('Veuillez attendre %d secondes avant d’envoyer un nouveau message.', self::MESSAGE_COOLDOWN_SECONDS - $secondsSinceLastSend));
                        $hasError = true;
                    }
                }

                $action = (string) $request->request->get('action', 'send_message');
                $content = '';
                $type = 'TEXTE';
                $attachmentUrl = null;
                $audioDuration = null;
                $sendDate = new \DateTimeImmutable();

                if ('schedule_meeting' === $action) {
                    $subject = trim((string) $request->request->get('meeting_subject', ''));
                    $datetime = trim((string) $request->request->get('meeting_datetime', ''));
                    $details = trim((string) $request->request->get('meeting_details', ''));
                    $meetingStart = null;
                    $meetingEnd = null;

                    if ('' === $subject || '' === $datetime) {
                        $this->addFlash('error', 'Veuillez renseigner le sujet et la date de la réunion.');
                        $hasError = true;
                    } else {
                        try {
                            $meetingStart = new \DateTimeImmutable($datetime);
                            $meetingEnd = $meetingStart->modify('+1 hour');
                            $type = 'MEETING';
                            $content = sprintf("Réunion planifiée : %s\nQuand : %s\n%s", $subject, $meetingStart->format('d/m/Y H:i'), $details ?: '');
                            $sendDate = $meetingStart;
                        } catch (\Throwable) {
                            $this->addFlash('error', 'La date de réunion n’est pas valide.');
                            $hasError = true;
                        }
                    }
                } elseif ('schedule_message' === $action) {
                    $content = trim((string) $request->request->get('content', ''));
                    $datetime = trim((string) $request->request->get('scheduled_datetime', ''));

                    if ('' === $content || '' === $datetime) {
                        $this->addFlash('error', 'Veuillez renseigner le message et la date de programmation.');
                        $hasError = true;
                    } else {
                        try {
                            $sendDate = new \DateTimeImmutable($datetime);
                            $type = 'SCHEDULED';
                        } catch (\Throwable) {
                            $this->addFlash('error', 'La date de programmation n’est pas valide.');
                            $hasError = true;
                        }
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
                    $message->setDateEnvoi($sendDate);
                    $message->setPieceJointeUrl($attachmentUrl);

                    if ('SCHEDULED' === $type) {
                        $message->setContenu(sprintf("Message programmé pour %s : %s", $sendDate->format('d/m/Y H:i'), $content));
                    }

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
                    $session->set($cooldownKey, $now);

                    if ('MEETING' === $type && isset($meetingStart, $meetingEnd)) {
                        $otherParticipant = $conversation->getOtherParticipant($user);
                        try {
                            if ($otherParticipant instanceof User) {
                                $meetingNotificationService->sendMeetingInvitation($user, $otherParticipant, $subject, $details, $meetingStart, $meetingEnd);
                            }

                            if ($googleCalendarService->isConfigured()) {
                                $attendees = [];
                                if ($otherParticipant instanceof User && '' !== trim((string) $otherParticipant->getEmail())) {
                                    $attendees[] = $otherParticipant->getEmail();
                                }
                                if ('' !== trim((string) $user->getEmail())) {
                                    $attendees[] = $user->getEmail();
                                }

                                if ([] !== $attendees) {
                                    $googleCalendarService->createEvent($subject, $content, $meetingStart, $meetingEnd, $attendees);
                                }
                            }
                        } catch (\Throwable $exception) {
                            $this->addFlash('warning', 'La réunion a bien été planifiée, mais la notification ou l’ajout à Google Calendar a échoué.');
                        }
                    }

                    $this->addFlash('success', 'Message envoyé.');

                    return $this->redirectToRoute('app_message_thread', ['id' => $conversation->getId()]);
                }
            }
        }

        $messages = $messageRepository->findByConversation($conversation);
        $meetingCalendarLinks = [];

        foreach ($messages as $message) {
            if ('MEETING' === $message->getTypeMessage()) {
                $meetingUrl = $this->buildMeetingCalendarUrl($message);
                if (null !== $meetingUrl) {
                    $meetingCalendarLinks[$message->getId()] = $meetingUrl;
                }
            }
        }

        $this->markConversationAsRead($conversation, $user, $entityManager);

        return $this->render('modules/message_thread.html.twig', [
            'conversation' => $conversation,
            'messages' => $messages,
            'currentUser' => $user,
            'meetingCalendarLinks' => $meetingCalendarLinks,
        ]);
    }

    private function buildMeetingCalendarUrl(Message $message): ?string
    {
        $content = trim($message->getContenu());
        if ('' === $content) {
            return null;
        }

        $lines = preg_split('/\r\n|\r|\n/', $content);
        if (!is_array($lines) || count($lines) < 2) {
            return null;
        }

        $subjectLine = trim($lines[0] ?? '');
        $dateLine = trim($lines[1] ?? '');
        $details = trim(implode("\n", array_slice($lines, 2)));

        $subject = preg_replace('/^Réunion planifiée\s*:\s*/i', '', $subjectLine);
        $dateText = preg_replace('/^Quand\s*:\s*/i', '', $dateLine);

        if ('' === $subject || '' === $dateText) {
            return null;
        }

        $start = \DateTimeImmutable::createFromFormat('d/m/Y H:i', $dateText);
        if (!$start instanceof \DateTimeImmutable) {
            try {
                $start = new \DateTimeImmutable($dateText);
            } catch (\Throwable) {
                return null;
            }
        }

        $end = $start->modify('+1 hour');

        return $this->buildGoogleCalendarTemplateUrl($subject, $start, $end, $details);
    }

    private function buildGoogleCalendarTemplateUrl(string $subject, \DateTimeImmutable $start, \DateTimeImmutable $end, string $details): string
    {
        $startUtc = $start->setTimezone(new \DateTimeZone('UTC'))->format('Ymd\THis\Z');
        $endUtc = $end->setTimezone(new \DateTimeZone('UTC'))->format('Ymd\THis\Z');

        $params = [
            'action' => 'TEMPLATE',
            'text' => $subject,
            'dates' => sprintf('%s/%s', $startUtc, $endUtc),
            'details' => $details,
            'sf' => 'true',
            'output' => 'xml',
        ];

        return 'https://calendar.google.com/calendar/render?' . http_build_query($params);
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
