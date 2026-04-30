<?php

namespace App\Service;

use App\Entity\User;
use Symfony\Component\Mailer\MailerInterface;
use Symfony\Component\Mime\Address;
use Symfony\Component\Mime\Email;

class MeetingNotificationService
{
    public function __construct(
        private readonly MailerInterface $mailer,
        private readonly string $fromEmail
    ) {
    }

    public function sendMeetingInvitation(User $organizer, User $recipient, string $subject, string $details, \DateTimeImmutable $start, \DateTimeImmutable $end): void
    {
        $recipientEmail = trim($recipient->getEmail() ?? '');
        if ('' === $recipientEmail) {
            return;
        }

        $organizerName = trim($organizer->getDisplayName() ?: '5ademni');
        $recipientName = trim($recipient->getDisplayName() ?: 'collaborateur');
        $bodyText = sprintf(
            "Bonjour %s,\n\nUne réunion a été planifiée avec %s sur 5ademni.tn.\n\nSujet : %s\nDate : %s\nFin : %s\n\nDétails : %s\n\nVous pouvez consulter cette invitation dans votre calendrier.",
            $recipientName,
            $organizerName,
            $subject,
            $start->format('d/m/Y H:i'),
            $end->format('d/m/Y H:i'),
            $details ?: 'Aucun détail supplémentaire'
        );

        $calendarUrl = $this->buildGoogleCalendarTemplateUrl($subject, $start, $end, $details);
        $buttonHtml = sprintf(
            '<p style="text-align:center; margin: 24px 0;"><a href="%s" target="_blank" rel="noopener noreferrer" style="background-color:#5c5cff;color:#ffffff;text-decoration:none;padding:12px 20px;border-radius:8px;display:inline-block;">Voir dans mon calendrier</a></p>',
            htmlspecialchars($calendarUrl, ENT_QUOTES, 'UTF-8')
        );

        $bodyHtml = sprintf(
            '<p>Bonjour %s,</p><p>Une réunion a été planifiée avec <strong>%s</strong> sur <strong>5ademni.tn</strong>.</p><ul><li><strong>Sujet :</strong> %s</li><li><strong>Date :</strong> %s</li><li><strong>Fin :</strong> %s</li></ul><p><strong>Détails :</strong><br/>%s</p>%s<p>Vous pouvez consulter cette invitation dans votre calendrier.</p>',
            htmlspecialchars($recipientName, ENT_QUOTES, 'UTF-8'),
            htmlspecialchars($organizerName, ENT_QUOTES, 'UTF-8'),
            htmlspecialchars($subject, ENT_QUOTES, 'UTF-8'),
            $start->format('d/m/Y H:i'),
            $end->format('d/m/Y H:i'),
            nl2br(htmlspecialchars($details ?: 'Aucun détail supplémentaire', ENT_QUOTES, 'UTF-8')),
            $buttonHtml
        );

        $email = (new Email())
            ->from(new Address($this->fromEmail, '5ademni'))
            ->to($recipientEmail)
            ->subject(sprintf('Réunion planifiée : %s', $subject))
            ->text($bodyText)
            ->html($bodyHtml)
            ->attach($this->buildIcs($organizerName, $recipientEmail, $subject, $details, $start, $end), 'invitation.ics', 'text/calendar; method=REQUEST; charset=UTF-8');

        $this->mailer->send($email);
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

    private function buildIcs(string $organizerName, string $recipientEmail, string $subject, string $details, \DateTimeImmutable $start, \DateTimeImmutable $end): string
    {
        $uid = bin2hex(random_bytes(16)).'@5ademni.tn';
        $dtStamp = (new \DateTimeImmutable('now', new \DateTimeZone('UTC')))->format('Ymd\THis\Z');
        $dtStart = $this->formatIcsDate($start);
        $dtEnd = $this->formatIcsDate($end);
        $description = $this->escapeIcsText($details ?: 'Aucun détail supplémentaire');
        $summary = $this->escapeIcsText($subject);
        $organizerEmail = $this->escapeIcsText($this->fromEmail);

        $lines = [
            'BEGIN:VCALENDAR',
            'PRODID:-//5ademni.tn//Meeting Invitation//FR',
            'VERSION:2.0',
            'METHOD:REQUEST',
            'BEGIN:VEVENT',
            'UID:'.$uid,
            'DTSTAMP:'.$dtStamp,
            'DTSTART:'.$dtStart,
            'DTEND:'.$dtEnd,
            'SUMMARY:'.$summary,
            'DESCRIPTION:'.$description,
            'ORGANIZER;CN='.$this->escapeIcsText($organizerName).':mailto:'.$organizerEmail,
            'ATTENDEE;CN='.$this->escapeIcsText($recipientEmail).';ROLE=REQ-PARTICIPANT;RSVP=TRUE:mailto:'.$recipientEmail,
            'END:VEVENT',
            'END:VCALENDAR',
        ];

        return implode("\r\n", $lines)."\r\n";
    }

    private function formatIcsDate(\DateTimeImmutable $date): string
    {
        return $date->setTimezone(new \DateTimeZone('UTC'))->format('Ymd\THis\Z');
    }

    private function escapeIcsText(string $text): string
    {
        return str_replace(["\\", "\n", "\r", ';', ','], ['\\\\', '\\n', '', '\\;', '\\,'], $text);
    }
}
