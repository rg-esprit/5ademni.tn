package com.khademni.service;

/**
 * Result of verifying a Stripe Checkout Session for a deposit (balance top-up).
 * Used to decide whether to credit the user and record the transaction.
 */
public class DepositVerificationResult {
    private final int userId;
    private final double amountDt;
    private final String stripePaymentId;
    private final boolean succeeded;
    private final String errorMessage;

    public DepositVerificationResult(int userId, double amountDt, String stripePaymentId, boolean succeeded, String errorMessage) {
        this.userId = userId;
        this.amountDt = amountDt;
        this.stripePaymentId = stripePaymentId;
        this.succeeded = succeeded;
        this.errorMessage = errorMessage;
    }

    public int getUserId() {
        return userId;
    }

    public double getAmountDt() {
        return amountDt;
    }

    public String getStripePaymentId() {
        return stripePaymentId;
    }

    public boolean isSucceeded() {
        return succeeded;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
