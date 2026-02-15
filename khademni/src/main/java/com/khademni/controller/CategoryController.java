package com.khademni.controller;

import com.khademni.model.CategoryModel;
import com.khademni.utils.MyDataBase;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

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

    private ObservableList<CategoryModel> categoryList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {

        idCol.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().getId()).asObject());
        nameCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getName()));
        descCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getDescription()));
        activeCol.setCellValueFactory(data -> new javafx.beans.property.SimpleBooleanProperty(data.getValue().isActive()));

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
        // Limiter la longueur du nom à 100 caractères
        nameField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && newValue.length() > 100) {
                nameField.setText(oldValue);
            }
            // Supprimer les caractères non autorisés
            if (newValue != null && !newValue.matches("^[a-zA-ZÀ-ÿ0-9\\s&-]*$")) {
                nameField.setText(oldValue);
            }
        });

        // Limiter la longueur de la description à 500 caractères
        descField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && newValue.length() > 500) {
                descField.setText(oldValue);
            }
        });

        // Afficher le compteur de caractères (optionnel)
        nameField.setPromptText("Nom de la catégorie (3-100 caractères)");
        descField.setPromptText("Description (10-500 caractères)");
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

        } catch (SQLException e) {
            showError("Erreur ajout: " + e.getMessage());
        }
    }

    // 🔹 UPDATE
    @FXML
    private void onUpdateCategory() {

        CategoryModel selected = categoryTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Sélectionnez une catégorie.");
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

        } catch (SQLException e) {
            showError("Erreur modification: " + e.getMessage());
        }
    }

    // 🔹 DELETE
    @FXML
    private void onDeleteCategory() {

        CategoryModel selected = categoryTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("⚠ Sélectionnez une catégorie à supprimer.");
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
            showSuccess("✓ Catégorie supprimée avec succès !");

        } catch (SQLException e) {
            showError("Erreur suppression:\n" + e.getMessage());
        }
    }

    // 🔹 Validation améliorée
    private boolean validateInput() {
        StringBuilder errors = new StringBuilder();

        // 1. Validation du Nom
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            errors.append("• Le nom est obligatoire.\n");
        } else if (name.length() < 3) {
            errors.append("• Le nom doit contenir au moins 3 caractères.\n");
        } else if (name.length() > 100) {
            errors.append("• Le nom ne doit pas dépasser 100 caractères.\n");
        } else if (!name.matches("^[a-zA-ZÀ-ÿ0-9\\s&-]+$")) {
            errors.append("• Le nom contient des caractères non autorisés.\n");
        }

        // 2. Validation de la Description
        String desc = descField.getText().trim();
        if (desc.isEmpty()) {
            errors.append("• La description est obligatoire.\n");
        } else if (desc.length() < 10) {
            errors.append("• La description doit contenir au moins 10 caractères.\n");
        } else if (desc.length() > 500) {
            errors.append("• La description ne doit pas dépasser 500 caractères.\n");
        }

        // 3. Vérifier si le nom existe déjà (pour l'ajout uniquement)
        if (errors.length() == 0 && !name.isEmpty()) {
            CategoryModel selected = categoryTable.getSelectionModel().getSelectedItem();
            if (selected == null && nameExists(name)) {
                errors.append("• Une catégorie avec ce nom existe déjà.\n");
            } else if (selected != null && nameExistsForOther(name, selected.getId())) {
                errors.append("• Une autre catégorie utilise déjà ce nom.\n");
            }
        }

        // Afficher les erreurs s'il y en a
        if (errors.length() > 0) {
            showError("Erreurs de validation :\n" + errors.toString());
            return false;
        }

        clearError();
        return true;
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
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        errorLabel.setVisible(true);
    }

    private void clearError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
    }

    // 🔹 Afficher un message de succès
    private void showSuccess(String message) {
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
        errorLabel.setVisible(true);

        // Masquer le message après 3 secondes
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                javafx.application.Platform.runLater(() -> clearError());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
