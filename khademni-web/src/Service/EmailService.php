<?php

namespace App\Service;

use App\Entity\Article;
use Symfony\Component\Mailer\MailerInterface;
use Symfony\Component\Mime\Address;
use Symfony\Component\Mime\Email;
use Twig\Environment;

class EmailService
{
    private MailerInterface $mailer;
    private Environment $twig;
    private string $fromEmail;

    public function __construct(MailerInterface $mailer, Environment $twig, string $fromEmail)
    {
        $this->mailer = $mailer;
        $this->twig = $twig;
        $this->fromEmail = $fromEmail;
    }

    public function sendEmail(string $to, string $subject, string $message, ?Article $article = null): void
    {
        $email = (new Email())
            ->from(new Address($this->fromEmail, '5ademni'))
            ->to($to)
            ->subject($subject)
            ->html($this->twig->render('article/emails/article_notification.html.twig', [
                'subject' => $subject,
                'message' => $message,
                'article' => $article,
            ]));

        $this->mailer->send($email);
    }
}
