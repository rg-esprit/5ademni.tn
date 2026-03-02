package com.khademni.dao.impl;

import com.khademni.dao.UserDAO;
import com.khademni.model.UserModel;
import com.khademni.model.UserRole;
import com.khademni.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC Implementation of UserDAO.
 * Handles row mapping and SQL execution.
 */
public class UserDAOImpl implements UserDAO {
    private static final Logger logger = LoggerFactory.getLogger(UserDAOImpl.class);
    private final Connection connection;

    public UserDAOImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Optional<UserModel> findByEmail(String email) throws DataAccessException {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding user by email: {}", email, e);
            throw new DataAccessException("Failed to find user by email", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<UserModel> findByUniqueId(int uniqueId) throws DataAccessException {
        String sql = "SELECT * FROM users WHERE unique_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, uniqueId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding user by unique ID: {}", uniqueId, e);
            throw new DataAccessException("Failed to find user by unique ID", e);
        }
        return Optional.empty();
    }

    @Override
    public void save(UserModel user) throws DataAccessException {
        String sql = "INSERT INTO users (unique_id, current_mode, first_name, last_name, date_of_birth, email, password, bio, profile_image) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, user.getUniqueId());
            ps.setString(2, user.getCurrentMode().name());
            ps.setString(3, user.getFirstName());
            ps.setString(4, user.getLastName());
            ps.setDate(5, user.getDateOfBirth() != null ? Date.valueOf(user.getDateOfBirth()) : null);
            ps.setString(6, user.getEmail());
            ps.setString(7, user.getPassword());
            ps.setString(8, user.getBio());
            ps.setString(9, user.getProfileImage());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    user.setId(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            logger.error("Error saving user: {}", user.getEmail(), e);
            throw new DataAccessException("Failed to save user", e);
        }
    }

    @Override
    public Optional<UserModel> findById(int id) throws DataAccessException {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding user by ID: {}", id, e);
            throw new DataAccessException("Failed to find user by ID", e);
        }
        return Optional.empty();
    }

    @Override
    public List<UserModel> findAll() throws DataAccessException {
        List<UserModel> users = new ArrayList<>();
        String sql = "SELECT * FROM users";
        try (Statement st = connection.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding all users", e);
            throw new DataAccessException("Failed to find all users", e);
        }
        return users;
    }

    @Override
    public void update(UserModel user) throws DataAccessException {
        String sql = "UPDATE users SET first_name=?, last_name=?, date_of_birth=?, bio=?, current_mode=?, profile_image=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, user.getFirstName());
            ps.setString(2, user.getLastName());
            ps.setDate(3, user.getDateOfBirth() != null ? Date.valueOf(user.getDateOfBirth()) : null);
            ps.setString(4, user.getBio());
            ps.setString(5, user.getCurrentMode().name());
            ps.setString(6, user.getProfileImage());
            ps.setInt(7, user.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating user: {}", user.getId(), e);
            throw new DataAccessException("Failed to update user", e);
        }
    }

    @Override
    public void delete(int id) throws DataAccessException {
        String sql = "DELETE FROM users WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error deleting user: {}", id, e);
            throw new DataAccessException("Failed to delete user", e);
        }
    }

    @Override
    public void addBalance(int userId, double amount) throws DataAccessException {
        if (amount <= 0) {
            return;
        }
        String sql = "UPDATE users SET balance = balance + ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setDouble(1, amount);
            ps.setInt(2, userId);
            int updated = ps.executeUpdate();
            if (updated == 0) {
                logger.warn("addBalance: no user found for id {}", userId);
            }
        } catch (SQLException e) {
            logger.error("Error adding balance for user: {}", userId, e);
            throw new DataAccessException("Failed to add balance", e);
        }
    }

    @Override
    public boolean setUniqueIdOnce(int userId, int uniqueId) throws DataAccessException {
        String sql = "UPDATE users SET unique_id = ? WHERE id = ? AND (unique_id = 0 OR unique_id < 1000 OR unique_id > 9999)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, uniqueId);
            ps.setInt(2, userId);
            int updated = ps.executeUpdate();
            return updated > 0;
        } catch (SQLException e) {
            logger.error("Error setUniqueIdOnce for user: {}", userId, e);
            throw new DataAccessException("Failed to set unique_id", e);
        }
    }

    private UserModel mapResultSetToUser(ResultSet rs) throws SQLException {
        UserModel user = new UserModel();
        user.setId(rs.getInt("id"));
        user.setUniqueId(rs.getInt("unique_id"));
        user.setFirstName(rs.getString("first_name"));
        user.setLastName(rs.getString("last_name"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password"));
        user.setBio(rs.getString("bio"));
        user.setProfileImage(rs.getString("profile_image"));
        user.setCurrentMode(UserRole.valueOf(rs.getString("current_mode")));
        user.setBalance(rs.getDouble("balance"));
        // ... map other fields ...
        return user;
    }
}
