package com.khademni.service;

import com.khademni.interfaces.ICommentaire;
import com.khademni.model.Article;
import com.khademni.model.Commentaire;
import com.khademni.model.UserModel;
import com.khademni.utils.MyDataBase;
import com.khademni.utils.SessionManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CommentaireController implements ICommentaire {

    private final Connection connection;

    public CommentaireController() {
        try {
            this.connection = MyDataBase.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to establish database connection", e);
        }
    }

    // ==============================
    // CREATE
    // ==============================
    @Override
    public Commentaire create(Commentaire commentaire) throws SQLException {
        if (commentaire.getUser() == null || commentaire.getUser().getId() <= 0) {
            throw new IllegalArgumentException("User must be set and have a valid ID.");
        }

        String sql = "INSERT INTO commentaire (content, status, created_at, article_id, user_id) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = MyDataBase.getConnection(); // Use a fresh connection
                PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, commentaire.getContent());
            stmt.setString(2, commentaire.getStatus());
            stmt.setTimestamp(3, Timestamp.valueOf(commentaire.getCreatedAt()));
            stmt.setLong(4, commentaire.getArticle().getId());
            stmt.setInt(5, commentaire.getUser().getId());

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    commentaire.setId(rs.getLong(1));
                }
            }
        }

        return commentaire;
    }

    public int countByArticle(Long articleId) throws SQLException {
        String query = "SELECT COUNT(*) FROM commentaire WHERE article_id = ?";
        try (Connection conn = MyDataBase.getConnection();
                PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setLong(1, articleId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    // ==============================
    // UPDATE COMPLET
    // ==============================
    @Override
    public boolean update(Commentaire commentaire) throws SQLException {

        String sql = "UPDATE commentaire SET content = ? WHERE id = ? AND user_id = ?";
        try (Connection conn = MyDataBase.getConnection(); // Use a fresh connection
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, commentaire.getContent());
            stmt.setLong(2, commentaire.getId());
            stmt.setInt(3, SessionManager.getCurrentUser().getId()); // Ensure only the author can update
            return stmt.executeUpdate() > 0;
        }
    }

    // ==============================
    // UPDATE CONTENU SEULEMENT (RECOMMANDÉ)
    // ==============================
    public boolean updateContent(Long id, String content) throws SQLException {

        String sql = "UPDATE commentaire SET content = ? WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, content);
            stmt.setLong(2, id);

            return stmt.executeUpdate() > 0;
        }
    }

    // ==============================
    // DELETE
    // ==============================
    @Override
    public boolean delete(Long id) throws SQLException {
        if (id == null) {
            throw new IllegalArgumentException("L'ID du commentaire ne peut pas être null.");
        }

        String sql = "DELETE FROM commentaire WHERE id = ?";
        try (Connection conn = MyDataBase.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    // ==============================
    // FIND BY ID
    // ==============================
    @Override
    public Commentaire findById(Long id) throws SQLException {

        String sql = "SELECT * FROM commentaire WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {
                    return map(rs);
                }
            }
        }

        return null;
    }

    // ==============================
    // FIND ALL
    // ==============================
    @Override
    public List<Commentaire> findAll() throws SQLException {

        String sql = "SELECT * FROM commentaire ORDER BY created_at DESC";
        List<Commentaire> commentaires = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                commentaires.add(map(rs));
            }
        }

        return commentaires;
    }

    // ==============================
    // FIND BY ARTICLE (IMPORTANT 🔥)
    // ==============================
    public List<Commentaire> findByArticleId(Long articleId) throws SQLException {
        String sql = "SELECT * FROM commentaire WHERE article_id = ? ORDER BY created_at DESC";
        List<Commentaire> commentaires = new ArrayList<>();

        try (Connection conn = MyDataBase.getConnection(); // Use a fresh connection
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, articleId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    commentaires.add(map(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }

        return commentaires;
    }

    // ==============================
    // MAPPING RESULTSET → OBJECT
    // ==============================
    private Commentaire map(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        String content = rs.getString("content");
        String status = rs.getString("status");
        LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();

        Article article = new Article();
        article.setId(rs.getLong("article_id"));

        // Récupérer l'utilisateur
        UserModel user = new UserModel();
        user.setId(rs.getInt("user_id"));

        // Requête pour récupérer le prénom et le nom de l'utilisateur
        String userSql = "SELECT first_name, last_name FROM users WHERE id = ?";
        try (PreparedStatement stmtUser = MyDataBase.getConnection().prepareStatement(userSql)) {
            stmtUser.setInt(1, user.getId());
            try (ResultSet rsUser = stmtUser.executeQuery()) {
                if (rsUser.next()) {
                    user.setFirstName(rsUser.getString("first_name"));
                    user.setLastName(rsUser.getString("last_name"));
                }
            }
        }

        return new Commentaire(id, content, status, createdAt, article, user);
    }
}
