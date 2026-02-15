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

        loadCategories();

        categoryTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                nameField.setText(newSel.getName());
                descField.setText(newSel.getDescription());
                activeCheck.setSelected(newSel.isActive());
            }
        });
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
            showError("Sélectionnez une catégorie.");
            return;
        }

        try {
            String sql = "DELETE FROM category WHERE id=?";
            PreparedStatement ps = MyDataBase.getConnection().prepareStatement(sql);
            ps.setInt(1, selected.getId());

            ps.executeUpdate();

            loadCategories();
            clearForm();

        } catch (SQLException e) {
            showError("Erreur suppression: " + e.getMessage());
        }
    }

    // 🔹 Validation
    private boolean validateInput() {
        String name = nameField.getText().trim();
        String desc = descField.getText().trim();

        if (name.isEmpty()) {
            showError("Nom obligatoire.");
            return false;
        }

        if (name.length() < 3) {
            showError("Nom minimum 3 caractères.");
            return false;
        }

        if (desc.length() > 255) {
            showError("Description trop longue.");
            return false;
        }

        clearError();
        return true;
    }

    private void clearForm() {
        nameField.clear();
        descField.clear();
        activeCheck.setSelected(false);
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private void clearError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
    }
}
