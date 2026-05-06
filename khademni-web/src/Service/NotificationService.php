<?php

namespace App\Service;

use App\Entity\Notification;
use App\Entity\User;
use Doctrine\ORM\EntityManagerInterface;

class NotificationService
{
    public function __construct(private EntityManagerInterface $em) {}

    public function send(User $recipient, string $message, string $type = 'info', ?string $link = null): void
    {
        $notification = new Notification();
        $notification->setRecipient($recipient);
        $notification->setMessage($message);
        $notification->setType($type);
        $notification->setLink($link);

        $this->em->persist($notification);
        $this->em->flush();
    }
}
