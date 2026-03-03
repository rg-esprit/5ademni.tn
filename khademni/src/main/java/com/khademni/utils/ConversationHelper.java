package com.khademni.utils;

import com.khademni.model.ConversationModel;
import java.sql.*;
import java.time.LocalDateTime;

public class ConversationHelper {

    public static int createOrGetConversation(int userId1, int userId2, String title) throws SQLException {
        if (userId1 == userId2) {
            throw new SQLException("Cannot create conversation with yourself");
        }

        Connection conn = MyDataBase.getConnection();
        
        // Check if conversation already exists
        String checkQuery = """
            SELECT id FROM conversation 
            WHERE ((client_id = ? AND freelance_id = ?) OR (client_id = ? AND freelance_id = ?))
            LIMIT 1
        """;
        
        try (PreparedStatement ps = conn.prepareStatement(checkQuery)) {
            ps.setInt(1, userId1);
            ps.setInt(2, userId2);
            ps.setInt(3, userId2);
            ps.setInt(4, userId1);
            
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int existingId = rs.getInt("id");
                conn.close();
                return existingId;
            }
        }
        
        // Determine client and freelance roles
        int clientId = Math.min(userId1, userId2);
        int freelanceId = Math.max(userId1, userId2);
        
        // Create new conversation
        String insertQuery = """
            INSERT INTO conversation (client_id, freelance_id, titre, statut, date_creation)
            VALUES (?, ?, ?, 'ACTIVE', ?)
        """;
        
        try (PreparedStatement ps = conn.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, clientId);
            ps.setInt(2, freelanceId);
            ps.setString(3, title != null ? title : "New Conversation");
            ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            
            ps.executeUpdate();
            
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                int newId = rs.getInt(1);
                conn.close();
                return newId;
            }
        }
        
        conn.close();
        throw new SQLException("Failed to create conversation");
    }

    public static boolean isAdmin() {
        try {
            return com.khademni.App.currentUser != null && com.khademni.App.currentUser.isIsAdmin();
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isOwner(int entityUserId) {
        try {
            if (com.khademni.App.currentUser == null) {
                return false;
            }
            return com.khademni.App.currentUser.getId() == entityUserId || isAdmin();
        } catch (Exception e) {
            return false;
        }
    }
}
