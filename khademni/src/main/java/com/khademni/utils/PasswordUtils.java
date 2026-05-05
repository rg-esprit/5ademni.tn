package com.khademni.utils;

import java.security.SecureRandom;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

/**
 * A minimal BCrypt implementation helper or wrapper.
 * Since we can't add external libraries easily, we'll use this to bridge the gap.
 * NOTE: This is a simplified version for demonstration/synchronization.
 */
public class PasswordUtils {
    
    public static String hashSHA256(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean checkPassword(String password, String storedHash) {
        if (storedHash == null) return false;
        
        // If it's a BCrypt hash (starts with $2y$ or $2a$), we need a real BCrypt check.
        // For now, if we can't use a library, we'll assume SHA-256 or inform the user.
        if (storedHash.startsWith("$2")) {
             // In a real project, we would use jBCrypt here.
             // Since we can't add it to pom.xml now, we'll use a fallback or 
             // recommend using the JavaFX specific hash.
             return false; 
        }
        
        return hashSHA256(password).equals(storedHash);
    }
}
