package com.khademni.controller;

import com.khademni.App;
import com.khademni.model.ContratModel;
import com.khademni.utils.MyDataBase;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;

public class ContratFormController {

    @FXML
    private Label formTitle;

    @FXML
    private TextField titreField;

    @FXML
    private TextField idFreelancerField;

    @FXML
    private TextField prixField;

    @FXML
    private TextArea descriptionField;

    @FXML
    private DatePicker dateContratPicker;

    @FXML
    private Label errorLabel;

    private ContratModel contratToEdit;

    public void setContratToEdit(ContratModel contrat) {
        this.contratToEdit = contrat;
        if (contrat != null) {
            if (formTitle != null)
                formTitle.setText("Modifier le Contrat");
            titreField.setText(contrat.getTitre());
            idFreelancerField.setText(String.valueOf(contrat.getIdFreelancer()));
            prixField.setText(String.valueOf(contrat.getPrix()));
            descriptionField.setText(contrat.getDescription());
            dateContratPicker.setValue(contrat.getDateContrat());
        }
    }

    @FXML
    private void saveContrat(ActionEvent event) {
        String titre = titreField.getText().trim();
        String freelancerIdStr = idFreelancerField.getText().trim();
        String prixStr = prixField.getText().trim();
        String description = descriptionField.getText().trim();
        LocalDate date = dateContratPicker.getValue();

        // Clear previous errors
        showError("");

        // Robust Validation
        if (titre.isEmpty() || titre.length() < 5) {
            showError("Le titre doit contenir au moins 5 caractères.");
            return;
        }

        int fId;
        try {
            fId = Integer.parseInt(freelancerIdStr);
            if (fId <= 0)
                throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showError("L'ID Freelancer doit être un nombre positif.");
            return;
        }

        double prix;
        try {
            prix = Double.parseDouble(prixStr);
            if (prix <= 0)
                throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showError("Le prix doit être un nombre positif.");
            return;
        }

        if (description.isEmpty() || description.length() < 10) {
            showError("La description doit contenir au moins 10 caractères.");
            return;
        }

        if (date == null) {
            showError("La date du contrat est requise.");
            return;
        }

        try {
            int clientId = App.getCurrentUser() != null ? App.getCurrentUser().getId() : 1;
            Connection conn = MyDataBase.getConnection();
            String query;

            if (contratToEdit == null) {
                query = "INSERT INTO contrats (client_id, freelancer_id, titre, description, prix, date_contrat, statut) VALUES (?, ?, ?, ?, ?, ?, ?)";
            } else {
                query = "UPDATE contrats SET client_id=?, freelancer_id=?, titre=?, description=?, prix=?, date_contrat=? WHERE id=?";
            }

            try (PreparedStatement stmt = (contratToEdit == null)
                    ? conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)
                    : conn.prepareStatement(query)) {

                stmt.setInt(1, clientId);
                stmt.setInt(2, fId);
                stmt.setString(3, titre);
                stmt.setString(4, description);
                stmt.setDouble(5, prix);
                stmt.setDate(6, java.sql.Date.valueOf(date));

                if (contratToEdit == null) {
                    stmt.setString(7, "EN_ATTENTE");
                } else {
                    stmt.setInt(7, contratToEdit.getIdContrat());
                }

                stmt.executeUpdate();

                String successMsg = "Contrat enregistré avec succès !";
                if (contratToEdit == null) {
                    try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            int newId = generatedKeys.getInt(1);
                            successMsg = "Contrat #" + newId + " créé avec succès !";
                        }
                    }
                }

                showAlert(Alert.AlertType.INFORMATION, "Succès", successMsg);
                App.setRoot("contrat");
            }
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            showError("Erreur base de données: " + e.getMessage());
        }
    }

    @FXML
    private void cancel(ActionEvent event) throws IOException {
        App.setRoot("contrat");
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showError(String message) {
        if (errorLabel == null)
            return;
        if (message == null || message.isEmpty()) {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        } else {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }
}
