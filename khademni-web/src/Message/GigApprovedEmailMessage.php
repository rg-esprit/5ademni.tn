<?php

namespace App\Message;

class GigApprovedEmailMessage
{
    public const TYPE_APPROVED = 'approved';
    public const TYPE_REJECTED = 'rejected';

    public function __construct(
        private readonly string $type,
        private readonly string $toEmail,
        private readonly string $displayName,
        private readonly string $gigTitle,
        private readonly ?string $reason = null,
    ) {
    }

    public function getType(): string
    {
        return $this->type;
    }

    public function getToEmail(): string
    {
        return $this->toEmail;
    }

    public function getDisplayName(): string
    {
        return $this->displayName;
    }

    public function getGigTitle(): string
    {
        return $this->gigTitle;
    }

    public function getReason(): ?string
    {
        return $this->reason;
    }

    public function isApproval(): bool
    {
        return self::TYPE_APPROVED === $this->type;
    }
}
