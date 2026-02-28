package com.khademni.service;

import com.khademni.model.Article;
import com.khademni.model.Favori;
import com.khademni.interfaces.IFavori;
import com.khademni.utils.MyDataBase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FavoriController implements IFavori {

    @Override
    public Favori create(Favori favori) throws SQLException {
        String checkUserQuery = "SELECT COUNT(*) FROM users WHERE id = ?";
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement checkUserStmt = conn.prepareStatement(checkUserQuery)) {
            checkUserStmt.setLong(1, favori.getUserId());
            ResultSet rs = checkUserStmt.executeQuery();
            if (rs.next() && rs.getInt(1) == 0) {
                throw new SQLException("L'utilisateur avec l'ID " + favori.getUserId() + " n'existe pas.");
            }
        }

        String sql = "INSERT INTO favori (user_id, article_id) VALUES (?, ?)";
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, favori.getUserId());
            stmt.setLong(2, favori.getArticle().getId());
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    favori.setId(rs.getLong(1));
                }
            }
        }
        return favori;
    }
public Favori findByUserAndArticle(long userId, long articleId) throws SQLException {
    String query = "SELECT * FROM favori WHERE user_id = ? AND article_id = ?";
    try (Connection conn = MyDataBase.getConnection();
         PreparedStatement stmt = conn.prepareStatement(query)) {
        stmt.setLong(1, userId);
        stmt.setLong(2, articleId);
        try (ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return map(rs);
            }
        }
    }
    return null;
}
    @Override
    public boolean update(Favori favori) throws SQLException {
        String sql = "UPDATE favori SET user_id = ?, article_id = ? WHERE id = ?";
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, favori.getUserId());
            stmt.setLong(2, favori.getArticleId());
            stmt.setLong(3, favori.getId());
            return stmt.executeUpdate() > 0;
        }
    }

    @Override
    public boolean delete(Long id) throws SQLException {
        String sql = "DELETE FROM favori WHERE id = ?";
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    @Override
    public Favori findById(Long id) throws SQLException {
        String sql = "SELECT * FROM favori WHERE id = ?";
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
        }
        return null;
    }

    @Override
    public List<Favori> findAll() throws SQLException {
        String sql = "SELECT * FROM favori ORDER BY id DESC";
        List<Favori> favoris = new ArrayList<>();
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                favoris.add(map(rs));
            }
        }
        return favoris;
    }

    public List<Favori> findByUserId(long userId) throws SQLException {
    String query = "SELECT * FROM favori WHERE user_id = ?";
    List<Favori> favoris = new ArrayList<>();
    try (Connection conn = MyDataBase.getConnection();
         PreparedStatement stmt = conn.prepareStatement(query)) {
        stmt.setLong(1, userId);
        try (ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Favori favori = new Favori();
                favori.setId(rs.getLong("id"));
                favori.setUserId(rs.getInt("user_id"));
                favori.setArticle(new Article());
                favori.getArticle().setId(rs.getLong("article_id"));
                favori.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                favoris.add(favori);
            }
        }
    }
    return favoris;
}

    public int countByArticle(Long articleId) throws SQLException {
        String query = "SELECT COUNT(*) FROM favori WHERE article_id = ?";
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

    public boolean isFavori(Favori favori) throws SQLException {
        String query = "SELECT COUNT(*) FROM favori WHERE user_id = ? AND article_id = ?";
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setLong(1, favori.getUserId());
            stmt.setLong(2, favori.getArticle().getId());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    private Favori map(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        Integer userId = rs.getInt("user_id");
        Article article = new Article();
        article.setId(rs.getLong("article_id"));
        Timestamp timestamp = rs.getTimestamp("created_at");
        LocalDateTime createdAt = timestamp != null ? timestamp.toLocalDateTime() : null;
        return new Favori(id, userId, article, createdAt);
    }
}