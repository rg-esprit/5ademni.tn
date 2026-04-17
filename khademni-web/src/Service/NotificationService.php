<?php

namespace App\Service;

use App\Entity\Notification;
use App\Entity\User;
use Doctrine\ORM\EntityManagerInterface;
use Psr\Log\LoggerInterface;

class NotificationService
{
    public function __construct(private EntityManagerInterface $em, private LoggerInterface $logger) {}

    public function send(User $recipient, string $message, string $type = 'info', ?string $link = null): void
    {
        $notification = new Notification();
        $notification->setRecipient($recipient);
        $notification->setMessage($message);
        $notification->setType($type);
        $notification->setLink($link);

        try {
            $this->em->persist($notification);
            $this->em->flush();
        } catch (\Throwable $exception) {
            $this->logger->error('Unable to save notification', [
                'recipient' => $recipient->getId(),
                'message' => $message,
                'error' => $exception->getMessage(),
            ]);
        }
    }
}
