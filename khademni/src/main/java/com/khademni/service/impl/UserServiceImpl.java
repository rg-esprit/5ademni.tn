package com.khademni.service.impl;

import com.khademni.dao.TransactionDAO;
import com.khademni.dao.UserDAO;
import com.khademni.dao.impl.TransactionDAOImpl;
import com.khademni.dao.impl.UserDAOImpl;
import com.khademni.exception.BusinessException;
import com.khademni.model.TransactionModel;
import com.khademni.model.UserModel;
import com.khademni.service.UserIdService;
import com.khademni.service.UserService;
import com.khademni.utils.MyDataBase;
import com.khademni.utils.PasswordUtil;
import com.khademni.utils.ValidationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Implementation of UserService.
 * Orchestrates DAO calls and applies business rules.
 */
public class UserServiceImpl implements UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    @Override
    public UserModel login(String email, String password) throws BusinessException {
        try (Connection conn = MyDataBase.getConnection()) {
            UserDAO userDAO = new UserDAOImpl(conn);
            Optional<UserModel> userOpt = userDAO.findByEmail(email);

            if (userOpt.isPresent()) {
                UserModel user = userOpt.get();
                if (PasswordUtil.checkPassword(password, user.getPassword())) {
                    logger.info("User logged in successfully: {}", email);
                    return user;
                }
            }
            throw new BusinessException("Identifiants de connexion invalides.");
        } catch (SQLException e) {
            logger.error("Database error during login for user: {}", email, e);
            throw new BusinessException("Une erreur serveur est survenue.");
        }
    }

    @Override
    public void register(UserModel user) throws BusinessException {
        // 1. Deep Validation
        if (!ValidationUtils.isValidEmail(user.getEmail())) {
            throw new BusinessException("Format d'email invalide.");
        }
        if (!com.khademni.service.UserIdService.isValid(user.getUniqueId())) {
            throw new BusinessException("L'ID doit comporter exactement 4 chiffres.");
        }
        if (!ValidationUtils.isStrongPassword(user.getPassword())) {
            throw new BusinessException(
                    "Le mot de passe doit contenir au moins 8 caractères, dont une lettre et un chiffre.");
        }

        try (Connection conn = MyDataBase.getConnection()) {
            // Transaction management
            conn.setAutoCommit(false);
            try {
                UserDAO userDAO = new UserDAOImpl(conn);
                if (userDAO.findByEmail(user.getEmail()).isPresent()) {
                    throw new BusinessException("Un compte avec cet email existe déjà.");
                }

                // Hash password before saving
                user.setPassword(PasswordUtil.hashPassword(user.getPassword()));
                userDAO.save(user);

                conn.commit();
                logger.info("User registered successfully: {}", user.getEmail());
            } catch (Exception e) {
                conn.rollback();
                if (e instanceof BusinessException)
                    throw (BusinessException) e;
                logger.error("Error during user registration", e);
                throw new BusinessException("L'inscription a échoué.");
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.error("Database connection error during registration", e);
            throw new BusinessException("Erreur de connexion serveur.");
        }
    }

    @Override
    public void updateProfile(UserModel user) throws BusinessException {
        try (Connection conn = MyDataBase.getConnection()) {
            UserDAO userDAO = new UserDAOImpl(conn);
            userDAO.update(user);
            logger.info("User profile updated: {}", user.getId());
        } catch (SQLException e) {
            logger.error("Error updating profile for user: {}", user.getId(), e);
            throw new BusinessException("Mise à jour du profil impossible.");
        }
    }

    @Override
    public Optional<UserModel> getUserById(int id) {
        try (Connection conn = MyDataBase.getConnection()) {
            UserDAO userDAO = new UserDAOImpl(conn);
            return userDAO.findById(id);
        } catch (SQLException e) {
            logger.error("Error fetching user by ID: {}", id, e);
            return Optional.empty();
        }
    }

    @Override
    public void deposit(int userId, double amountEur, String stripePaymentId) throws BusinessException {
        if (amountEur <= 0) {
            throw new BusinessException("Le montant doit être strictement positif.");
        }
        if (stripePaymentId == null || stripePaymentId.isBlank()) {
            throw new BusinessException("Identifiant de paiement Stripe manquant.");
        }
        try (Connection conn = MyDataBase.getConnection()) {
            conn.setAutoCommit(false);
            try {
                TransactionDAO transactionDAO = new TransactionDAOImpl(conn);
                UserDAO userDAO = new UserDAOImpl(conn);
                if (transactionDAO.existsByStripePaymentId(stripePaymentId)) {
                    logger.info("Deposit already processed for stripe_payment_id {}", stripePaymentId);
                    conn.rollback();
                    return; // idempotent: already credited
                }
                userDAO.addBalance(userId, amountEur);
                TransactionModel tx = new TransactionModel(userId, stripePaymentId, amountEur, "succeeded");
                tx.setCreatedAt(LocalDateTime.now());
                transactionDAO.save(tx);
                conn.commit();
                logger.info("Deposit successful: user {} amount {} EUR stripe {}", userId, amountEur, stripePaymentId);
            } catch (Exception e) {
                conn.rollback();
                if (e instanceof BusinessException) {
                    throw (BusinessException) e;
                }
                logger.error("Error during deposit", e);
                throw new BusinessException("Échec du dépôt: " + e.getMessage());
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.error("Database error during deposit", e);
            throw new BusinessException("Erreur de connexion serveur.");
        }
    }

    @Override
    public void setUniqueIdOnceIfMissing(int userId, int uniqueId) throws BusinessException {
        if (!UserIdService.isValid(uniqueId)) {
            throw new BusinessException("L'ID doit comporter exactement 4 chiffres (1000–9999).");
        }
        if (!UserIdService.isIdAvailable(uniqueId)) {
            throw new BusinessException("Cet ID est déjà utilisé. Veuillez en choisir un autre.");
        }
        try (Connection conn = MyDataBase.getConnection()) {
            UserDAO userDAO = new UserDAOImpl(conn);
            boolean updated = userDAO.setUniqueIdOnce(userId, uniqueId);
            if (!updated) {
                Optional<UserModel> u = userDAO.findById(userId);
                if (u.isPresent() && UserIdService.hasValidUniqueId(u.get())) {
                    return;
                }
                throw new BusinessException("Impossible d'enregistrer l'ID (déjà défini ou compte introuvable).");
            }
        } catch (SQLException e) {
            logger.error("Error setUniqueIdOnce for user {}", userId, e);
            throw new BusinessException("Erreur serveur.");
        }
    }
}
