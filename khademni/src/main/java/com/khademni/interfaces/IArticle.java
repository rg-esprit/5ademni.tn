package com.khademni.interfaces;

import com.khademni.model.Article;

import java.sql.SQLException;
import java.util.List;

public interface IArticle {

    Article create(Article article) throws SQLException;

    boolean update(Article article) throws SQLException;

    boolean delete(Long id) throws SQLException;

    Article findById(Long id) throws SQLException;

    List<Article> findAll() throws SQLException;
}
