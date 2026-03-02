package com.khademni.service;

import com.khademni.model.UserModel;
import com.khademni.exception.BusinessException;
import java.util.Optional;

/**
 * Service interface for User-related business logic.
 */
public interface UserService {
    UserModel login(String email, String password) throws BusinessException;

    void register(UserModel user) throws BusinessException;

    void updateProfile(UserModel user) throws BusinessException;

    Optional<UserModel> getUserById(int id);

    /**
     * Credit user balance after a successful Stripe payment. Idempotent per stripePaymentId.
     * @param userId user to credit
     * @param amountEur amount in euros (€)
     * @param stripePaymentId Stripe PaymentIntent id for audit
     */
    void deposit(int userId, double amountEur, String stripePaymentId) throws BusinessException;

    /**
     * Set the user's unique_id once, only if not already set (valid 4-digit).
     * Used when the user chooses their ID at first offer or first contract creation.
     * @throws BusinessException if uniqueId invalid, already used by another user, or cannot be set
     */
    void setUniqueIdOnceIfMissing(int userId, int uniqueId) throws BusinessException;
}
