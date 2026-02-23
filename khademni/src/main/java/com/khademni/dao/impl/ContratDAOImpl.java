package com.khademni.dao.impl;

import com.khademni.dao.ContratDAO;
import com.khademni.model.ContratModel;
import com.khademni.utils.MyDataBase;
import java.sql.*;

public class ContratDAOImpl implements ContratDAO {
    @Override
    public void save(ContratModel contrat, Connection conn) throws SQLException {
        String query = "INSERT INTO contrats (client_id, freelancer_id, titre, description, prix, date_contrat, statut) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, contrat.getIdClient());
            pstmt.setInt(2, contrat.getIdFreelancer());
            pstmt.setString(3, contrat.getTitre());
            pstmt.setString(4, contrat.getDescription());
            pstmt.setDouble(5, contrat.getPrix());
            pstmt.setDate(6, java.sql.Date.valueOf(contrat.getDateContrat()));
            pstmt.setString(7, contrat.getStatut());
            pstmt.executeUpdate();
        }
    }

    @Override
    public void update(ContratModel contrat, Connection conn) throws SQLException {
        String query = "UPDATE contrats SET client_id = ?, freelancer_id = ?, titre = ?, description = ?, prix = ?, date_contrat = ?, statut = ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, contrat.getIdClient());
            pstmt.setInt(2, contrat.getIdFreelancer());
            pstmt.setString(3, contrat.getTitre());
            pstmt.setString(4, contrat.getDescription());
            pstmt.setDouble(5, contrat.getPrix());
            pstmt.setDate(6, java.sql.Date.valueOf(contrat.getDateContrat()));
            pstmt.setString(7, contrat.getStatut());
            pstmt.setInt(8, contrat.getIdContrat());
            pstmt.executeUpdate();
        }
    }
}
