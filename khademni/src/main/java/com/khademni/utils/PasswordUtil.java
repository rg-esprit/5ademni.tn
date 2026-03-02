package com.khademni.utils;

import at.favre.lib.crypto.bcrypt.BCrypt;

public class PasswordUtil {

    /**
     * Hashes a plain text password using BCrypt.
     * 
     * @param plainTextPassword The password to hash.
     * @return The hashed password.
     */
    public static String hashPassword(String plainTextPassword) {
        return BCrypt.withDefaults().hashToString(12, plainTextPassword.toCharArray());
    }

    /**
     * Verifies a plain text password against a hashed password.
     * 
     * @param plainTextPassword The plain text password to check.
     * @param hashedPassword    The hashed password to compare against.
     * @return true if the password matches, false otherwise.
     */
    public static boolean checkPassword(String plainTextPassword, String hashedPassword) {
        try {
            BCrypt.Result result = BCrypt.verifyer().verify(plainTextPassword.toCharArray(), hashedPassword);
            return result.verified;
        } catch (Exception e) {
            return false;
        }
    }
}
