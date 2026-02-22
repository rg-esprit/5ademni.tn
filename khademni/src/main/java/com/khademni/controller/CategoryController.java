package com.khademni.controller;

import com.khademni.model.CategoryModel;
import com.khademni.utils.MyDataBase;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CategoryController {

    // Stats
    @FXML private Label totalCategoriesLabel;
    @FXML private Label activeCategoriesLabel;
    @FXML private Label inactiveCategoriesLabel;
    @FXML private Label categoryCountLabel;

    // Search & Filters
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilterCombo;

    // Display
    @FXML private FlowPane categoriesContainer;
    @FXML private VBox emptyState;

    // Messages
    @FXML private HBox messageContainer;
    @FXML private Label errorLabel;

    private final List<CategoryModel> allCategories = new ArrayList<>();

    @FXML
    public void initialize() {
        System.out.println("=== CategoryController: Starting initialization ===");

        if (statusFilterCombo != null) {
            statusFilterCombo.getItems().addAll("All Status", "Active", "Inactive");
            statusFilterCombo.setValue("All Status");
            statusFilterCombo.setOnAction(e -> applyFilters());
        }

        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        }

        loadCategories();
        updateStats();

        System.out.println("=== CategoryController: Initialization complete ===");
    }

    private void loadCategories() {
        allCategories.clear();
        System.out.println("Loading categories...");

        String query = "SELECT id, name, description, is_active FROM category ORDER BY id DESC";

        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {

            int count = 0;
            while (rs.next()) {
                CategoryModel cat = new CategoryModel(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getBoolean("is_active")
                );
                allCategories.add(cat);
                count++;
                System.out.println("  Loaded category: " + cat.getName() + " (ID: " + cat.getId() + ")");
            }

            System.out.println("Total categories loaded: " + count);
            applyFilters();

        } catch (SQLException e) {
            System.err.println("ERROR loading categories: " + e.getMessage());
            e.printStackTrace();
            showError("Error loading categories: " + e.getMessage());
        }
    }

    private void applyFilters() {
        List<CategoryModel> filtered = new ArrayList<>(allCategories);

        if (searchField != null && !searchField.getText().trim().isEmpty()) {
            String search = searchField.getText().toLowerCase();
            filtered.removeIf(cat ->
                !cat.getName().toLowerCase().contains(search) &&
                !cat.getDescription().toLowerCase().contains(search)
            );
        }

        if (statusFilterCombo != null && statusFilterCombo.getValue() != null) {
            String status = statusFilterCombo.getValue();
            if ("Active".equals(status)) {
                filtered.removeIf(cat -> !cat.isActive());
            } else if ("Inactive".equals(status)) {
                filtered.removeIf(CategoryModel::isActive);
            }
        }

        displayCategories(filtered);
        updateStats();
    }

    private void displayCategories(List<CategoryModel> categories) {
        if (categoriesContainer == null || emptyState == null) return;

        categoriesContainer.getChildren().clear();

        if (categories.isEmpty()) {
            emptyState.setVisible(true);
            emptyState.setManaged(true);
        } else {
            emptyState.setVisible(false);
            emptyState.setManaged(false);

            for (CategoryModel category : categories) {
                categoriesContainer.getChildren().add(createCategoryCard(category));
            }
        }

        if (categoryCountLabel != null) {
            categoryCountLabel.setText(categories.size() + " categor" + (categories.size() > 1 ? "ies" : "y"));
        }
    }

    private VBox createCategoryCard(CategoryModel category) {
        VBox card = new VBox();
        card.setAlignment(Pos.TOP_LEFT);
        card.setSpacing(16);
        card.setPrefWidth(340);
        card.setMinWidth(340);
        card.setMaxWidth(340);
        card.setPadding(new Insets(24));
        card.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 16;" +
            "-fx-border-color: " + (category.isActive() ? "#e0e7ff" : "#fee2e2") + ";" +
            "-fx-border-width: 2;" +
            "-fx-border-radius: 16;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 16, 0, 0, 4);"
        );

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(12);

        Label icon = new Label("📂");
        icon.setStyle("-fx-font-size: 28;");

        VBox titleBox = new VBox(4);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        Label name = new Label(category.getName());
        name.setStyle("-fx-font-size: 18; -fx-font-weight: 700; -fx-text-fill: #1e293b;");
        name.setWrapText(true);
        name.setMaxWidth(200);

        Label idLabel = new Label("ID: " + category.getId());
        idLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #94a3b8; -fx-font-weight: 500;");

        titleBox.getChildren().addAll(name, idLabel);

        Label statusBadge = new Label(category.isActive() ? "● Active" : "● Inactive");
        statusBadge.setStyle(
            "-fx-background-color: " + (category.isActive() ? "#dcfce7" : "#fee2e2") + ";" +
            "-fx-text-fill: " + (category.isActive() ? "#16a34a" : "#dc2626") + ";" +
            "-fx-font-size: 11; -fx-font-weight: 700; -fx-padding: 6 12;" +
            "-fx-background-radius: 20;"
        );

        header.getChildren().addAll(icon, titleBox, statusBadge);

        Label desc = new Label(category.getDescription());
        desc.setWrapText(true);
        desc.setMaxHeight(60);
        desc.setStyle("-fx-font-size: 13; -fx-text-fill: #64748b; -fx-line-spacing: 2;");

        // Gig Counter Badge
        int gigCount = getGigCountForCategory(category.getId());
        HBox gigCountBox = new HBox(8);
        gigCountBox.setAlignment(Pos.CENTER_LEFT);
        gigCountBox.setStyle(
            "-fx-background-color: #f0f9ff;" +
            "-fx-padding: 10 14;" +
            "-fx-background-radius: 10;" +
            "-fx-border-color: #bae6fd;" +
            "-fx-border-width: 1.5;" +
            "-fx-border-radius: 10;"
        );

        Label gigIcon = new Label("💼");
        gigIcon.setStyle("-fx-font-size: 16;");

        Label gigCountLabel = new Label(gigCount + " Gig" + (gigCount != 1 ? "s" : ""));
        gigCountLabel.setStyle(
            "-fx-font-size: 13; -fx-font-weight: 700; -fx-text-fill: #0369a1;"
        );

        gigCountBox.getChildren().addAll(gigIcon, gigCountLabel);

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #e2e8f0;");

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER);

        Button editBtn = new Button("✎ Edit");
        editBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(editBtn, Priority.ALWAYS);
        editBtn.setStyle(
            "-fx-background-color: #6366f1;" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 13; -fx-font-weight: 600;" +
            "-fx-padding: 10 16; -fx-background-radius: 8;" +
            "-fx-cursor: hand;"
        );
        editBtn.setOnAction(e -> showEditDialog(category));

        Button deleteBtn = new Button("✕ Delete");
        deleteBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(deleteBtn, Priority.ALWAYS);
        deleteBtn.setStyle(
            "-fx-background-color: #ef4444;" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 13; -fx-font-weight: 600;" +
            "-fx-padding: 10 16; -fx-background-radius: 8;" +
            "-fx-cursor: hand;"
        );
        deleteBtn.setOnAction(e -> deleteCategory(category));

        actions.getChildren().addAll(editBtn, deleteBtn);
        card.getChildren().addAll(header, desc, gigCountBox, sep, actions);

        return card;
    }

    private void updateStats() {
        int total = allCategories.size();
        int active = (int) allCategories.stream().filter(CategoryModel::isActive).count();
        int inactive = total - active;

        if (totalCategoriesLabel != null) totalCategoriesLabel.setText(String.valueOf(total));
        if (activeCategoriesLabel != null) activeCategoriesLabel.setText(String.valueOf(active));
        if (inactiveCategoriesLabel != null) inactiveCategoriesLabel.setText(String.valueOf(inactive));
    }

    private int getGigCountForCategory(int categoryId) {
        String query = "SELECT COUNT(*) as count FROM gig WHERE category_id = ?";
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setInt(1, categoryId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("count");
            }
        } catch (SQLException e) {
            System.err.println("Error counting gigs for category " + categoryId + ": " + e.getMessage());
        }
        return 0;
    }

    @FXML
    public void onAddCategory() {
        showEditDialog(null);
    }

    private void showEditDialog(CategoryModel categoryToEdit) {
        Dialog<CategoryModel> dialog = new Dialog<>();
        dialog.setTitle(categoryToEdit == null ? "New Category" : "Edit Category");
        dialog.setHeaderText(categoryToEdit == null ? "✨ Create New Category" : "✎ Edit Category");

        ButtonType saveButtonType = new ButtonType(categoryToEdit == null ? "Create" : "Save Changes", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Style the DialogPane
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 16;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 40, 0, 0, 10);"
        );

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(16);
        grid.setPadding(new Insets(24));
        grid.setStyle("-fx-min-width: 500;");

        TextField nameField = new TextField(categoryToEdit != null ? categoryToEdit.getName() : "");
        nameField.setPromptText("Category name");
        nameField.setStyle(
            "-fx-background-color: #f9fafb;" +
            "-fx-background-radius: 10;" +
            "-fx-border-color: #e5e7eb;" +
            "-fx-border-radius: 10;" +
            "-fx-border-width: 1.5;" +
            "-fx-padding: 12 16;" +
            "-fx-font-size: 14;" +
            "-fx-font-weight: 400;" +
            "-fx-text-fill: #1f2937;"
        );

        TextArea descField = new TextArea(categoryToEdit != null ? categoryToEdit.getDescription() : "");
        descField.setPromptText("Category description");
        descField.setPrefRowCount(4);
        descField.setWrapText(true);
        descField.setStyle(
            "-fx-background-color: #f9fafb;" +
            "-fx-background-radius: 10;" +
            "-fx-border-color: #e5e7eb;" +
            "-fx-border-radius: 10;" +
            "-fx-border-width: 1.5;" +
            "-fx-padding: 12 16;" +
            "-fx-font-size: 14;" +
            "-fx-text-fill: #1f2937;"
        );

        // Generate Description Button
        Button generateDescBtn = new Button("✨ Generate Description");
        generateDescBtn.setStyle(
            "-fx-background-color: linear-gradient(to right, #10b981, #059669);" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 13;" +
            "-fx-font-weight: 600;" +
            "-fx-padding: 8 16;" +
            "-fx-background-radius: 8;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(16,185,129,0.3), 8, 0, 0, 2);"
        );
        generateDescBtn.setOnAction(e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                showError("Please enter a category name first!");
                return;
            }

            generateDescBtn.setDisable(true);
            generateDescBtn.setText("⏳ Generating...");

            // Run in background thread
            new Thread(() -> {
                try {
                    System.out.println("🚀 Starting FREE AI generation for category...");
                    System.out.println("   Category name: " + name);

                    // Utilise le service AI GRATUIT (pas besoin de quota/crédit)
                    com.khademni.utils.FreeAIService freeAI = new com.khademni.utils.FreeAIService();
                    String generatedDesc = freeAI.generateCategoryDescription(name);

                    System.out.println("✅ AI generation completed for category!");

                    // Update UI on JavaFX thread
                    javafx.application.Platform.runLater(() -> {
                        descField.setText(generatedDesc);
                        generateDescBtn.setDisable(false);
                        generateDescBtn.setText("✨ Generate Description");
                        showSuccess("Description generated with FREE AI! ✨");
                    });
                } catch (Exception ex) {
                    System.err.println("❌ Error during AI generation for category:");
                    ex.printStackTrace();

                    javafx.application.Platform.runLater(() -> {
                        generateDescBtn.setDisable(false);
                        generateDescBtn.setText("✨ Generate Description");
                        showError("Error generating description: " + ex.getMessage());
                    });
                }
            }).start();
        });

        CheckBox activeCheck = new CheckBox("Active Category");
        activeCheck.setSelected(categoryToEdit != null ? categoryToEdit.isActive() : true);
        activeCheck.setStyle("-fx-font-size: 14; -fx-font-weight: 600; -fx-text-fill: #1e293b;");

        Label nameError = new Label();
        nameError.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12; -fx-font-weight: 600;");
        nameError.setMinHeight(18);
        nameError.setWrapText(true);
        nameError.setMaxWidth(Double.MAX_VALUE);

        Label descError = new Label();
        descError.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12; -fx-font-weight: 600;");
        descError.setMinHeight(18);
        descError.setWrapText(true);
        descError.setMaxWidth(Double.MAX_VALUE);

        // Labels for fields
        Label nameLabel = new Label("Name:");
        nameLabel.setStyle("-fx-font-size: 14; -fx-font-weight: 600; -fx-text-fill: #1e293b;");

        Label descLabel = new Label("Description:");
        descLabel.setStyle("-fx-font-size: 14; -fx-font-weight: 600; -fx-text-fill: #1e293b;");

        int row = 0;
        grid.add(nameLabel, 0, row);
        grid.add(nameField, 1, row++);
        grid.add(nameError, 1, row++);

        grid.add(descLabel, 0, row);
        grid.add(descField, 1, row++);
        grid.add(generateDescBtn, 1, row++);
        grid.add(descError, 1, row++);

        grid.add(activeCheck, 1, row);

        dialog.getDialogPane().setContent(grid);

        // Style the buttons
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.setStyle(
            "-fx-background-color: linear-gradient(to bottom right, #6366f1, #8b5cf6);" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 14;" +
            "-fx-font-weight: 700;" +
            "-fx-padding: 12 32;" +
            "-fx-background-radius: 10;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(99,102,241,0.3), 12, 0, 0, 4);"
        );

        Button cancelButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelButton.setStyle(
            "-fx-background-color: #f3f4f6;" +
            "-fx-text-fill: #374151;" +
            "-fx-font-size: 14;" +
            "-fx-font-weight: 600;" +
            "-fx-padding: 12 32;" +
            "-fx-background-radius: 10;" +
            "-fx-cursor: hand;"
        );

        Runnable validateForm = () -> {
            boolean valid = true;
            System.out.println("=== Validating category form ===");

            nameError.setText("");
            descError.setText("");

            if (nameField.getText().trim().isEmpty()) {
                nameError.setText("✗ Name is required");
                valid = false;
                System.out.println("  ✗ Name empty");
            } else if (nameField.getText().trim().length() < 3) {
                nameError.setText("✗ Name must be at least 3 characters");
                valid = false;
                System.out.println("  ✗ Name too short");
            } else {
                System.out.println("  ✓ Name OK");
            }

            if (descField.getText().trim().isEmpty()) {
                descError.setText("✗ Description is required");
                valid = false;
                System.out.println("  ✗ Description empty");
            } else if (descField.getText().trim().length() < 10) {
                descError.setText("✗ Description must be at least 10 characters");
                valid = false;
                System.out.println("  ✗ Description too short");
            } else {
                System.out.println("  ✓ Description OK");
            }

            System.out.println("  Form valid: " + valid);
            saveButton.setDisable(!valid);
        };

        nameField.textProperty().addListener((obs, old, val) -> validateForm.run());
        descField.textProperty().addListener((obs, old, val) -> validateForm.run());
        validateForm.run();

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                CategoryModel cat = categoryToEdit != null ? categoryToEdit : new CategoryModel();
                cat.setName(nameField.getText().trim());
                cat.setDescription(descField.getText().trim());
                cat.setActive(activeCheck.isSelected());
                return cat;
            }
            return null;
        });

        Optional<CategoryModel> result = dialog.showAndWait();
        if (result.isPresent()) {
            CategoryModel cat = result.get();
            if (saveCategory(categoryToEdit, cat.getName(), cat.getDescription(), cat.isActive())) {
                loadCategories();
                updateStats();
            }
        }
    }

    private boolean saveCategory(CategoryModel categoryToEdit, String name, String desc, boolean isActive) {
        String query;

        if (categoryToEdit == null) {
            // Create new category
            query = "INSERT INTO category (name, description, is_active) VALUES (?, ?, ?)";
        } else {
            // Update existing category
            query = "UPDATE category SET name = ?, description = ?, is_active = ? WHERE id = ?";
        }

        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setString(1, name);
            ps.setString(2, desc);
            ps.setBoolean(3, isActive);

            if (categoryToEdit != null) {
                ps.setInt(4, categoryToEdit.getId());
            }

            ps.executeUpdate();

            if (categoryToEdit == null) {
                showSuccess("✅ Category created successfully!");
            } else {
                showSuccess("✅ Category updated successfully!");
            }

            return true;

        } catch (SQLException e) {
            System.err.println("ERROR saving category: " + e.getMessage());
            e.printStackTrace();
            showError("❌ Error saving category: " + e.getMessage());
            return false;
        }
    }

    private void deleteCategory(CategoryModel category) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Category");
        alert.setHeaderText("Are you sure?");
        alert.setContentText("Delete category: " + category.getName() + "?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String query = "DELETE FROM category WHERE id = ?";

            try (Connection conn = MyDataBase.getConnection();
                 PreparedStatement ps = conn.prepareStatement(query)) {

                ps.setInt(1, category.getId());
                ps.executeUpdate();

                showSuccess("✅ Category deleted successfully!");
                loadCategories();

            } catch (SQLException e) {
                System.err.println("ERROR deleting category: " + e.getMessage());
                e.printStackTrace();
                showError("Error deleting category: " + e.getMessage());
            }
        }
    }

    @FXML
    public void onClearFilters() {
        if (searchField != null) searchField.clear();
        if (statusFilterCombo != null) statusFilterCombo.setValue("All Status");
        applyFilters();
    }

    @FXML
    public void onRefresh() {
        loadCategories();
        showSuccess("🔄 Categories refreshed!");
    }

    @FXML
    public void onGoBack() {
        try {
            com.khademni.App.setRoot("profile");
        } catch (IOException e) {
            showError("Error navigating to profile: " + e.getMessage());
        }
    }

    @FXML
    public void onExit() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Exit");
        alert.setHeaderText("Are you sure?");
        alert.setContentText("Do you want to exit the application?");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            javafx.application.Platform.exit();
            System.exit(0);
        }
    }

    private void showError(String message) {
        if (errorLabel == null || messageContainer == null) return;

        errorLabel.setText(message);
        messageContainer.setStyle("-fx-background-color: #fee2e2; -fx-padding: 16 24;");
        errorLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 14; -fx-font-weight: 600;");
        messageContainer.setVisible(true);
        messageContainer.setManaged(true);

        new Thread(() -> {
            try {
                Thread.sleep(5000);
                javafx.application.Platform.runLater(() -> {
                    messageContainer.setVisible(false);
                    messageContainer.setManaged(false);
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void showSuccess(String message) {
        if (errorLabel == null || messageContainer == null) return;

        errorLabel.setText(message);
        messageContainer.setStyle("-fx-background-color: #d1fae5; -fx-padding: 16 24;");
        errorLabel.setStyle("-fx-text-fill: #047857; -fx-font-size: 14; -fx-font-weight: 600;");
        messageContainer.setVisible(true);
        messageContainer.setManaged(true);

        new Thread(() -> {
            try {
                Thread.sleep(3000);
                javafx.application.Platform.runLater(() -> {
                    messageContainer.setVisible(false);
                    messageContainer.setManaged(false);
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
