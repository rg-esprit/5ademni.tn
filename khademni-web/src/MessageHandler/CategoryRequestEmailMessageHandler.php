<?php

namespace App\MessageHandler;

use App\Message\CategoryRequestEmailMessage;
use Symfony\Component\DependencyInjection\Attribute\Autowire;
use Symfony\Component\Mailer\MailerInterface;
use Symfony\Component\Messenger\Attribute\AsMessageHandler;
use Symfony\Component\Mime\Email;

#[AsMessageHandler]
class CategoryRequestEmailMessageHandler
{
    public function __construct(
        private readonly MailerInterface $mailer,
        #[Autowire('%env(MAILER_FROM_ADDRESS)%')]
        private readonly string $fromAddress,
    ) {
    }

    public function __invoke(CategoryRequestEmailMessage $message): void
    {
        if ($message->isApproval()) {
            $subject = sprintf('Category approved: %s', $message->getCategoryName());
            $body = sprintf(
                "Hello %s,\n\nGood news. Your category request \"%s\" has been approved by the admin team.\nYou can now use it for gig creation.\n\nRegards,\n5ademni.tn",
                $message->getDisplayName(),
                $message->getCategoryName(),
            );
        } else {
            $reason = trim((string) $message->getReason());
            $subject = sprintf('Category rejected: %s', $message->getCategoryName());
            $body = sprintf(
                "Hello %s,\n\nYour category request \"%s\" was rejected by the admin team.\nReason: %s\n\nYou can submit a new request with more details anytime.\n\nRegards,\n5ademni.tn",
                $message->getDisplayName(),
                $message->getCategoryName(),
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
