package com.khademni.utils;

import java.util.regex.Pattern;

/**
 * Utility class for centralizing validation logic and regex patterns.
 * Ensures consistent data sanitization across the application.
 */
public class ValidationUtils {

    // RFC 5322 compliant email regex
    private static final String EMAIL_REGEX = "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^-]+(?:\\.[a-zA-Z0-9_!#$%&'*+/=?`{|}~^-]+)*@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*$";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);

    /** Minimum 8 characters, at least one letter and one number. */
    private static final String PASSWORD_REGEX = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$";
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(PASSWORD_REGEX);

    public static boolean isValidEmail(String email) {
        if (email == null)
            return false;
        return EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isStrongPassword(String password) {
        if (password == null)
            return false;
        return PASSWORD_PATTERN.matcher(password).matches();
    }

    /**
     * Auto-corrects a text value:
     * <ul>
     * <li>Trims leading/trailing whitespace</li>
     * <li>Collapses multiple consecutive spaces into one</li>
     * <li>Capitalizes the first letter</li>
     * </ul>
     *
     * @param input raw text from a field
     * @return corrected text, or empty string if input is null/blank
     */
    public static String autoCorrect(String input) {
        if (input == null)
            return "";
        String corrected = input.trim().replaceAll("\\s{2,}", " ");
        if (corrected.isEmpty())
            return "";
        return Character.toUpperCase(corrected.charAt(0)) + corrected.substring(1);
    }

    /**
     * Validates that a field is not empty and meets the minimum length.
     *
     * @param value     the already auto-corrected text
     * @param fieldName human-readable name used in the error message
     * @param minLength minimum number of characters required
     * @return null if valid, error message string if invalid
     */
    public static String validateField(String value, String fieldName, int minLength) {
        if (value == null || value.isBlank()) {
            return fieldName + " ne peut pas être vide.";
        }
        if (value.length() < minLength) {
            return fieldName + " doit contenir au moins " + minLength + " caractère(s).";
        }
        return null; // valid
    }

    /**
     * Legacy alias — delegates to {@link #autoCorrect(String)}.
     */
    public static String sanitize(String input) {
        return autoCorrect(input);
    }
}
