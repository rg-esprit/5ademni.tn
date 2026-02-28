package com.khademni.dao.impl;

import com.khademni.dao.OffreDAO;
import com.khademni.model.OffreModel;
import com.khademni.utils.MyDataBase;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OffreDAOImpl implements OffreDAO {

    @Override
    public OffreModel findById(int id) throws SQLException {
        // Implementation for common fields if merged, but currently they are separate
        // tables
        return null;
    }

    @Override
    public List<OffreModel> findByType(String type) throws SQLException {
        String table = "OFFRE".equalsIgnoreCase(type) ? "offres" : "demandes";
        // Join with users to get the 4-digit unique_id
        // Use LEFT JOIN on unique_id to handle both cases if needed, but primarily
        // unique_id
        String query = "SELECT t.*, u.unique_id FROM " + table
                + " t LEFT JOIN users u ON t.user_id = u.id WHERE t.statut != 'ANNULE'";
        List<OffreModel> list = new ArrayList<>();

        try (Connection conn = MyDataBase.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                list.add(mapResultSetToModel(rs, type));
            }
        }
        return list;
    }

    @Override
    public List<OffreModel> findByUserId(int userId, String type) throws SQLException {
        String table = "OFFRE".equalsIgnoreCase(type) ? "offres" : "demandes";
        // Join with users to get the 4-digit unique_id
        String query = "SELECT t.*, u.unique_id FROM " + table
                + " t LEFT JOIN users u ON t.user_id = u.id WHERE t.user_id = ? AND t.statut != 'ANNULE'";
        List<OffreModel> list = new ArrayList<>();

        try (Connection conn = MyDataBase.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToModel(rs, type));
                }
            }
        }
        return list;
    }

    @Override
    public void save(OffreModel offre) throws SQLException {
        String table = "OFFRE".equalsIgnoreCase(offre.getType()) ? "offres" : "demandes";
        String query = "INSERT INTO " + table
                + " (user_id, titre, description, prix, date_creation, statut, date_limite) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = MyDataBase.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, offre.getUserId());
            stmt.setString(2, offre.getTitre());
            stmt.setString(3, offre.getDescription());
            stmt.setDouble(4, offre.getPrix());
            stmt.setTimestamp(5, Timestamp.valueOf(offre.getDateCreation()));
            stmt.setString(6, offre.getStatut());
            stmt.setTimestamp(7, offre.getDateLimite() != null ? Timestamp.valueOf(offre.getDateLimite()) : null);
            stmt.executeUpdate();
        }
    }

    @Override
    public void update(OffreModel offre) throws SQLException {
        String table = "OFFRE".equalsIgnoreCase(offre.getType()) ? "offres" : "demandes";
        String query = "UPDATE " + table
                + " SET user_id = ?, titre = ?, description = ?, prix = ?, statut = ?, date_creation = ?, date_limite = ? WHERE id = ?";

        try (Connection conn = MyDataBase.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, offre.getUserId());
            stmt.setString(2, offre.getTitre());
            stmt.setString(3, offre.getDescription());
            stmt.setDouble(4, offre.getPrix());
            stmt.setString(5, offre.getStatut());
            stmt.setTimestamp(6, Timestamp.valueOf(offre.getDateCreation()));
            stmt.setTimestamp(7, offre.getDateLimite() != null ? Timestamp.valueOf(offre.getDateLimite()) : null);
            stmt.setInt(8, offre.getId());
            stmt.executeUpdate();
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        // Ambiguous without type, but we'll add a specialized method or handle it in
        // service
    }

    public void deleteWithType(int id, String type) throws SQLException {
        String table = "OFFRE".equalsIgnoreCase(type) ? "offres" : "demandes";
        String query = "DELETE FROM " + table + " WHERE id = ?";
        try (Connection conn = MyDataBase.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    private OffreModel mapResultSetToModel(ResultSet rs, String type) throws SQLException {
        Timestamp deadlineTs = rs.getTimestamp("date_limite");
        LocalDateTime deadline = deadlineTs != null ? deadlineTs.toLocalDateTime() : null;

        return new OffreModel(
                rs.getInt("id"),
                rs.getInt("user_id"), // Technical ID
                rs.getInt("unique_id"), // Business ID (4-digit)
                rs.getString("titre"),
                rs.getString("description"),
                rs.getDouble("prix"),
                rs.getTimestamp("date_creation").toLocalDateTime(),
                rs.getString("statut"),
                type,
                deadline,
                null); // userName is resolved in OffreController's loadOffres()
    }
}
