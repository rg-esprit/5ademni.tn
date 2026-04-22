<?php

namespace App\Service;

use App\Entity\Category;
use App\Entity\User;
use App\Message\CategoryRequestEmailMessage;
use Symfony\Component\Messenger\MessageBusInterface;

class CategoryRequestNotificationService
{
    public function __construct(
        private readonly MessageBusInterface $messageBus,
    ) {
    }

    public function sendApproval(User $user, Category $category): void
    {
        $this->messageBus->dispatch(new CategoryRequestEmailMessage(
            CategoryRequestEmailMessage::TYPE_APPROVED,
            $user->getEmail(),
            $user->getDisplayName(),
            $category->getName(),
        ));
    }

    public function sendRejection(User $user, string $categoryName, string $reason): void
    {
        $this->messageBus->dispatch(new CategoryRequestEmailMessage(
            CategoryRequestEmailMessage::TYPE_REJECTED,
            $user->getEmail(),
            $user->getDisplayName(),
            $categoryName,
            trim($reason),
        ));
    }
}
