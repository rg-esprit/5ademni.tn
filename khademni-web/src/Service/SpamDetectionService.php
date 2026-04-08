<?php

namespace App\Service;

class SpamDetectionService
{
    private const THRESHOLD = 40;
    private const FORBIDDEN_PHRASE_SCORE = 40;
    private const MIN_TITLE_LENGTH = 10;
    private const MAX_UPPERCASE_RATIO = 0.60;
    private const MAX_SPECIAL_CHAR_RATIO = 0.30;
    private const MAX_WORD_REPETITIONS = 3;

    /**
     * @var string[]
     */
    private const FORBIDDEN_PHRASES = [
        'free money',
        'bitcoin',
        'telegram',
        'whatsapp',
        '100% Guaranteed',
    ];

    /**
     * @return array{is_spam:bool, score:int, reasons:string[]}
     */
    public function analyze(string $title, string $description, float $price): array
    {
        $score = 0;
        $reasons = [];

        if (mb_strlen(trim($title)) < self::MIN_TITLE_LENGTH) {
            $score += 20;
            $reasons[] = sprintf('Title is too short (less than %d characters).', self::MIN_TITLE_LENGTH);
        }

        $combined = trim($title.' '.$description);
        $letterCount = preg_match_all('/\p{L}/u', $combined, $m);
        if ($letterCount > 0) {
            $upperCount = preg_match_all('/\p{Lu}/u', $combined, $m2);
            $ratio = $upperCount / $letterCount;
            if ($ratio > self::MAX_UPPERCASE_RATIO) {
                $score += 15;
                $reasons[] = sprintf('Excessive uppercase usage (%d%%).', (int) round($ratio * 100));
            }
        }

        $tokens = preg_split('/\W+/u', mb_strtolower($combined), -1, PREG_SPLIT_NO_EMPTY) ?: [];
        if ([] !== $tokens) {
            $counts = array_count_values($tokens);
            foreach ($counts as $word => $count) {
                if ($count > self::MAX_WORD_REPETITIONS) {
                    $score += 20;
                    $reasons[] = sprintf('Word "%s" repeated %d times.', $word, $count);
                    break;
                }
            }
        }

        $lower = mb_strtolower($combined);
        foreach (self::FORBIDDEN_PHRASES as $phrase) {
            $phraseLower = mb_strtolower($phrase);
            if (str_contains($lower, $phraseLower)) {
                $score += self::FORBIDDEN_PHRASE_SCORE;
                $reasons[] = sprintf('Contains forbidden phrase: "%s".', $phrase);
                break;
            }
        }

        $compact = preg_replace('/\s+/u', '', $combined) ?? '';
        $totalChars = mb_strlen($compact);
        if ($totalChars > 0) {
            $specialCount = preg_match_all('/[^\p{L}\p{N}\s]/u', $combined, $matches);
            $ratio = $specialCount / $totalChars;
            if ($ratio > self::MAX_SPECIAL_CHAR_RATIO) {
                $score += 15;
                $reasons[] = sprintf('Too many special characters (%d%%).', (int) round($ratio * 100));
            }
        }

        if ($price <= 0) {
            $score += 10;
            $reasons[] = 'Price must be greater than 0.';
        }

        return [
            'is_spam' => $score >= self::THRESHOLD,
            'score' => $score,
            'reasons' => $reasons,
        ];
    }
}
