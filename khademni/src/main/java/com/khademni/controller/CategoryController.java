package com.khademni.controller;

import com.khademni.model.CategoryModel;
import com.khademni.utils.MyDataBase;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.Region;

import java.io.IOException;
import java.sql.*;

public class CategoryController {

    @FXML private TableView<CategoryModel> categoryTable;
    @FXML private TableColumn<CategoryModel, Integer> idCol;
    @FXML private TableColumn<CategoryModel, String> nameCol;
    @FXML private TableColumn<CategoryModel, String> descCol;
    @FXML private TableColumn<CategoryModel, Boolean> activeCol;

    @FXML private TextField nameField;
    @FXML private TextArea descField;
    @FXML private CheckBox activeCheck;
    @FXML private Label errorLabel;
    @FXML private Label nameErrorLabel;
    @FXML private Label descErrorLabel;

    private ObservableList<CategoryModel> categoryList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {

        idCol.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().getId()).asObject());
        nameCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getName()));
        descCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getDescription()));
        activeCol.setCellValueFactory(data -> new javafx.beans.property.SimpleBooleanProperty(data.getValue().isActive()));

        // DEBUG : Vérifier que errorLabel n'est pas null
        if (errorLabel == null) {
            System.err.println("ERREUR : errorLabel est NULL !");
        } else {
            System.out.println("OK : errorLabel est initialisé correctement");
        }

        // Contrôles de saisie en temps réel
        setupInputValidation();

        loadCategories();

        categoryTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                nameField.setText(newSel.getName());
                descField.setText(newSel.getDescription());
                activeCheck.setSelected(newSel.isActive());
            }
        });
    }

    // 🔹 Configuration de la validation en temps réel
    private void setupInputValidation() {
        // Validation en temps réel pour le nom
        nameField.textProperty().addListener((observable, oldValue, newValue) -> {
            // Limiter à 100 caractères
            if (newValue != null && newValue.length() > 100) {
                nameField.setText(oldValue);
                return;
            }
            // Supprimer les caractères non autorisés
            if (newValue != null && !newValue.matches("^[a-zA-ZÀ-ÿ0-9\\s&-]*$")) {
                nameField.setText(oldValue);
                return;
            }

            // Valider et afficher l'erreur si nécessaire
            validateNameField();
        });

        // Validation en temps réel pour la description
        descField.textProperty().addListener((observable, oldValue, newValue) -> {
            // Limiter à 500 caractères
            if (newValue != null && newValue.length() > 500) {
                descField.setText(oldValue);
                return;
            }

            // Valider et afficher l'erreur si nécessaire
            validateDescField();
        });

        // Afficher le compteur de caractères
        nameField.setPromptText("Nom de la catégorie (3-100 caractères)");
        descField.setPromptText("Description (10-500 caractères)");
    }

    // 🔹 Valider le champ nom
    private void validateNameField() {
        String name = nameField.getText().trim();

        if (name.isEmpty()) {
            showFieldError(nameErrorLabel, "• Le nom est obligatoire");
        } else if (name.length() < 3) {
            showFieldError(nameErrorLabel, "• Le nom doit contenir au moins 3 caractères");
        } else if (!name.matches("^[a-zA-ZÀ-ÿ0-9\\s&-]+$")) {
            showFieldError(nameErrorLabel, "• Le nom contient des caractères non autorisés");
        } else {
            // Effacer l'erreur si le champ est valide
            nameErrorLabel.setText("");
            nameErrorLabel.setVisible(false);
            nameErrorLabel.setManaged(false);
        }
    }

    // 🔹 Valider le champ description
    private void validateDescField() {
        String desc = descField.getText().trim();

        if (desc.isEmpty()) {
            showFieldError(descErrorLabel, "• La description est obligatoire");
        } else if (desc.length() < 10) {
            showFieldError(descErrorLabel, "• La description doit contenir au moins 10 caractères");
        } else if (desc.length() > 500) {
            showFieldError(descErrorLabel, "• La description ne doit pas dépasser 500 caractères");
        } else {
            // Effacer l'erreur si le champ est valide
            descErrorLabel.setText("");
            descErrorLabel.setVisible(false);
            descErrorLabel.setManaged(false);
        }
    }

    // 🔹 READ
    private void loadCategories() {
        categoryList.clear();

        try {
            String sql = "SELECT * FROM category";
            PreparedStatement ps = MyDataBase.getConnection().prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                CategoryModel cat = new CategoryModel(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getBoolean("is_active")
                );
                categoryList.add(cat);
            }

            categoryTable.setItems(categoryList);

        } catch (SQLException e) {
            showError("Erreur chargement: " + e.getMessage());
        }
    }

    // 🔹 CREATE
    @FXML
    private void onAddCategory() {

        if (!validateInput()) return;

        try {
            String sql = "INSERT INTO category (name, description, is_active) VALUES (?, ?, ?)";
            PreparedStatement ps = MyDataBase.getConnection().prepareStatement(sql);

            ps.setString(1, nameField.getText());
            ps.setString(2, descField.getText());
            ps.setBoolean(3, activeCheck.isSelected());

            ps.executeUpdate();

            loadCategories();
            clearForm();
            showSuccess("✅ Catégorie ajoutée avec succès !");

        } catch (SQLException e) {
            showError("Erreur ajout: " + e.getMessage());
        }
    }

    // 🔹 UPDATE
    @FXML
    private void onUpdateCategory() {

        CategoryModel selected = categoryTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("⚠️ Veuillez sélectionner une catégorie à modifier");
            return;
        }

        if (!validateInput()) return;

        try {
            String sql = "UPDATE category SET name=?, description=?, is_active=? WHERE id=?";
            PreparedStatement ps = MyDataBase.getConnection().prepareStatement(sql);

            ps.setString(1, nameField.getText());
            ps.setString(2, descField.getText());
            ps.setBoolean(3, activeCheck.isSelected());
            ps.setInt(4, selected.getId());

            ps.executeUpdate();

            loadCategories();
            clearForm();
            showSuccess("✅ Catégorie modifiée avec succès !");

        } catch (SQLException e) {
            showError("Erreur modification: " + e.getMessage());
        }
    }

    // 🔹 DELETE
    @FXML
    private void onDeleteCategory() {

        CategoryModel selected = categoryTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("⚠️ Veuillez sélectionner une catégorie à supprimer");
            return;
        }

        // Confirmation de suppression
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation");
        confirmation.setHeaderText("Supprimer la catégorie");
        confirmation.setContentText("Voulez-vous vraiment supprimer la catégorie :\n\"" + selected.getName() + "\" ?");

        if (confirmation.showAndWait().get() != ButtonType.OK) {
            return;
        }

        try {
            String sql = "DELETE FROM category WHERE id=?";
            PreparedStatement ps = MyDataBase.getConnection().prepareStatement(sql);
            ps.setInt(1, selected.getId());

            ps.executeUpdate();

            loadCategories();
            clearForm();
            showSuccess("✅ Catégorie supprimée avec succès !");

        } catch (SQLException e) {
            showError("Erreur suppression:\n" + e.getMessage());
        }
    }

    // 🔹 Validation améliorée avec erreurs individuelles
    private boolean validateInput() {
        boolean isValid = true;

        // Effacer toutes les erreurs précédentes
        clearFieldErrors();

        // 1. Validation du Nom
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            showFieldError(nameErrorLabel, "• Le nom est obligatoire");
            isValid = false;
        } else if (name.length() < 3) {
            showFieldError(nameErrorLabel, "• Le nom doit contenir au moins 3 caractères");
            isValid = false;
        } else if (name.length() > 100) {
            showFieldError(nameErrorLabel, "• Le nom ne doit pas dépasser 100 caractères");
            isValid = false;
        } else if (!name.matches("^[a-zA-ZÀ-ÿ0-9\\s&-]+$")) {
            showFieldError(nameErrorLabel, "• Le nom contient des caractères non autorisés");
            isValid = false;
        } else {
            // Vérifier si le nom existe déjà
            CategoryModel selected = categoryTable.getSelectionModel().getSelectedItem();
            if (selected == null && nameExists(name)) {
                showFieldError(nameErrorLabel, "• Une catégorie avec ce nom existe déjà");
                isValid = false;
            } else if (selected != null && nameExistsForOther(name, selected.getId())) {
                showFieldError(nameErrorLabel, "• Une autre catégorie utilise déjà ce nom");
                isValid = false;
            }
        }

        // 2. Validation de la Description
        String desc = descField.getText().trim();
        if (desc.isEmpty()) {
            showFieldError(descErrorLabel, "• La description est obligatoire");
            isValid = false;
        } else if (desc.length() < 10) {
            showFieldError(descErrorLabel, "• La description doit contenir au moins 10 caractères");
            isValid = false;
        } else if (desc.length() > 500) {
            showFieldError(descErrorLabel, "• La description ne doit pas dépasser 500 caractères");
            isValid = false;
        }

        return isValid;
    }

    // 🔹 Afficher une erreur sous un champ spécifique
    private void showFieldError(Label errorLabel, String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    // 🔹 Effacer toutes les erreurs de champs
    private void clearFieldErrors() {
        nameErrorLabel.setText("");
        nameErrorLabel.setVisible(false);
        nameErrorLabel.setManaged(false);

        descErrorLabel.setText("");
        descErrorLabel.setVisible(false);
        descErrorLabel.setManaged(false);
    }

    // 🔹 Vérifier si le nom existe déjà
    private boolean nameExists(String name) {
        try {
            String sql = "SELECT COUNT(*) FROM category WHERE LOWER(name) = LOWER(?)";
            PreparedStatement ps = MyDataBase.getConnection().prepareStatement(sql);
            ps.setString(1, name.trim());
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // 🔹 Vérifier si le nom existe pour une autre catégorie
    private boolean nameExistsForOther(String name, int currentId) {
        try {
            String sql = "SELECT COUNT(*) FROM category WHERE LOWER(name) = LOWER(?) AND id != ?";
            PreparedStatement ps = MyDataBase.getConnection().prepareStatement(sql);
            ps.setString(1, name.trim());
            ps.setInt(2, currentId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void clearForm() {
        nameField.clear();
        descField.clear();
        activeCheck.setSelected(false);
        clearFieldErrors();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setWrapText(true);
        errorLabel.setMaxWidth(Double.MAX_VALUE);
        errorLabel.setMinHeight(60);
        errorLabel.setStyle(
            "-fx-text-fill: #d32f2f; " +
            "-fx-font-weight: bold; " +
            "-fx-font-size: 13px; " +
            "-fx-line-spacing: 6px; " +
            "-fx-background-color: #ffebee; " +
            "-fx-padding: 16px 20px; " +
            "-fx-background-radius: 8px; " +
            "-fx-border-color: #ef5350; " +
            "-fx-border-width: 2px; " +
            "-fx-border-radius: 8px;"
        );
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);

        System.out.println("DEBUG - Affichage erreur: " + message);
    }

    private void clearError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    // 🔹 Afficher un message de succès
    private void showSuccess(String message) {
        errorLabel.setText(message);
        errorLabel.setWrapText(true);
        errorLabel.setMaxWidth(Double.MAX_VALUE);
        errorLabel.setMinHeight(70);
        errorLabel.setPrefHeight(Region.USE_COMPUTED_SIZE);
        errorLabel.setStyle(
            "-fx-text-fill: #2e7d32; " +
            "-fx-font-weight: bold; " +
            "-fx-font-size: 14px; " +
            "-fx-line-spacing: 8px; " +
            "-fx-background-color: #e8f5e9; " +
            "-fx-padding: 20px 24px; " +
            "-fx-background-radius: 8px; " +
            "-fx-border-color: #66bb6a; " +
            "-fx-border-width: 2px; " +
            "-fx-border-radius: 8px;"
        );
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);

        // Masquer le message après 3 secondes
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                javafx.application.Platform.runLater(this::clearError);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    // 🔹 REFRESH - Recharger les données
    @FXML
    private void onRefresh() {
        loadCategories();
        clearForm();
        clearError();
        showSuccess("🔄 Données rechargées avec succès !");
    }

    // 🔹 GO BACK - Retour à la page profile
    @FXML
    private void onGoBack() {
        try {
            com.khademni.App.setRoot("profile");
        } catch (IOException e) {
            showError("Erreur lors du retour au profile:\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    // 🔹 EXIT - Quitter l'application
    @FXML
    private void onExit() {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation");
        confirmation.setHeaderText("Quitter l'application");
        confirmation.setContentText("Voulez-vous vraiment quitter l'application ?");

        if (confirmation.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            javafx.application.Platform.exit();
            System.exit(0);
        }
    }
}
