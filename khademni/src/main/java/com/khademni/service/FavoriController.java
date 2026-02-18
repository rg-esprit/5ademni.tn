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

	private final Connection connection;

	public FavoriController() {
		try {
        this.connection = MyDataBase.getConnection();
    } catch (SQLException e) {
        throw new RuntimeException("Failed to establish database connection", e);
    }
	}

	@Override
	public Favori create(Favori favori) throws SQLException {
		String sql = "INSERT INTO favori (user_id, article_id) VALUES (?, ?)";

		try (PreparedStatement stmt = connection.prepareStatement(
				sql, Statement.RETURN_GENERATED_KEYS)) {

			stmt.setLong(1, favori.getUserId());
			stmt.setLong(2, favori.getArticleId());
			stmt.executeUpdate();

			try (ResultSet rs = stmt.getGeneratedKeys()) {
				if (rs.next()) {
					favori.setId(rs.getLong(1));
				}
			}
		}
		return favori;
	}

	@Override
	public boolean update(Favori favori) throws SQLException {
		String sql = "UPDATE favori SET user_id = ?, article_id = ? WHERE id = ?";

		try (PreparedStatement stmt = connection.prepareStatement(sql)) {
			stmt.setLong(1, favori.getUserId());
			stmt.setLong(2, favori.getArticleId());
			stmt.setLong(3, favori.getId());
			return stmt.executeUpdate() > 0;
		}
	}

	@Override
	public boolean delete(Long id) throws SQLException {
		String sql = "DELETE FROM favori WHERE id = ?";

		try (PreparedStatement stmt = connection.prepareStatement(sql)) {
			stmt.setLong(1, id);
			return stmt.executeUpdate() > 0;
		}
	}

	@Override
	public Favori findById(Long id) throws SQLException {
		String sql = "SELECT * FROM favori WHERE id = ?";

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

	@Override
	public List<Favori> findAll() throws SQLException {
		String sql = "SELECT * FROM favori ORDER BY id DESC";
		List<Favori> favoris = new ArrayList<>();

		try (PreparedStatement stmt = connection.prepareStatement(sql);
			 ResultSet rs = stmt.executeQuery()) {

			while (rs.next()) {
				favoris.add(map(rs));
			}
		}
		return favoris;
	}

	private Favori map(ResultSet rs) throws SQLException {

    Long id = rs.getLong("id");
    Long userId = rs.getLong("user_id");

    Article article = new Article();
    article.setId(rs.getLong("article_id"));

    Timestamp timestamp = rs.getTimestamp("created_at");
    LocalDateTime createdAt = null;

    if (timestamp != null) {
        createdAt = timestamp.toLocalDateTime();
    }

    return new Favori(id, userId, article, createdAt);
}


public List<Favori> findByUserId(Long userId) throws SQLException {
    String query = "SELECT * FROM favori WHERE user_id = ?";
    PreparedStatement statement = MyDataBase.getConnection().prepareStatement(query);
    statement.setLong(1, userId);

    ResultSet resultSet = statement.executeQuery();
    List<Favori> favoris = new ArrayList<>();

    while (resultSet.next()) {
        Favori favori = new Favori();
        favori.setId(resultSet.getLong("id"));
        favori.setUserId(resultSet.getLong("user_id"));
        favori.setArticle(new Article());
        favori.getArticle().setId(resultSet.getLong("article_id"));
        favori.setCreatedAt(resultSet.getTimestamp("created_at").toLocalDateTime());
        favoris.add(favori);
    }

    return favoris;
}


}
