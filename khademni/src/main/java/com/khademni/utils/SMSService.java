package com.khademni.utils;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import java.io.InputStream;
import java.util.Properties;

public class SMSService {

    private static String ACCOUNT_SID;
    private static String AUTH_TOKEN;
    private static String FROM_NUMBER;
    private static boolean initialized = false;

    static {
        loadCredentials();
    }

    private static void loadCredentials() {
        ACCOUNT_SID = System.getenv("TWILIO_ACCOUNT_SID");
        AUTH_TOKEN = System.getenv("TWILIO_AUTH_TOKEN");
        FROM_NUMBER = System.getenv("TWILIO_PHONE_NUMBER");

        if (ACCOUNT_SID == null || AUTH_TOKEN == null || FROM_NUMBER == null) {
            try (InputStream input = SMSService.class.getClassLoader()
                    .getResourceAsStream("config.properties")) {
                if (input != null) {
                    Properties prop = new Properties();
                    prop.load(input);
                    if (ACCOUNT_SID == null || ACCOUNT_SID.isBlank()) {
                        ACCOUNT_SID = prop.getProperty("TWILIO_ACCOUNT_SID", "").trim();
                    }
                    if (AUTH_TOKEN == null || AUTH_TOKEN.isBlank()) {
                        AUTH_TOKEN = prop.getProperty("TWILIO_AUTH_TOKEN", "").trim();
                    }
                    if (FROM_NUMBER == null || FROM_NUMBER.isBlank()) {
                        FROM_NUMBER = prop.getProperty("TWILIO_PHONE_NUMBER", "").trim();
                    }
                }
            } catch (Exception e) {
                System.err.println("SMSService: Failed to load config.properties - " + e.getMessage());
            }
        }

        ACCOUNT_SID = ACCOUNT_SID != null ? ACCOUNT_SID.trim() : "";
        AUTH_TOKEN = AUTH_TOKEN != null ? AUTH_TOKEN.trim() : "";
        FROM_NUMBER = FROM_NUMBER != null ? FROM_NUMBER.trim() : "";

        if (!ACCOUNT_SID.isEmpty() && !ACCOUNT_SID.contains("your_") && !ACCOUNT_SID.equals("YOUR_ACCOUNT_SID_HERE")
                && !AUTH_TOKEN.isEmpty() && !AUTH_TOKEN.contains("your_") && !AUTH_TOKEN.equals("YOUR_AUTH_TOKEN_HERE")) {
            Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
            initialized = true;
            System.out.println("SMSService: Twilio initialized successfully.");
        } else {
            System.out.println("SMSService: Twilio credentials not configured. SMS sending is disabled.");
        }
    }

    /**
     * Sends an SMS to the given phone number.
     *
     * @param toPhone Full international number, e.g. "+21698765432"
     * @param message Text body of the SMS
     * @return true if sent successfully, false otherwise
     */
    public static boolean sendSms(String toPhone, String message) {
        if (!initialized) {
            System.out.println("SMSService: Twilio is not configured. Skipping SMS to " + toPhone);
            return false;
        }
        if (toPhone == null || toPhone.trim().isEmpty()) {
            System.out.println("SMSService: No phone number provided. Skipping SMS.");
            return false;
        }
        try {
            Message msg = Message.creator(
                    new PhoneNumber(toPhone),
                    new PhoneNumber(FROM_NUMBER),
                    message).create();
            System.out.println("SMSService: SMS sent successfully. SID = " + msg.getSid());
            return true;
        } catch (Exception e) {
            System.err.println("SMSService: Failed to send SMS - " + e.getMessage());
            return false;
        }
    }
}
