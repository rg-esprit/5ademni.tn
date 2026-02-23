package com.khademni.controller;

import com.khademni.App;
import com.khademni.model.OffreModel;
import com.khademni.utils.MyDataBase;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class OffreFormController {

    public static String creationType = "OFFRE"; // Set by OffreController before navigation
    public static boolean editMode = false;
    public static OffreModel selectedOffre = null;

    @FXML
    private Label formTitle;
    @FXML
    private TextField userIdField;
    @FXML
    private DatePicker dateField;
    @FXML
    private DatePicker dateLimiteField;
    @FXML
    private TextField titreField;
    @FXML
    private TextArea descriptionField;
    @FXML
    private TextField prixField;

    @FXML
    private Label errorLabel;

    @FXML
    private VBox userIdContainer;
    @FXML
    private VBox dateCreationContainer;
    @FXML
    private VBox dateLimiteContainer;
    @FXML
    private VBox titreContainer;
    @FXML
    private VBox descriptionContainer;
    @FXML
    private VBox prixContainer;

    @FXML
    public void initialize() {
        if (editMode && selectedOffre != null) {
            formTitle.setText("Modifier " + (selectedOffre.getType().equals("OFFRE") ? "l'Offre" : "la Demande"));
            userIdField.setText(String.valueOf(selectedOffre.getUserUniqueId()));
            userIdField.setEditable(false);
            dateField.setValue(selectedOffre.getDateCreation().toLocalDate());
            if (selectedOffre.getDateLimite() != null) {
                dateLimiteField.setValue(selectedOffre.getDateLimite().toLocalDate());
            }
            titreField.setText(selectedOffre.getTitre());
            descriptionField.setText(selectedOffre.getDescription());
            prixField.setText(selectedOffre.getPrix() == 0 ? "----" : String.valueOf(selectedOffre.getPrix()));
        } else {
            if ("DEMANDE".equals(creationType)) {
                formTitle.setText("Créer une Demande d'Offre");
            } else {
                formTitle.setText("Créer un Offre de Service");
            }
            dateField.setValue(LocalDate.now());
            dateLimiteField.setValue(LocalDate.now().plusDays(7));
            userIdField.setEditable(true);

            // Auto-fill user ID if logged in
            if (App.getCurrentUser() != null && App.getCurrentUser().getUniqueId() >= 1000) {
                userIdField.setText(String.valueOf(App.getCurrentUser().getUniqueId()));
                userIdField.setEditable(false);
            }
        }
    }

    @FXML
    private void handleSave() {
        String userIdText = userIdField.getText().trim();
        LocalDate creationDate = dateField.getValue();
        LocalDate deadlineDate = dateLimiteField.getValue();
        String titre = titreField.getText().trim();
        String description = descriptionField.getText().trim();
        String prixText = prixField.getText().trim();

        // Clear previous errors
        showError("");

        // Control de saisie (Validation)
        if (userIdText.isEmpty()) {
            showError("L'ID Utilisateur est requis.");
            return;
        }

        int userId;
        try {
            userId = Integer.parseInt(userIdText);
        } catch (NumberFormatException e) {
            showError("L'ID Utilisateur doit être un nombre valide.");
            return;
        }

        // Resolve technical ID from unique_id
        int technicalUserId = userId;
        try (Connection conn = MyDataBase.getConnection();
                PreparedStatement ps = conn.prepareStatement("SELECT id FROM users WHERE unique_id = ?")) {
            ps.setInt(1, userId);
            java.sql.ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                technicalUserId = rs.getInt("id");
            } else {
                showError("Utilisateur introuvable avec l'ID : " + userId);
                return;
            }
        } catch (SQLException e) {
            showError("Erreur de recherche utilisateur: " + e.getMessage());
            return;
        }

        if (creationDate == null) {
            showError("La date de création est requise.");
            return;
        }

        if (titre.isEmpty() || titre.length() < 5) {
            showError("Le titre doit contenir au moins 5 caractères.");
            return;
        }

        if (description.isEmpty() || description.length() < 10) {
            showError("La description doit contenir au moins 10 caractères.");
            return;
        }

        double prix;
        if (prixText.isEmpty() || prixText.equalsIgnoreCase("----")) {
            prix = 0;
        } else {
            try {
                prix = Double.parseDouble(prixText.replace(",", "."));
                if (prix < 0) {
                    showError("Le prix ne peut pas être négatif.");
                    return;
                }
            } catch (NumberFormatException e) {
                showError("Le prix doit être un nombre valide ou ---- pour ne pas afficher.");
                return;
            }
        }

        if (editMode && selectedOffre != null) {
            updateInDatabase(selectedOffre.getId(), technicalUserId, creationDate.atStartOfDay(),
                    deadlineDate, titre, description, prix);
        } else {
            saveToDatabase(technicalUserId, creationDate.atStartOfDay(), deadlineDate, titre, description, prix);
        }
    }

    private void updateInDatabase(int id, int userId, LocalDateTime dateCreation, LocalDate deadlineDate,
            String titre, String description, double prix) {
        String table = "DEMANDE".equals(creationType) ? "demandes" : "offres";
        String query = "UPDATE " + table
                + " SET user_id = ?, titre = ?, description = ?, prix = ?, date_creation = ?, date_limite = ? WHERE id = ?";
        try (Connection conn = MyDataBase.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, userId);
            pstmt.setString(2, titre);
            pstmt.setString(3, description);
            pstmt.setDouble(4, prix);
            pstmt.setTimestamp(5, java.sql.Timestamp.valueOf(dateCreation));
            pstmt.setTimestamp(6,
                    deadlineDate != null ? java.sql.Timestamp.valueOf(deadlineDate.atTime(23, 59, 59)) : null);
            pstmt.setInt(7, id);

            pstmt.executeUpdate();

            showAlert(Alert.AlertType.INFORMATION, "Succès", "Mise à jour effectuée avec succès.");
            resetEditMode();
            goToOffres();
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            showError("Impossible de mettre à jour : " + e.getMessage());
        }
    }

    private void saveToDatabase(int userId, LocalDateTime dateCreation, LocalDate deadlineDate,
            String titre, String description, double prix) {
        String table = "DEMANDE".equals(creationType) ? "demandes" : "offres";
        String query = "INSERT INTO " + table
                + " (user_id, titre, description, prix, date_creation, date_limite, statut) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = MyDataBase.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(query, java.sql.Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, userId);
            pstmt.setString(2, titre);
            pstmt.setString(3, description);
            pstmt.setDouble(4, prix);
            pstmt.setTimestamp(5, java.sql.Timestamp.valueOf(dateCreation));
            pstmt.setTimestamp(6,
                    deadlineDate != null ? java.sql.Timestamp.valueOf(deadlineDate.atTime(23, 59, 59)) : null);
            pstmt.setString(7, "ACTIF");

            pstmt.executeUpdate();

            String successMsg = "Votre " + creationType.toLowerCase() + " a été créé avec succès.";
            try (java.sql.ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int newId = generatedKeys.getInt(1);
                    successMsg = (creationType.equals("OFFRE") ? "Offre #" : "Demande #") + newId
                            + " créée avec succès.";
                }
            }

            showAlert(Alert.AlertType.INFORMATION, "Succès", successMsg);
            goToOffres();
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            showError("Impossible d'enregistrer : " + e.getMessage());
        }
    }

    private void resetEditMode() {
        editMode = false;
        selectedOffre = null;
    }

    @FXML
    private void goToOffres() throws IOException {
        String target;
        if (editMode && selectedOffre != null) {
            target = selectedOffre.getType().equals("OFFRE") ? "freelancer_space" : "client_space";
        } else if ("DEMANDE".equals(creationType)) {
            target = "client_space";
        } else if ("OFFRE".equals(creationType)) {
            target = "freelancer_space";
        } else {
            target = "offre";
        }
        resetEditMode();
        App.setRoot(target);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
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
