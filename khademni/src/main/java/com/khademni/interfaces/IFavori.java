package com.khademni.interfaces;
import com.khademni.model.Favori;

import java.sql.SQLException;
import java.util.List;
public interface IFavori {
    Favori create(Favori favori) throws SQLException;

    boolean update(Favori favori) throws SQLException;

    boolean delete(Long id) throws SQLException;

    Favori findById(Long id) throws SQLException;

    List<Favori> findAll() throws SQLException;
}
