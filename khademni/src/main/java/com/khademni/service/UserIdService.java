package com.khademni.service;

import com.khademni.dao.UserDAO;
import com.khademni.dao.impl.UserDAOImpl;
import com.khademni.utils.MyDataBase;
import java.sql.Connection;
import java.sql.SQLException;

import com.khademni.model.UserModel;

public class UserIdService {

    public static boolean isValid(int id) {
        return id >= 1000 && id <= 9999;
    }

    /** Returns true if the user has a valid 4-digit unique_id (already registered and not modifiable). */
    public static boolean hasValidUniqueId(UserModel user) {
        return user != null && isValid(user.getUniqueId());
    }

    public static boolean isIdAvailable(int id) {
        if (!isValid(id))
            return false;
        try (Connection conn = MyDataBase.getConnection()) {
            UserDAO dao = new UserDAOImpl(conn);
            return !dao.findByUniqueId(id).isPresent();
        } catch (SQLException e) {
            return false;
        }
    }

    public static int generateUniqueId() throws SQLException {
        java.util.Random random = new java.util.Random();
        int id;
        boolean available;
        int attempts = 0;
        do {
            id = 1000 + random.nextInt(9000); // [1000, 9999]
            available = isIdAvailable(id);
            attempts++;
            if (attempts > 100) {
                throw new SQLException("Could not generate a unique 4-digit ID after 100 attempts.");
            }
        } while (!available);
        return id;
    }

    public static int resolveTechnicalId(int uniqueId) throws SQLException {
        try (Connection conn = MyDataBase.getConnection()) {
            UserDAO dao = new UserDAOImpl(conn);
            return dao.findByUniqueId(uniqueId)
                    .map(com.khademni.model.UserModel::getId)
                    .orElseThrow(() -> new SQLException("Utilisateur introuvable avec l'ID : " + uniqueId));
        } catch (com.khademni.exception.DataAccessException e) {
            throw new SQLException("Erreur d'accès aux données : " + e.getMessage());
        }
    }
}
