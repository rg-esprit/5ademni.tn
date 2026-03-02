package com.khademni.dao.impl;

import com.khademni.dao.ClientDAO;
import com.khademni.model.Client;
import com.khademni.utils.MyDataBase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ClientDAOImpl implements ClientDAO {
    @Override
    public List<Client> findAll() throws SQLException {
        List<Client> clients = new ArrayList<>();
        String query = "SELECT DISTINCT u.id, u.unique_id, u.first_name, u.last_name " +
                "FROM users u INNER JOIN demandes d ON u.id = d.user_id " +
                "WHERE u.current_mode = 'CLIENT' AND u.unique_id BETWEEN 1000 AND 9999 AND d.statut != 'ANNULE'";
        try (Connection conn = MyDataBase.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(query);
                ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                clients.add(new Client(rs.getInt("id"), rs.getInt("unique_id"), rs.getString("first_name"),
                        rs.getString("last_name")));
            }
        }
        return clients;
    }
}
