package com.khademni.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.param.checkout.SessionRetrieveParams;

import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

public class StripeService {

    private String secretKey;
    private String defaultCurrency = "usd";

    public StripeService() {
        // Load configuration from stripe.properties or environment variables
        String envSecret = System.getenv("STRIPE_SECRET_KEY");
        String fileSecret = null;
        String currency = null;

        try (InputStream is = getClass().getResourceAsStream("/stripe.properties")) {
            if (is != null) {
                System.out.println("[StripeService] stripe.properties found.");
                Properties props = new Properties();
                props.load(is);
                fileSecret = props.getProperty("stripe.secret_key");
                System.out.println("[StripeService] Key from file: " + (fileSecret != null ? "Present" : "Null"));;
                currency = props.getProperty("stripe.default_currency");
            }
        } catch (Exception ignored) {
        }

        this.secretKey = (envSecret != null && !envSecret.isBlank()) ? envSecret
                : (fileSecret != null ? fileSecret : "");
        if (currency != null && !currency.isBlank()) {
            this.defaultCurrency = currency;
        }

        if (!this.secretKey.isBlank()) {
            Stripe.apiKey = this.secretKey;
            System.out.println("[StripeService] API Key configured: " + this.secretKey.substring(0, 7) + "********");
        }
    }

    public String createCheckoutSession(double amount, String description, long contratId) throws Exception {
        return createCheckoutSession(amount, this.defaultCurrency, description, contratId, null);
    }

    public String createCheckoutSession(double amount, String description, long contratId, String expectedPhone)
            throws Exception {
        return createCheckoutSession(amount, this.defaultCurrency, description, contratId, expectedPhone);
    }

