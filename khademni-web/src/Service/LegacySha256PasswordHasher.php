<?php

namespace App\Service;

use Symfony\Component\PasswordHasher\Exception\InvalidPasswordException;
use Symfony\Component\PasswordHasher\PasswordHasherInterface;

class LegacySha256PasswordHasher implements PasswordHasherInterface
{
    public function hash(string $plainPassword): string
    {
        $this->guardPasswordLength($plainPassword);

        return hash('sha256', $plainPassword);
    }

    public function verify(string $hashedPassword, string $plainPassword): bool
    {
        $this->guardPasswordLength($plainPassword);

        return hash_equals($hashedPassword, $this->hash($plainPassword));
    }

    public function needsRehash(string $hashedPassword): bool
    {
        return 64 !== strlen($hashedPassword);
    }

    private function guardPasswordLength(string $plainPassword): void
    {
        if (strlen($plainPassword) > PasswordHasherInterface::MAX_PASSWORD_LENGTH) {
            throw new InvalidPasswordException();
        }
    }
}
