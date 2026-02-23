package com.khademni.dao;

import com.khademni.model.OffreModel;
import java.sql.SQLException;
import java.util.List;

public interface OffreDAO extends BaseDAO<OffreModel> {
    List<OffreModel> findByType(String type) throws SQLException;

    List<OffreModel> findByUserId(int userId, String type) throws SQLException;
}
