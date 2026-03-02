package com.khademni.service;

import com.khademni.model.GigModel;
import com.khademni.model.SpamResult;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service responsible for analyzing gig submissions and detecting spam content.
 * Uses a weighted scoring algorithm where each rule contributes a specific number
 * of penalty points. A total score >= 40 marks the gig as spam.
 */
public class SpamDetectionService {

    /** Score threshold above which a gig is classified as spam */
    private static final int SPAM_THRESHOLD = 40;

    /** Minimum acceptable title length — shorter titles are often low-effort spam */
    private static final int MIN_TITLE_LENGTH = 10;

    /** Maximum allowed ratio of uppercase letters before flagging (60%) */
    private static final double MAX_UPPERCASE_RATIO = 0.60;

    /** Maximum allowed ratio of special characters before flagging (30%) */
    private static final double MAX_SPECIAL_CHAR_RATIO = 0.30;

    /** Maximum allowed repetitions of a single word before flagging */
    private static final int MAX_WORD_REPETITIONS = 3;

    /** Keywords commonly associated with fraudulent or spammy gig listings */
    private static final List<String> FORBIDDEN_PHRASES = List.of(
            "free money", "bitcoin", "telegram", "whatsapp", "100% guaranteed"
    );

    /**
     * Analyzes a gig submission and returns a SpamResult with the total score,
     * spam verdict, and a list of reasons for any penalties applied.
     *
     * @param gig the gig to analyze
     * @return SpamResult containing score, verdict, and reasons
     */
    public SpamResult analyze(GigModel gig) {
        SpamResult result = new SpamResult();

        String title = gig.getTitle() != null ? gig.getTitle() : "";
        String description = gig.getDescription() != null ? gig.getDescription() : "";

        // Apply each detection rule independently
        checkTitleLength(title, result);
        checkUppercaseRatio(title, description, result);
        checkWordRepetition(title, description, result);
        checkForbiddenPhrases(title, description, result);
        checkSpecialCharacters(title, description, result);
        checkPrice(gig.getPrice(), result);

        // Final verdict: spam if cumulative score meets or exceeds threshold
        result.setSpam(result.getSpamScore() >= SPAM_THRESHOLD);

        return result;
    }

    /**
     * Titles shorter than 10 characters are often low-effort or automated spam.
     * Penalty: +20 points
     */
    private void checkTitleLength(String title, SpamResult result) {
        if (title.trim().length() < MIN_TITLE_LENGTH) {
            result.addPenalty(20, "Title is too short (less than " + MIN_TITLE_LENGTH + " characters)");
        }
    }

    /**
     * Excessive uppercase usage is a common spam indicator (e.g., "FREE MONEY NOW").
     * Only counts actual letters to avoid false positives from numbers/symbols.
     * Penalty: +15 points if >60% of all letters are uppercase.
     */
    private void checkUppercaseRatio(String title, String description, SpamResult result) {
        String combined = title + " " + description;
        long letterCount = combined.chars().filter(Character::isLetter).count();
        if (letterCount == 0) return;

        long uppercaseCount = combined.chars().filter(Character::isUpperCase).count();
        double ratio = (double) uppercaseCount / letterCount;

        if (ratio > MAX_UPPERCASE_RATIO) {
            result.addPenalty(15, "Excessive uppercase letters (" + Math.round(ratio * 100) + "% uppercase)");
        }
    }

    /**
     * Repetitive words often indicate keyword-stuffing spam (e.g., "cheap cheap cheap cheap").
     * Penalty: +20 points if any single word appears more than 3 times across title + description.
     */
    private void checkWordRepetition(String title, String description, SpamResult result) {
        String combined = (title + " " + description).toLowerCase().trim();
        if (combined.isEmpty()) return;

        // Split on non-word characters, filter out empty tokens
        String[] words = combined.split("\\W+");
        Map<String, Long> frequency = Arrays.stream(words)
                .filter(w -> !w.isEmpty())
                .collect(Collectors.groupingBy(w -> w, Collectors.counting()));

        for (Map.Entry<String, Long> entry : frequency.entrySet()) {
            if (entry.getValue() > MAX_WORD_REPETITIONS) {
                result.addPenalty(20, "Word \"" + entry.getKey() + "\" repeated " + entry.getValue() + " times");
                // Only penalize once for this rule, even if multiple words are repeated
                return;
            }
        }
    }

    /**
     * Checks for known scam-related phrases in both title and description.
     * Matching is case-insensitive to catch variations like "FREE MONEY" or "Free Money".
     * Penalty: +30 points if any forbidden phrase is found.
     */
    private void checkForbiddenPhrases(String title, String description, SpamResult result) {
        String combinedLower = (title + " " + description).toLowerCase();

        for (String phrase : FORBIDDEN_PHRASES) {
            if (combinedLower.contains(phrase)) {
                result.addPenalty(30, "Contains forbidden phrase: \"" + phrase + "\"");
                // Only penalize once for this rule to avoid excessive stacking
                return;
            }
        }
    }

    /**
     * Excessive special characters often indicate obfuscation or gibberish content
     * (e.g., "$$$!!!@@@###"). Only non-letter, non-digit, non-space characters count.
     * Penalty: +15 points if >30% of characters are special characters.
     */
    private void checkSpecialCharacters(String title, String description, SpamResult result) {
        String combined = title + " " + description;
        if (combined.trim().isEmpty()) return;

        long totalChars = combined.chars().filter(c -> !Character.isWhitespace(c)).count();
        if (totalChars == 0) return;

        long specialCount = combined.chars()
                .filter(c -> !Character.isLetterOrDigit(c) && !Character.isWhitespace(c))
                .count();
        double ratio = (double) specialCount / totalChars;

        if (ratio > MAX_SPECIAL_CHAR_RATIO) {
            result.addPenalty(15, "Too many special characters (" + Math.round(ratio * 100) + "% of content)");
        }
    }

    /**
     * A price of zero or negative likely indicates a fraudulent or placeholder listing.
     * Penalty: +10 points
     */
    private void checkPrice(double price, SpamResult result) {
        if (price <= 0) {
            result.addPenalty(10, "Invalid price (must be greater than 0)");
        }
    }
}
