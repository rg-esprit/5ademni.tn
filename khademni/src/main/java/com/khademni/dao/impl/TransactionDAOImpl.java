package com.khademni.dao.impl;

import com.khademni.dao.TransactionDAO;
import com.khademni.model.TransactionModel;
import com.khademni.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;

public class TransactionDAOImpl implements TransactionDAO {
    private static final Logger logger = LoggerFactory.getLogger(TransactionDAOImpl.class);
    private final Connection connection;

    public TransactionDAOImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void save(TransactionModel transaction) throws DataAccessException {
        String sql = "INSERT INTO transactions (user_id, stripe_payment_id, amount, status, created_at) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, transaction.getUserId());
            ps.setString(2, transaction.getStripePaymentId());
            ps.setDouble(3, transaction.getAmount());
            ps.setString(4, transaction.getStatus());
            ps.setTimestamp(5, transaction.getCreatedAt() != null
                    ? Timestamp.valueOf(transaction.getCreatedAt())
                    : Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    transaction.setId(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            logger.error("Error saving transaction for user {} stripe {}", transaction.getUserId(), transaction.getStripePaymentId(), e);
            throw new DataAccessException("Failed to save transaction", e);
        }
    }

    @Override
    public boolean existsByStripePaymentId(String stripePaymentId) throws DataAccessException {
        if (stripePaymentId == null || stripePaymentId.isBlank()) {
            return false;
        }
        String sql = "SELECT 1 FROM transactions WHERE stripe_payment_id = ? LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, stripePaymentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.error("Error checking transaction by stripe_payment_id: {}", stripePaymentId, e);
            throw new DataAccessException("Failed to check transaction", e);
        }
    }
}
