package com.khademni.dao;

import java.sql.SQLException;

public interface BaseDAO<T> {
    T findById(int id) throws SQLException;

    void save(T item) throws SQLException;

    void update(T item) throws SQLException;

    void delete(int id) throws SQLException;
}
