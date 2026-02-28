package com.khademni.service;

import com.khademni.model.Article;
import com.khademni.model.GigModel;
import com.khademni.interfaces.IArticle;
import com.khademni.utils.MyDataBase;
import com.khademni.service.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ArticleController implements IArticle {
    private final FavoriController favoriService = new FavoriController();
    private final CommentaireController commentaireService = new CommentaireController();

    private final Connection connection;

    public ArticleController() {
        try {
            this.connection = MyDataBase.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to establish database connection", e);
        }
    }

    public int getFavoriCount(Article article) throws SQLException {
        return favoriService.countByArticle(article.getId());
    }

    public int getCommentCount(Article article) throws SQLException {
        return commentaireService.countByArticle(article.getId());
    }

    @Override
    public Article create(Article article) throws SQLException {
        String sql = "INSERT INTO article (title, content, status, created_at, image_path) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = MyDataBase.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, article.getTitle());
            stmt.setString(2, article.getContent());
            stmt.setString(3, article.getStatus());
            stmt.setTimestamp(4, java.sql.Timestamp.valueOf(article.getCreatedAt()));
            stmt.setString(5, article.getImagePath());

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("L'insertion de l'article a échoué, aucune ligne affectée.");
            }

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    article.setId(rs.getLong(1));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'insertion de l'article : " + e.getMessage());
            throw e;
        }

        return article;
    }

    @Override
    public boolean update(Article article) throws SQLException {
        String sql = "UPDATE article SET title = ?, content = ?, status = ?, image_path = ? WHERE id = ?";

        try (Connection conn = MyDataBase.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, article.getTitle());
            stmt.setString(2, article.getContent());
            stmt.setString(3, article.getStatus());
            stmt.setString(4, article.getImagePath());
            stmt.setLong(5, article.getId());

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lors de la mise à jour de l'article : " + e.getMessage());
            throw e;
        }
    }

    @Override
    public boolean delete(Long id) throws SQLException {
        String sql = "DELETE FROM article WHERE id = ?";

        try (Connection conn = MyDataBase.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression de l'article : " + e.getMessage());
            throw e;
        }
    }

    @Override
    public Article findById(Long id) throws SQLException {
        String sql = "SELECT * FROM article WHERE id = ?";
        try (Connection conn = MyDataBase.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapArticle(rs);
                }
            }
        }
        return null;
    }

    // change avec executeUpdate
    // le query dans affichage
    @Override
    public List<Article> findAll() throws SQLException {
        String sql = "SELECT * FROM article ORDER BY created_at DESC";
        List<Article> articles = new ArrayList<>();

        try (Connection conn = MyDataBase.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                articles.add(mapArticle(rs));
            }
        }

        return articles;
    }

    private Article mapArticle(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        String title = rs.getString("title");
        String content = rs.getString("content");
        String status = rs.getString("status");

        Timestamp ts = rs.getTimestamp("created_at");
        LocalDateTime createdAt = ts != null ? ts.toLocalDateTime() : null;
        String imagePath = rs.getString("image_path");

        return new Article(id, title, content, status, createdAt, imagePath);
    }

    private List<GigModel> findGigsByKeywords(List<String> keywords) throws SQLException {
        List<GigModel> gigs = new ArrayList<>();

        // Construire une requête SQL dynamique avec des mots-clés
        StringBuilder queryBuilder = new StringBuilder("""
                    SELECT g.id, g.title, g.description, g.price, g.delivery_time, g.image, g.status
                    FROM gig g
                    WHERE
                """);

        for (int i = 0; i < keywords.size(); i++) {
            queryBuilder.append("LOWER(g.title) LIKE ? OR LOWER(g.description) LIKE ?");
            if (i < keywords.size() - 1) {
                queryBuilder.append(" OR ");
            }
        }

        try (Connection conn = MyDataBase.getConnection();
                PreparedStatement ps = conn.prepareStatement(queryBuilder.toString())) {

            // Ajouter les mots-clés aux paramètres de la requête
            int paramIndex = 1;
            for (String keyword : keywords) {
                String likePattern = "%" + keyword + "%";
                ps.setString(paramIndex++, likePattern);
                ps.setString(paramIndex++, likePattern);
            }

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                GigModel gig = new GigModel(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getDouble("price"),
                        rs.getTimestamp("delivery_time").toLocalDateTime(),
                        rs.getString("image"),
                        rs.getString("status"));
                gigs.add(gig);
            }
        }

        return gigs;
    }

    public List<GigModel> getGigsByArticle(Long articleId) throws SQLException {
        List<GigModel> gigs = new ArrayList<>();

        String query = """
                    SELECT g.id, g.title, g.description, g.price, g.delivery_time, g.image, g.status
                    FROM gig g
                    WHERE g.article_id = ?
                """;

        try (Connection conn = MyDataBase.getConnection();
                PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setLong(1, articleId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                GigModel gig = new GigModel(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getDouble("price"),
                        rs.getTimestamp("delivery_time").toLocalDateTime(),
                        rs.getString("image"),
                        rs.getString("status"));
                gigs.add(gig);
            }
        }

        return gigs;
    }

}
