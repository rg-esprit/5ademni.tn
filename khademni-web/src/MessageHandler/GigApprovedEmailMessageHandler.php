<?php

namespace App\MessageHandler;

use App\Message\GigApprovedEmailMessage;
use Symfony\Component\DependencyInjection\Attribute\Autowire;
use Symfony\Component\Mailer\MailerInterface;
use Symfony\Component\Messenger\Attribute\AsMessageHandler;
use Symfony\Component\Mime\Email;

#[AsMessageHandler]
class GigApprovedEmailMessageHandler
{
    public function __construct(
        private readonly MailerInterface $mailer,
        #[Autowire('%env(MAILER_FROM_ADDRESS)%')]
        private readonly string $fromAddress,
    ) {
    }

    public function __invoke(GigApprovedEmailMessage $message): void
    {
        if ($message->isApproval()) {
            $subject = sprintf('Gig approved: %s', $message->getGigTitle());
            $body = sprintf(
                "Hello %s,\n\nYour gig \"%s\" has been approved by the admin team and is now visible according to workflow rules.\n\nRegards,\n5ademni.tn",
                $message->getDisplayName(),
                $message->getGigTitle(),
            );
        } else {
            $reason = trim((string) $message->getReason());
            $subject = sprintf('Gig rejected: %s', $message->getGigTitle());
            $body = sprintf(
                "Hello %s,\n\nYour gig \"%s\" was rejected by the admin team.\nReason: %s\n\nYou can update it and submit again.\n\nRegards,\n5ademni.tn",
                $message->getDisplayName(),
                $message->getGigTitle(),
                '' === $reason ? 'No reason provided.' : $reason,
            );
        }

        $email = (new Email())
            ->from($this->fromAddress)
            ->to($message->getToEmail())
            ->subject($subject)
            ->text($body);

        $this->mailer->send($email);
    }
}
