package com.khademni.dao;

import com.khademni.model.UserModel;
import com.khademni.exception.DataAccessException;
import java.util.Optional;
import java.util.List;

/**
 * Interface for User data access operations.
 * Decouples the storage logic from the service layer.
 */
public interface UserDAO {
    Optional<UserModel> findById(int id) throws DataAccessException;

    Optional<UserModel> findByEmail(String email) throws DataAccessException;

    Optional<UserModel> findByUniqueId(int uniqueId) throws DataAccessException;

    List<UserModel> findAll() throws DataAccessException;

    void save(UserModel user) throws DataAccessException;

    void update(UserModel user) throws DataAccessException;

    void delete(int id) throws DataAccessException;

    /**
     * Add amount to user balance (for Stripe deposit). Uses atomic UPDATE to avoid race conditions.
     */
    void addBalance(int userId, double amount) throws DataAccessException;

    /**
     * Set unique_id once, only if the user does not yet have a valid one (0 or not in 1000–9999).
     * Never overwrites an already registered unique_id.
     * @return true if one row was updated
     */
    boolean setUniqueIdOnce(int userId, int uniqueId) throws DataAccessException;
}
