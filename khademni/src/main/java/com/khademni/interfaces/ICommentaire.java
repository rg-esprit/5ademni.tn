package com.khademni.interfaces;

import com.khademni.model.Commentaire;

import java.sql.SQLException;
import java.util.List;

public interface ICommentaire {

    Commentaire create(Commentaire commentaire) throws SQLException;

    boolean update(Commentaire commentaire) throws SQLException;

    boolean delete(Long id) throws SQLException;

    Commentaire findById(Long id) throws SQLException;

    List<Commentaire> findAll() throws SQLException;
}
