package com.khademni.dao.impl;

import com.khademni.dao.FreelancerDAO;
import com.khademni.model.Freelancer;
import com.khademni.utils.MyDataBase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FreelancerDAOImpl implements FreelancerDAO {
    @Override
    public List<Freelancer> findAll() throws SQLException {
        List<Freelancer> freelancers = new ArrayList<>();
        String query = "SELECT DISTINCT u.id, u.unique_id, u.first_name, u.last_name " +
                "FROM users u INNER JOIN offres o ON u.id = o.user_id " +
                "WHERE u.current_mode = 'FREELANCER' AND u.unique_id BETWEEN 1000 AND 9999 AND o.statut != 'ANNULE'";
        try (Connection conn = MyDataBase.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(query);
                ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                freelancers.add(new Freelancer(rs.getInt("id"), rs.getInt("unique_id"), rs.getString("first_name"),
                        rs.getString("last_name")));
            }
        }
        return freelancers;
    }
}
