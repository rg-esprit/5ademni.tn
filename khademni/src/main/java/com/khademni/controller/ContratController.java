package com.khademni.controller;

import com.khademni.model.ContratModel;
import com.khademni.utils.MyDataBase;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.*;
import java.time.LocalDate;

public class ContratController {

    @FXML
    private TextField idClientField;

    @FXML
    private TextField idFreelancerField;

    @FXML
    private TextArea descriptionField;

    @FXML
    private DatePicker dateContratPicker;

    @FXML
    private void ajouterContrat() {

        try {
            int idClient = Integer.parseInt(idClientField.getText());
            int idFreelancer = Integer.parseInt(idFreelancerField.getText());
            String description = descriptionField.getText();
            LocalDate date = dateContratPicker.getValue();

            if (description == null || description.isEmpty()) {
                throw new Exception("Description vide");
            }
            if (date == null) {
                throw new Exception("Date vide");
            }

            // Database insertion
            String query = "INSERT INTO contrats (client_id, freelancer_id, date_contrat, description) VALUES (?, ?, ?, ?)";

            try (Connection conn = MyDataBase.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {

                stmt.setInt(1, idClient);
                stmt.setInt(2, idFreelancer);
                stmt.setDate(3, Date.valueOf(date));
                stmt.setString(4, description);

                int rowsAffected = stmt.executeUpdate();

                if (rowsAffected > 0) {
                    // Retrieve generated ID
                    int generatedId = -1;
                    try (ResultSet rs = stmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            generatedId = rs.getInt(1);
                        }
                    }

                    ContratModel contrat = new ContratModel(
                            generatedId,
                            idClient,
                            idFreelancer,
                            description,
                            date);

                    System.out.println("Contrat ajouté avec succès. ID: " + contrat.getIdContrat());

                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Succès");
                    alert.setHeaderText(null);
                    alert.setContentText("Contrat ajouté avec succès dans la base de données !");
                    alert.showAndWait();

                    clearFields();
                } else {
                    throw new SQLException("Échec de la création du contrat, aucune ligne affectée.");
                }
            }

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur de format", "Les IDs doivent être des nombres entiers !");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur Base de Données", "Erreur SQL : " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Vérifiez les champs : " + e.getMessage());
        }
    }

    private void clearFields() {
        idClientField.clear();
        idFreelancerField.clear();
        descriptionField.clear();
        dateContratPicker.setValue(null);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
