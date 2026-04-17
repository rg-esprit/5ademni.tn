<?php

namespace App\Service;

use App\Entity\User;
use Symfony\Component\HttpFoundation\RequestStack;
use Symfony\Component\Mailer\MailerInterface;
use Symfony\Component\Mime\Address;
use Symfony\Component\Mime\Email;

/**
 * Handles one-time email verification codes for the payment flow.
 *
 * How it works:
 * - Generates a 6-digit code, stores it in the session (NO database)
 * - Sends the code to the user's email via Symfony Mailer
 * - Once verified, the user stays verified for 24 hours (session-based)
 *
 * Zero database changes. Zero impact on other modules.
 */
class PaymentOtpService
{
    private const OTP_SESSION_KEY = 'payment_otp_code';
    private const OTP_EXPIRY_KEY = 'payment_otp_expires';
    private const VERIFIED_UNTIL_KEY = 'payment_email_verified_until';
    private const OTP_LIFETIME_SECONDS = 600; // 10 minutes
    private const VERIFICATION_DURATION_SECONDS = 86400; // 24 hours

    public function __construct(
        private readonly MailerInterface $mailer,
        private readonly RequestStack $requestStack,
    ) {
    }

    /**
     * Check if the user has already verified their email in this session.
     */
    public function isVerified(): bool
    {
        $session = $this->requestStack->getSession();
        $verifiedUntil = $session->get(self::VERIFIED_UNTIL_KEY);

        if (null === $verifiedUntil) {
            return false;
        }

        return time() < (int) $verifiedUntil;
    }

    /**
     * Generate a 6-digit OTP code and send it to the user's email.
     */
    public function sendOtp(User $user): void
    {
        $code = str_pad((string) random_int(0, 999999), 6, '0', STR_PAD_LEFT);
        $session = $this->requestStack->getSession();

        $session->set(self::OTP_SESSION_KEY, $code);
        $session->set(self::OTP_EXPIRY_KEY, time() + self::OTP_LIFETIME_SECONDS);

        $email = (new Email())
            ->from(new Address('hello@demomailtrap.co', 'Khademni Sécurité'))
            ->to($user->getEmail())
            ->subject('🔐 Code de vérification — Paiement Khademni')
            ->html($this->buildEmailHtml($user, $code));

        $this->mailer->send($email);
    }

    /**
     * Verify the user-submitted code against the session-stored OTP.
     *
     * @return array{valid: bool, error: string|null}
     */
    public function verifyOtp(string $submittedCode): array
    {
        $session = $this->requestStack->getSession();
        $storedCode = $session->get(self::OTP_SESSION_KEY);
        $expiresAt = $session->get(self::OTP_EXPIRY_KEY, 0);

        if (null === $storedCode) {
            return ['valid' => false, 'error' => 'Aucun code n\'a été envoyé. Veuillez recommencer.'];
        }

        if (time() > (int) $expiresAt) {
            $session->remove(self::OTP_SESSION_KEY);
            $session->remove(self::OTP_EXPIRY_KEY);

            return ['valid' => false, 'error' => 'Le code a expiré. Veuillez en demander un nouveau.'];
        }

        if ($submittedCode !== $storedCode) {
            return ['valid' => false, 'error' => 'Code incorrect. Veuillez réessayer.'];
        }

        // Verified! Mark session as verified for 24 hours
        $session->set(self::VERIFIED_UNTIL_KEY, time() + self::VERIFICATION_DURATION_SECONDS);
        $session->remove(self::OTP_SESSION_KEY);
        $session->remove(self::OTP_EXPIRY_KEY);

        return ['valid' => true, 'error' => null];
    }

    /**
     * Get the number of seconds remaining before the OTP expires.
     */
    public function getOtpSecondsRemaining(): int
    {
        $expiresAt = $this->requestStack->getSession()->get(self::OTP_EXPIRY_KEY, 0);
        $remaining = (int) $expiresAt - time();

        return max(0, $remaining);
    }

    private function buildEmailHtml(User $user, string $code): string
    {
        $name = $user->getFirstName() ?: 'Utilisateur';

        return <<<HTML
        <div style="font-family: 'Inter', 'Segoe UI', Arial, sans-serif; max-width: 480px; margin: 0 auto; padding: 40px 24px; background: #fafafa;">
            <div style="background: white; border-radius: 20px; padding: 40px 32px; box-shadow: 0 4px 24px rgba(108,92,231,0.08); border: 1px solid #f0eef5;">
                
                <div style="text-align: center; margin-bottom: 24px;">
                    <span style="font-size: 14px; font-weight: 900; letter-spacing: 4px; color: #6c5ce7; text-transform: uppercase;">5ADEMNI.TN</span>
                </div>

                <h2 style="font-size: 22px; font-weight: 800; color: #1a1a1a; margin: 0 0 8px; text-align: center;">
                    Vérification du paiement
                </h2>
                <p style="font-size: 14px; color: #6b7280; text-align: center; margin: 0 0 32px;">
                    Bonjour {$name}, voici votre code de sécurité
                </p>

                <div style="background: linear-gradient(135deg, #6c5ce7 0%, #a855f7 100%); border-radius: 16px; padding: 28px; text-align: center; margin-bottom: 24px;">
                    <span style="font-size: 36px; font-weight: 900; color: white; letter-spacing: 12px; font-family: 'Courier New', monospace;">
                        {$code}
                    </span>
                </div>

                <p style="font-size: 13px; color: #9ca3af; text-align: center; margin: 0;">
                    Ce code expire dans <strong>10 minutes</strong>.<br>
                    Si vous n'avez pas demandé ce code, ignorez cet email.
                </p>
            </div>
        </div>
        HTML;
    }
}
