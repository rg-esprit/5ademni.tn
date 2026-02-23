package com.khademni.model;

import java.time.LocalDateTime;

/**
 * Model for deposit/withdrawal transactions (Stripe) stored in table transactions.
 */
public class TransactionModel {
    private int id;
    private int userId;
    private String stripePaymentId;
    private double amount;
    private String status;
    private LocalDateTime createdAt;

    public TransactionModel() {
    }

    public TransactionModel(int userId, String stripePaymentId, double amount, String status) {
        this.userId = userId;
        this.stripePaymentId = stripePaymentId;
        this.amount = amount;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getStripePaymentId() {
        return stripePaymentId;
    }

    public void setStripePaymentId(String stripePaymentId) {
        this.stripePaymentId = stripePaymentId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
