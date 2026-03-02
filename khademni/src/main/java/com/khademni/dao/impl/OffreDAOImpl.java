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
    public List<OffreModel> findAllCombined() throws SQLException {
        String query = "SELECT t.id, t.user_id, u.unique_id, CONCAT(u.first_name, ' ', u.last_name) AS user_name, t.titre, t.description, t.prix, t.date_creation, t.date_limite, t.statut, 'OFFRE' as type "
                + "FROM offres t LEFT JOIN users u ON t.user_id = u.id " +
                "UNION ALL " +
                "SELECT t.id, t.user_id, u.unique_id, CONCAT(u.first_name, ' ', u.last_name) AS user_name, t.titre, t.description, t.prix, t.date_creation, t.date_limite, t.statut, 'DEMANDE' as type "
                + "FROM demandes t LEFT JOIN users u ON t.user_id = u.id " +
                "ORDER BY date_creation DESC";
        List<OffreModel> list = new ArrayList<>();
        try (Connection conn = MyDataBase.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                OffreModel offre = new OffreModel(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getInt("unique_id"),
                        rs.getString("titre"),
                        rs.getString("description"),
                        rs.getDouble("prix"),
                        rs.getTimestamp("date_creation").toLocalDateTime(),
                        rs.getString("statut"),
                        rs.getString("type"),
                        rs.getTimestamp("date_limite") != null ? rs.getTimestamp("date_limite").toLocalDateTime()
                                : null,
                        rs.getString("user_name"));
                list.add(offre);
            }
        }
        return list;
    }

    @Override
    public List<OffreModel> findByUserId(int userId, String type) throws SQLException {
        String table = "OFFRE".equalsIgnoreCase(type) ? "offres" : "demandes";
        // Join with users to get the 4-digit unique_id
        String query = "SELECT t.*, u.unique_id FROM " + table
                + " t LEFT JOIN users u ON t.user_id = u.id WHERE t.user_id = ?";
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

    @Override
    public void deleteWithType(int id, String type) throws SQLException {
        String table = "OFFRE".equalsIgnoreCase(type) ? "offres" : "demandes";
        String query = "DELETE FROM " + table + " WHERE id = ?";
        try (Connection conn = MyDataBase.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
            MyDataBase.resetAutoIncrementIfEmpty(table, 0);
        }
    }

    private OffreModel mapResultSetToModel(ResultSet rs, String type) throws SQLException {
        Timestamp deadlineTs = rs.getTimestamp("date_limite");
        LocalDateTime deadline = deadlineTs != null ? deadlineTs.toLocalDateTime() : null;

        int uniqueId = rs.getInt("user_id"); // Fallback
        try {
            uniqueId = rs.getInt("unique_id");
        } catch (SQLException e) {
            // Ignored, fallback stands
        }

        return new OffreModel(
                rs.getInt("id"),
                rs.getInt("user_id"), // Technical ID
                uniqueId, // Business ID (4-digit)
                rs.getString("titre"),
                rs.getString("description"),
                rs.getDouble("prix"),
                rs.getTimestamp("date_creation").toLocalDateTime(),
                rs.getString("statut"),
                type,
                deadline,
                null); // userName is resolved in OffreController's loadOffres()
    }

    @Override
    public List<OffreModel> searchPaginated(String query, int offset, int limit) throws SQLException {
        String baseQuery = "SELECT t.id, t.user_id, u.unique_id, CONCAT(u.first_name, ' ', u.last_name) AS user_name, t.titre, t.description, t.prix, t.date_creation, t.date_limite, t.statut, 'OFFRE' as type "
                + "FROM offres t LEFT JOIN users u ON t.user_id = u.id " +
                "UNION ALL " +
                "SELECT t.id, t.user_id, u.unique_id, CONCAT(u.first_name, ' ', u.last_name) AS user_name, t.titre, t.description, t.prix, t.date_creation, t.date_limite, t.statut, 'DEMANDE' as type "
                + "FROM demandes t LEFT JOIN users u ON t.user_id = u.id";

        boolean hasFilter = query != null && !query.trim().isEmpty();
        if (hasFilter) {
            baseQuery = "SELECT * FROM (" + baseQuery
                    + ") AS combined WHERE combined.titre LIKE ? OR combined.description LIKE ? OR combined.statut LIKE ? ORDER BY date_creation DESC LIMIT ? OFFSET ?";
        } else {
            baseQuery = baseQuery + " ORDER BY date_creation DESC LIMIT ? OFFSET ?";
        }

        List<OffreModel> list = new ArrayList<>();
        try (Connection conn = MyDataBase.getConnection();
                PreparedStatement stmt = conn.prepareStatement(baseQuery)) {

            int paramIndex = 1;
            if (hasFilter) {
                String likePattern = "%" + query.trim() + "%";
                stmt.setString(paramIndex++, likePattern);
                stmt.setString(paramIndex++, likePattern);
                stmt.setString(paramIndex++, likePattern);
            }
            stmt.setInt(paramIndex++, limit);
            stmt.setInt(paramIndex, offset);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    OffreModel offre = new OffreModel(
                            rs.getInt("id"),
                            rs.getInt("user_id"),
                            rs.getInt("unique_id"),
                            rs.getString("titre"),
                            rs.getString("description"),
                            rs.getDouble("prix"),
                            rs.getTimestamp("date_creation").toLocalDateTime(),
                            rs.getString("statut"),
                            rs.getString("type"),
                            rs.getTimestamp("date_limite") != null ? rs.getTimestamp("date_limite").toLocalDateTime()
                                    : null,
                            rs.getString("user_name"));
                    list.add(offre);
                }
            }
        }
        return list;
    }

    @Override
    public int countSearchResults(String query) throws SQLException {
        String baseQuery = "SELECT COUNT(*) FROM (" +
                "SELECT titre, description, statut FROM offres " +
                "UNION ALL " +
                "SELECT titre, description, statut FROM demandes) AS combined";

        boolean hasFilter = query != null && !query.trim().isEmpty();
        if (hasFilter) {
            baseQuery += " WHERE titre LIKE ? OR description LIKE ? OR statut LIKE ?";
        }

        try (Connection conn = MyDataBase.getConnection();
                PreparedStatement stmt = conn.prepareStatement(baseQuery)) {

            if (hasFilter) {
                String likePattern = "%" + query.trim() + "%";
                stmt.setString(1, likePattern);
                stmt.setString(2, likePattern);
                stmt.setString(3, likePattern);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }
}
