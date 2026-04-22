<?php

namespace App\Service;

use App\Entity\Gig;
use App\Entity\User;
use App\Message\GigApprovedEmailMessage;
use Symfony\Component\Messenger\MessageBusInterface;

class GigApprovalNotificationService
{
    public function __construct(
        private readonly MessageBusInterface $messageBus,
    ) {
    }

    public function sendApproval(User $user, Gig $gig): void
    {
        $this->messageBus->dispatch(new GigApprovedEmailMessage(
            GigApprovedEmailMessage::TYPE_APPROVED,
            $user->getEmail(),
            $user->getDisplayName(),
            $gig->getTitle(),
        ));
    }

    public function sendRejection(User $user, Gig $gig, ?string $reason = null): void
    {
        $this->messageBus->dispatch(new GigApprovedEmailMessage(
            GigApprovedEmailMessage::TYPE_REJECTED,
            $user->getEmail(),
            $user->getDisplayName(),
            $gig->getTitle(),
            null === $reason ? null : trim($reason),
        ));
    }
}
