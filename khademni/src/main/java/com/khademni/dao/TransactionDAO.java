package com.khademni.dao;

import com.khademni.model.TransactionModel;
import com.khademni.exception.DataAccessException;

/**
 * DAO for deposit/withdrawal transactions (Stripe).
 */
public interface TransactionDAO {
    /**
     * Insert a new transaction.
     */
    void save(TransactionModel transaction) throws DataAccessException;

    /**
     * Check if a transaction with this Stripe payment id already exists (idempotence).
     */
    boolean existsByStripePaymentId(String stripePaymentId) throws DataAccessException;
}
