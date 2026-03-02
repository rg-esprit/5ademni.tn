package com.khademni.dao;

import com.khademni.model.Client;
import java.sql.SQLException;
import java.util.List;

public interface ClientDAO {
    List<Client> findAll() throws SQLException;
}
