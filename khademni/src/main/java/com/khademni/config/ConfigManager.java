package com.khademni.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages application configuration and secrets via environment variables.
 */
public class ConfigManager {
    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);
    private static final Dotenv dotenv;

    static {
        Dotenv temp = null;
        try {
            temp = Dotenv.configure()
                    .ignoreIfMissing() // Allow system env vars to override
                    .load();
        } catch (Exception e) {
            logger.warn("Could not load .env file, relying on system environment variables.");
        }
        dotenv = temp;
    }

    public static String get(String key) {
        String value = System.getenv(key);
        if (value == null && dotenv != null) {
            value = dotenv.get(key);
        }
        return value;
    }

    public static String get(String key, String defaultValue) {
        String value = get(key);
        return value != null ? value : defaultValue;
    }
}
