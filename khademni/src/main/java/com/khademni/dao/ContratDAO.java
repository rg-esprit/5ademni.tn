package com.khademni.dao;

import com.khademni.model.ContratModel;
import java.sql.SQLException;

public interface ContratDAO {
    void save(ContratModel contrat, java.sql.Connection conn) throws java.sql.SQLException;

    void update(ContratModel contrat, java.sql.Connection conn) throws java.sql.SQLException;
}