    /**
     * Creates a Stripe Checkout Session with a specific currency.
     * The amount should already be in the target currency (not TND).
     */
    public String createCheckoutSession(double amount, String currency, String description, long contratId,
            String expectedPhone)
            throws Exception {
        if (this.secretKey.isBlank()) {
            throw new Exception("Stripe secret key not configured. Set STRIPE_SECRET_KEY.");
        }

        long amountCents = (Math.round(amount * 100));

        SessionCreateParams.Builder builder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl("https://example.com/success?session_id={CHECKOUT_SESSION_ID}&contrat_id=" + contratId)
                .setCancelUrl("https://example.com/cancel")
                .setPhoneNumberCollection(
                        SessionCreateParams.PhoneNumberCollection.builder()
                                .setEnabled(true)
                                .build())
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency(currency)
                                                .setUnitAmount(amountCents)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName(description == null ? "Service Payment"
                                                                        : description)
                                                                .build())
                                                .build())
                                .build());

        // Store the expected phone in metadata for post-payment verification
        if (expectedPhone != null && !expectedPhone.isBlank()) {
            builder.putMetadata("expected_phone", expectedPhone);
        }

        SessionCreateParams sessionParams = builder.build();

        Session session = Session.create(sessionParams);
        return session.getUrl();
    }

    public boolean verifySession(String sessionId) throws Exception {
        if (this.secretKey.isBlank()) {
            throw new Exception("Stripe secret key not configured.");
        }
        if (sessionId == null || sessionId.isBlank())
            return false;

        try {
            SessionRetrieveParams params = SessionRetrieveParams.builder().addExpand("payment_intent").build();
            Session session = Session.retrieve(sessionId, params, null);
            return "paid".equals(session.getPaymentStatus());
        } catch (StripeException e) {
            throw new Exception("Stripe API error: " + e.getMessage(), e);
        }
    }

    /**
     * Verifies payment AND checks if the phone number entered in Stripe matches the
     * expected one.
     * Returns: [0] = payment verified (boolean), [1] = phone matches (boolean), [2]
     * = entered phone (String)
     */
    public Object[] verifySessionWithPhone(String sessionId) throws Exception {
        if (this.secretKey.isBlank()) {
            throw new Exception("Stripe secret key not configured.");
        }
        if (sessionId == null || sessionId.isBlank())
            return new Object[] { false, false, "" };

        try {
            SessionRetrieveParams params = SessionRetrieveParams.builder().addExpand("payment_intent").build();
            Session session = Session.retrieve(sessionId, params, null);
            boolean paid = "paid".equals(session.getPaymentStatus());

            // Get the phone entered by customer in Stripe Checkout
            String customerPhone = session.getCustomerDetails() != null
                    ? session.getCustomerDetails().getPhone()
                    : null;

            // Get the expected phone from metadata
            Map<String, String> metadata = session.getMetadata();
            String expectedPhone = metadata != null ? metadata.get("expected_phone") : null;

            // Normalize and compare ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢EURÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â strip all non-digit chars
            boolean phoneMatch = true; // default to true if no expected phone
            if (expectedPhone != null && !expectedPhone.isBlank()) {
                String normalizedExpected = expectedPhone.replaceAll("[^0-9]", "");
                String normalizedEntered = customerPhone != null ? customerPhone.replaceAll("[^0-9]", "") : "";
                // Direct match OR suffix match (Stripe prepends country code e.g. +216)
                phoneMatch = normalizedExpected.equals(normalizedEntered)
                        || normalizedEntered.endsWith(normalizedExpected)
                        || normalizedExpected.endsWith(normalizedEntered);
            }

            return new Object[] { paid, phoneMatch, customerPhone != null ? customerPhone : "" };
        } catch (StripeException e) {
            throw new Exception("Stripe API error: " + e.getMessage(), e);
        }
    }

    /**
     * Create a Checkout Session for depositing money to user balance.
     * Amount in euros; payment interface Stripe affiche et facture en EUR.
     * Stored in metadata and converted to cents for Stripe (1 EUR = 100 centimes).
     */
    public String createDepositCheckoutSession(double amountEur, int userId) throws Exception {
        if (this.secretKey.isBlank()) {
            throw new Exception("Stripe secret key not configured. Set STRIPE_SECRET_KEY.");
        }
        if (amountEur <= 0) {
            throw new IllegalArgumentException("Le montant doit ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Âªtre strictement positif.");
        }
        long amountCents = Math.round(amountEur * 100);

        SessionCreateParams sessionParams = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl("https://example.com/deposit-success?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl("https://example.com/deposit-cancel")
                .putMetadata("user_id", String.valueOf(userId))
                .putMetadata("amount_eur", String.valueOf(amountEur))
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("eur")
                                                .setUnitAmount(amountCents)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName("DÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©pÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â´t sur le portefeuille 5ademni")
                                                                .setDescription(
                                                                        "Ajout de " + String.format("%.2f", amountEur)
                                                                                + " ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â  votre solde")
                                                                .build())
                                                .build())
                                .build())
                .build();

        Session session = Session.create(sessionParams);
        return session.getUrl();
    }

    /**
     * Retrieve a deposit Checkout Session and verify PaymentIntent status.
     * Only returns succeeded=true when Stripe reports status = "succeeded".
     */
    public DepositVerificationResult retrieveSessionForDeposit(String sessionId) throws Exception {
        if (this.secretKey.isBlank()) {
            throw new Exception("Stripe secret key not configured.");
        }
        if (sessionId == null || sessionId.isBlank()) {
            return new DepositVerificationResult(0, 0, null, false, "Session ID manquant.");
        }
        try {
            SessionRetrieveParams params = SessionRetrieveParams.builder().addExpand("payment_intent").build();
            Session session = Session.retrieve(sessionId, params, null);

            Map<String, String> metadata = session.getMetadata();
            if (metadata == null) {
                return new DepositVerificationResult(0, 0, null, false, "MÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©tadonnÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©es de session manquantes.");
            }
            String userIdStr = metadata.get("user_id");
            String amountEurStr = metadata.get("amount_eur");
            if (userIdStr == null || amountEurStr == null) {
                return new DepositVerificationResult(0, 0, null, false,
                        "user_id ou amount_eur manquant dans la session.");
            }
            int userId = Integer.parseInt(userIdStr);
            double amountEur = Double.parseDouble(amountEurStr);

            String paymentIntentId = null;
            String paymentStatus = null;
            Object piObj = session.getPaymentIntent();
            if (piObj instanceof PaymentIntent) {
                PaymentIntent pi = (PaymentIntent) piObj;
                paymentIntentId = pi.getId();
                paymentStatus = pi.getStatus();
            } else if (piObj instanceof String) {
                paymentIntentId = (String) piObj;
                PaymentIntent pi = PaymentIntent.retrieve(paymentIntentId);
                paymentStatus = pi.getStatus();
            } else if (piObj != null) {
                paymentIntentId = piObj.toString();
                PaymentIntent pi = PaymentIntent.retrieve(paymentIntentId);
                paymentStatus = pi.getStatus();
            }
            if (paymentIntentId == null) {
                return new DepositVerificationResult(userId, amountEur, null, false, "PaymentIntent introuvable.");
            }
            boolean succeeded = "succeeded".equals(paymentStatus);
            return new DepositVerificationResult(userId, amountEur, paymentIntentId, succeeded,
                    succeeded ? null : "Statut paiement: " + paymentStatus);
        } catch (StripeException e) {
            return new DepositVerificationResult(0, 0, null, false, "Stripe: " + e.getMessage());
        } catch (NumberFormatException e) {
            return new DepositVerificationResult(0, 0, null, false, "MÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©tadonnÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©es invalides: " + e.getMessage());
        }
    }
}
