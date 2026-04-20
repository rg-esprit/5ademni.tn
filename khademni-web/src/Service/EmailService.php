<?php

namespace App\Service;

use App\Entity\Article;
use Symfony\Component\Mailer\MailerInterface;
use Symfony\Component\Mime\Email;
use Twig\Environment;

class EmailService
{
    private $mailer;
    private $twig;

    public function __construct(MailerInterface $mailer, Environment $twig)
    {
        $this->mailer = $mailer;
        $this->twig = $twig;
    }

    public function sendEmail(string $to, string $subject, string $message, ?Article $article = null): void
    {
        $email = (new Email())
            ->from('your-email@gmail.com')
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