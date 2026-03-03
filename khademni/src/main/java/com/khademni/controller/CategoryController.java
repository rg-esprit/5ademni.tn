package com.khademni.controller;

import com.khademni.model.CategoryModel;
import com.khademni.utils.MyDataBase;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.io.IOException;
import java.sql.*;
import java.util.*;

public class CategoryController {

    @FXML private Label totalCategoriesLabel;
    @FXML private Label activeCategoriesLabel;
    @FXML private Label inactiveCategoriesLabel;
    @FXML private Label categoryCountLabel;
    @FXML private VBox statsContainer;
    @FXML private Label totalGigsLabel;
    @FXML private Label mostActiveCategoryLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private FlowPane categoriesContainer;
    @FXML private VBox emptyState;
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
        loadCategoryStatistics();
        System.out.println("=== CategoryController: Initialization complete ===");
    }

    private void loadCategories() {
        System.out.println("Loading categories...");
        allCategories.clear();
        String query = "SELECT id, name, description, is_active FROM category ORDER BY id DESC";
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                CategoryModel cat = new CategoryModel(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getBoolean("is_active")
                );
                allCategories.add(cat);
                System.out.println("  Loaded category: " + cat.getName() + " (ID: " + cat.getId() + ")");
            }
            System.out.println("Total categories loaded: " + allCategories.size());
            applyFilters();
        } catch (SQLException e) {
            showError("Error loading categories: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadCategoryStatistics() {
        if (statsContainer == null) return;
        statsContainer.getChildren().clear();
        String query = "SELECT c.name, c.is_active, COUNT(g.id) AS gig_count " +
                       "FROM category c LEFT JOIN gig g ON g.category_id = c.id " +
                       "GROUP BY c.id, c.name, c.is_active ORDER BY gig_count DESC";
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            List<String> names = new ArrayList<>();
            List<Boolean> actives = new ArrayList<>();
            List<Integer> counts = new ArrayList<>();
            int totalGigs = 0;
            while (rs.next()) {
                names.add(rs.getString("name"));
                actives.add(rs.getBoolean("is_active"));
                int c = rs.getInt("gig_count");
                counts.add(c);
                totalGigs += c;
            }
            if (totalGigsLabel != null) totalGigsLabel.setText(String.valueOf(totalGigs));
            if (mostActiveCategoryLabel != null) {
                mostActiveCategoryLabel.setText(!names.isEmpty() && counts.get(0) > 0
                    ? names.get(0) + " (" + counts.get(0) + ")" : "-");
            }
            String[] colors = {"#6366f1","#10b981","#f59e0b","#ef4444","#3b82f6",
                               "#8b5cf6","#ec4899","#14b8a6","#f97316","#84cc16"};
            for (int i = 0; i < names.size(); i++) {
                double pct = totalGigs > 0 ? (counts.get(i) * 100.0 / totalGigs) : 0;
                statsContainer.getChildren().add(
                    buildStatRow(names.get(i), actives.get(i), counts.get(i),
                                 pct, colors[i % colors.length], i % 2 == 0)
                );
            }
            if (names.isEmpty()) {
                Label empty = new Label("No data available");
                empty.setStyle("-fx-padding:24;-fx-text-fill:#94a3b8;-fx-font-size:14;");
                statsContainer.getChildren().add(empty);
            }
        } catch (SQLException e) {
            System.err.println("ERROR loading statistics: " + e.getMessage());
        }
    }

    private HBox buildStatRow(String name, boolean active, int count, double pct,
                               String color, boolean even) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14, 28, 14, 28));
        row.setStyle("-fx-background-color:" + (even ? "white" : "#f8fafc") +
                     ";-fx-border-color:#f1f5f9;-fx-border-width:0 0 1 0;");

        HBox nameBox = new HBox(10);
        nameBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(nameBox, Priority.ALWAYS);
        Label dot = new Label("*");
        dot.setStyle("-fx-font-size:14;-fx-text-fill:" + color + ";");
        Label nl = new Label(name);
        nl.setStyle("-fx-font-size:14;-fx-font-weight:600;-fx-text-fill:#1e293b;");
        nameBox.getChildren().addAll(dot, nl);

        Label sb = new Label(active ? "Active" : "Inactive");
        sb.setMinWidth(80);
        sb.setAlignment(Pos.CENTER);
        sb.setStyle("-fx-background-color:" + (active ? "#dcfce7" : "#fee2e2") +
                    ";-fx-text-fill:" + (active ? "#16a34a" : "#dc2626") +
                    ";-fx-font-size:11;-fx-font-weight:700;-fx-padding:4 10;-fx-background-radius:20;");

        Label cl = new Label(String.valueOf(count));
        cl.setMinWidth(50);
        cl.setAlignment(Pos.CENTER);
        cl.setStyle("-fx-font-size:15;-fx-font-weight:700;-fx-text-fill:" + color + ";");

        HBox bar = new HBox();
        bar.setMinWidth(200); bar.setMaxWidth(200);
        bar.setPadding(new Insets(0, 0, 0, 16));
        bar.setAlignment(Pos.CENTER_LEFT);
        StackPane track = new StackPane();
        track.setMinWidth(180); track.setMaxWidth(180);
        track.setMinHeight(10); track.setMaxHeight(10);
        track.setStyle("-fx-background-color:#e2e8f0;-fx-background-radius:10;");
        double fw = Math.max(pct / 100.0 * 180, pct > 0 ? 6 : 0);
        HBox fill = new HBox();
        fill.setPrefWidth(fw); fill.setMinWidth(fw); fill.setMaxWidth(fw);
        fill.setPrefHeight(10);
        fill.setStyle("-fx-background-color:" + color + ";-fx-background-radius:10;");
        StackPane.setAlignment(fill, Pos.CENTER_LEFT);
        track.getChildren().add(fill);
        bar.getChildren().add(track);

        Label pl = new Label(String.format("%.1f%%", pct));
        pl.setMinWidth(50);
        pl.setAlignment(Pos.CENTER_RIGHT);
        pl.setStyle("-fx-font-size:13;-fx-font-weight:600;-fx-text-fill:#64748b;");

        row.getChildren().addAll(nameBox, sb, cl, bar, pl);
        return row;
    }

    private void applyFilters() {
        List<CategoryModel> filtered = new ArrayList<>(allCategories);
        if (searchField != null && !searchField.getText().trim().isEmpty()) {
            String s = searchField.getText().toLowerCase();
            filtered.removeIf(c -> !c.getName().toLowerCase().contains(s)
                              && !c.getDescription().toLowerCase().contains(s));
        }
        if (statusFilterCombo != null && statusFilterCombo.getValue() != null) {
            String status = statusFilterCombo.getValue();
            if ("Active".equals(status)) filtered.removeIf(c -> !c.isActive());
            else if ("Inactive".equals(status)) filtered.removeIf(CategoryModel::isActive);
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
            for (CategoryModel cat : categories) {
                categoriesContainer.getChildren().add(createCategoryCard(cat));
            }
        }
        if (categoryCountLabel != null) {
            categoryCountLabel.setText(categories.size() + " categor" +
                (categories.size() > 1 ? "ies" : "y"));
        }
    }

    private VBox createCategoryCard(CategoryModel category) {
        VBox card = new VBox();
        card.setAlignment(Pos.TOP_LEFT);
        card.setSpacing(16);
        card.setPrefWidth(340); card.setMinWidth(340); card.setMaxWidth(340);
        card.setPadding(new Insets(24));
        card.setStyle("-fx-background-color:white;-fx-background-radius:16;" +
                      "-fx-border-color:" + (category.isActive() ? "#e0e7ff" : "#fee2e2") +
                      ";-fx-border-width:2;-fx-border-radius:16;" +
                      "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),16,0,0,4);");

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        VBox titleBox = new VBox(4);
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        Label nl = new Label(category.getName());
        nl.setStyle("-fx-font-size:18;-fx-font-weight:700;-fx-text-fill:#1e293b;");
        nl.setWrapText(true); nl.setMaxWidth(200);
        Label idLabel = new Label("ID: " + category.getId());
        idLabel.setStyle("-fx-font-size:11;-fx-text-fill:#94a3b8;-fx-font-weight:500;");
        titleBox.getChildren().addAll(nl, idLabel);
        Label sb = new Label(category.isActive() ? "Active" : "Inactive");
        sb.setStyle("-fx-background-color:" + (category.isActive() ? "#dcfce7" : "#fee2e2") +
                    ";-fx-text-fill:" + (category.isActive() ? "#16a34a" : "#dc2626") +
                    ";-fx-font-size:11;-fx-font-weight:700;-fx-padding:6 12;-fx-background-radius:20;");
        header.getChildren().addAll(titleBox, sb);

        Label desc = new Label(category.getDescription());
        desc.setWrapText(true); desc.setMaxHeight(60);
        desc.setStyle("-fx-font-size:13;-fx-text-fill:#64748b;-fx-line-spacing:2;");

        int gigCount = getGigCountForCategory(category.getId());
        HBox gigBox = new HBox(8);
        gigBox.setAlignment(Pos.CENTER_LEFT);
        gigBox.setStyle("-fx-background-color:#f0f9ff;-fx-padding:10 14;" +
                        "-fx-background-radius:10;-fx-border-color:#bae6fd;" +
                        "-fx-border-width:1.5;-fx-border-radius:10;");
        Label gigLabel = new Label(gigCount + " Gig" + (gigCount != 1 ? "s" : ""));
        gigLabel.setStyle("-fx-font-size:13;-fx-font-weight:700;-fx-text-fill:#0369a1;");
        gigBox.getChildren().add(gigLabel);

        Separator sep = new Separator();
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER);

        Button editBtn = new Button("Edit");
        editBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(editBtn, Priority.ALWAYS);
        editBtn.setStyle("-fx-background-color:#6366f1;-fx-text-fill:white;" +
                         "-fx-font-size:13;-fx-font-weight:600;-fx-padding:10 16;" +
                         "-fx-background-radius:8;-fx-cursor:hand;");
        editBtn.setOnAction(e -> showEditDialog(category));

        Button deleteBtn = new Button("Delete");
        deleteBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(deleteBtn, Priority.ALWAYS);
        deleteBtn.setStyle("-fx-background-color:#ef4444;-fx-text-fill:white;" +
                           "-fx-font-size:13;-fx-font-weight:600;-fx-padding:10 16;" +
                           "-fx-background-radius:8;-fx-cursor:hand;");
        deleteBtn.setOnAction(e -> deleteCategory(category));
        actions.getChildren().addAll(editBtn, deleteBtn);

        card.getChildren().addAll(header, desc, gigBox, sep, actions);
        return card;
    }

    private void updateStats() {
        int total = allCategories.size();
        int active = (int) allCategories.stream().filter(CategoryModel::isActive).count();
        if (totalCategoriesLabel    != null) totalCategoriesLabel.setText(String.valueOf(total));
        if (activeCategoriesLabel   != null) activeCategoriesLabel.setText(String.valueOf(active));
        if (inactiveCategoriesLabel != null) inactiveCategoriesLabel.setText(String.valueOf(total - active));
    }

    private int getGigCountForCategory(int categoryId) {
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) FROM gig WHERE category_id = ?")) {
            ps.setInt(1, categoryId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("Error counting gigs: " + e.getMessage());
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
        dialog.setHeaderText(categoryToEdit == null ? "Create New Category" : "Edit Category");

        ButtonType saveBtn = new ButtonType(
            categoryToEdit == null ? "Create" : "Save Changes",
            ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);
        dialog.getDialogPane().setStyle(
            "-fx-background-color:white;-fx-background-radius:16;");

        GridPane grid = new GridPane();
        grid.setHgap(16); grid.setVgap(16);
        grid.setPadding(new Insets(24));
        grid.setMinWidth(520);

        String fieldStyle = "-fx-background-color:#f9fafb;-fx-background-radius:10;" +
                            "-fx-border-color:#e5e7eb;-fx-border-radius:10;" +
                            "-fx-border-width:1.5;-fx-padding:12 16;-fx-font-size:14;";

        TextField nameField = new TextField(
            categoryToEdit != null ? categoryToEdit.getName() : "");
        nameField.setPromptText("Category name (min 3 characters)");
        nameField.setStyle(fieldStyle);

        TextArea descField = new TextArea(
            categoryToEdit != null ? categoryToEdit.getDescription() : "");
        descField.setPromptText("Category description (min 10 characters)");
        descField.setPrefRowCount(4);
        descField.setWrapText(true);
        descField.setStyle(fieldStyle);

        Button genBtn = new Button("Generate with AI");
        genBtn.setStyle("-fx-background-color:linear-gradient(to right,#10b981,#059669);" +
                        "-fx-text-fill:white;-fx-font-size:13;-fx-font-weight:600;" +
                        "-fx-padding:8 16;-fx-background-radius:8;-fx-cursor:hand;");
        genBtn.setOnAction(e -> {
            String n = nameField.getText().trim();
            if (n.isEmpty()) { showError("Enter a category name first!"); return; }
            genBtn.setDisable(true); genBtn.setText("Generating...");
            new Thread(() -> {
                try {
                    com.khademni.utils.FreeAIService ai =
                        new com.khademni.utils.FreeAIService();
                    String generatedDesc = ai.generateCategoryDescription(n);
                    Platform.runLater(() -> {
                        descField.setText(generatedDesc);
                        genBtn.setDisable(false);
                        genBtn.setText("Generate with AI");
                        showSuccess("Description generated!");
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        genBtn.setDisable(false);
                        genBtn.setText("Generate with AI");
                        showError("Error: " + ex.getMessage());
                    });
                }
            }).start();
        });

        CheckBox activeCheck = new CheckBox("Active Category");
        activeCheck.setSelected(categoryToEdit == null || categoryToEdit.isActive());
        activeCheck.setStyle(
            "-fx-font-size:14;-fx-font-weight:600;-fx-text-fill:#1e293b;");

        Label nameErr = new Label();
        nameErr.setStyle(
            "-fx-text-fill:#ef4444;-fx-font-size:12;-fx-font-weight:600;");
        nameErr.setMinHeight(18);
        nameErr.setMaxWidth(Double.MAX_VALUE);
        nameErr.setWrapText(true);

        Label descErr = new Label();
        descErr.setStyle(
            "-fx-text-fill:#ef4444;-fx-font-size:12;-fx-font-weight:600;");
        descErr.setMinHeight(18);
        descErr.setMaxWidth(Double.MAX_VALUE);
        descErr.setWrapText(true);

        Label nameLabel = new Label("Name:");
        nameLabel.setStyle(
            "-fx-font-size:14;-fx-font-weight:600;-fx-text-fill:#1e293b;");
        Label descLabel = new Label("Description:");
        descLabel.setStyle(
            "-fx-font-size:14;-fx-font-weight:600;-fx-text-fill:#1e293b;");

        int r = 0;
        grid.add(nameLabel, 0, r); grid.add(nameField, 1, r++);
        grid.add(nameErr, 1, r++);
        grid.add(descLabel, 0, r); grid.add(descField, 1, r++);
        grid.add(genBtn, 1, r++);
        grid.add(descErr, 1, r++);
        grid.add(activeCheck, 1, r);
        dialog.getDialogPane().setContent(grid);

        Button save = (Button) dialog.getDialogPane().lookupButton(saveBtn);
        save.setStyle(
            "-fx-background-color:linear-gradient(to bottom right,#6366f1,#8b5cf6);" +
            "-fx-text-fill:white;-fx-font-size:14;-fx-font-weight:700;" +
            "-fx-padding:12 32;-fx-background-radius:10;-fx-cursor:hand;");
        Button cancel = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancel.setStyle(
            "-fx-background-color:#f3f4f6;-fx-text-fill:#374151;" +
            "-fx-font-size:14;-fx-font-weight:600;-fx-padding:12 32;" +
            "-fx-background-radius:10;-fx-cursor:hand;");

        Runnable validate = () -> {
            boolean valid = true;
            nameErr.setText(""); descErr.setText("");
            String nm = nameField.getText().trim();
            String dc = descField.getText().trim();
            if (nm.isEmpty()) {
                nameErr.setText("Name is required");
                valid = false;
            } else if (nm.length() < 3) {
                nameErr.setText("Name must be at least 3 characters");
                valid = false;
            }
            if (dc.isEmpty()) {
                descErr.setText("Description is required");
                valid = false;
            } else if (dc.length() < 10) {
                descErr.setText("Description must be at least 10 characters");
                valid = false;
            }
            save.setDisable(!valid);
        };
        nameField.textProperty().addListener((o, ov, nv) -> validate.run());
        descField.textProperty().addListener((o, ov, nv) -> validate.run());
        validate.run();

        dialog.setResultConverter(btn -> {
            if (btn == saveBtn) {
                CategoryModel cat =
                    categoryToEdit != null ? categoryToEdit : new CategoryModel();
                cat.setName(nameField.getText().trim());
                cat.setDescription(descField.getText().trim());
                cat.setActive(activeCheck.isSelected());
                return cat;
            }
            return null;
        });

        dialog.initOwner(categoriesContainer.getScene().getWindow());
        Optional<CategoryModel> res = dialog.showAndWait();
        if (res.isPresent()) {
            CategoryModel cat = res.get();
            if (saveCategory(categoryToEdit, cat.getName(),
                             cat.getDescription(), cat.isActive())) {
                loadCategories();
                updateStats();
                loadCategoryStatistics();
            }
        }
    }

    private boolean saveCategory(CategoryModel toEdit, String name,
                                  String desc, boolean active) {
        String query = toEdit == null
            ? "INSERT INTO category (name,description,is_active) VALUES (?,?,?)"
            : "UPDATE category SET name=?,description=?,is_active=? WHERE id=?";
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, name);
            ps.setString(2, desc);
            ps.setBoolean(3, active);
            if (toEdit != null) ps.setInt(4, toEdit.getId());
            ps.executeUpdate();
            showSuccess(toEdit == null ? "Category created!" : "Category updated!");
            return true;
        } catch (SQLException e) {
            showError("Error saving: " + e.getMessage());
            return false;
        }
    }

    private void deleteCategory(CategoryModel category) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(categoriesContainer.getScene().getWindow());
        alert.setTitle("Delete Category");
        alert.setHeaderText("Are you sure you want to delete this category?");
        alert.setContentText("Delete: " + category.getName() + "?");
        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try (Connection conn = MyDataBase.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "DELETE FROM category WHERE id=?")) {
                ps.setInt(1, category.getId());
                ps.executeUpdate();
                showSuccess("Category deleted successfully!");
                loadCategories();
                loadCategoryStatistics();
            } catch (SQLException e) {
                showError("Error deleting: " + e.getMessage());
            }
        }
    }

    @FXML public void onClearFilters() {
        if (searchField != null) searchField.clear();
        if (statusFilterCombo != null) statusFilterCombo.setValue("All Status");
        applyFilters();
    }

    @FXML public void onRefresh() {
        loadCategories();
        loadCategoryStatistics();
        showSuccess("Refreshed successfully!");
    }

    @FXML public void onGoBack() {
        try {
            com.khademni.App.setRoot("profile");
        } catch (IOException e) {
            showError("Error navigating back: " + e.getMessage());
        }
    }

    @FXML public void onExit() {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.initOwner(categoriesContainer.getScene().getWindow());
        a.setTitle("Exit");
        a.setHeaderText("Exit the application?");
        a.setContentText("Are you sure you want to exit?");
        if (a.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            Platform.exit();
            System.exit(0);
        }
    }

    private void showMessage(String message, boolean isError) {
        if (errorLabel == null || messageContainer == null) return;
        errorLabel.setText(message);
        messageContainer.setStyle("-fx-background-color:" +
            (isError ? "#fee2e2" : "#d1fae5") +
            ";-fx-padding:16 24;-fx-background-radius:10;");
        errorLabel.setStyle("-fx-text-fill:" +
            (isError ? "#dc2626" : "#047857") +
            ";-fx-font-size:14;-fx-font-weight:600;");
        messageContainer.setVisible(true);
        messageContainer.setManaged(true);
        new Thread(() -> {
            try { Thread.sleep(isError ? 5000 : 3000); }
            catch (InterruptedException ignored) {}
            Platform.runLater(() -> {
                messageContainer.setVisible(false);
                messageContainer.setManaged(false);
            });
        }).start();
    }

    private void showError(String msg)   { showMessage(msg, true);  }
    private void showSuccess(String msg) { showMessage(msg, false); }
}

