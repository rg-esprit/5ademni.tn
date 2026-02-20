package com.khademni.controller;

import com.khademni.App;
import com.khademni.model.CategoryModel;
import com.khademni.model.GigModel;
import com.khademni.utils.MyDataBase;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controller for Gig Management (Fiverr-Style Modern UI)
 */
public class GigController implements Initializable {

    // ===== FXML Components - Filters =====
    @FXML private TextField searchField;
    @FXML private ComboBox<CategoryModel> filterCategoryComboBox;
    @FXML private ComboBox<String> filterStatusComboBox;
    @FXML private TextField minPriceField;
    @FXML private TextField maxPriceField;

    // ===== FXML Components - Stats & Display =====
    @FXML private Label messageLabel;
    @FXML private Label totalGigsLabel;
    @FXML private Label activeGigsLabel;
    @FXML private Label totalRevenueLabel;
    @FXML private FlowPane gigsContainer;
    @FXML private VBox emptyState;

    // ===== Data =====
    private List<GigModel> allGigs = new ArrayList<>();
    private List<CategoryModel> categories = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        System.out.println("=== GigController: Starting initialization ===");

        // Check if components are initialized
        System.out.println("searchField: " + (searchField != null ? "OK" : "NULL"));
        System.out.println("filterCategoryComboBox: " + (filterCategoryComboBox != null ? "OK" : "NULL"));
        System.out.println("filterStatusComboBox: " + (filterStatusComboBox != null ? "OK" : "NULL"));
        System.out.println("gigsContainer: " + (gigsContainer != null ? "OK" : "NULL"));

        setupFilters();
        System.out.println("Filters setup complete");

        loadCategories();
        System.out.println("Categories loaded: " + categories.size());

        loadGigs();
        System.out.println("Gigs loaded: " + allGigs.size());

        updateStats();
        System.out.println("=== GigController: Initialization complete ===");
    }

    // ==================== SETUP ====================

    private void setupFilters() {
        // Status filter
        filterStatusComboBox.getItems().addAll("All Status", "Active", "Inactive", "Expired");
        filterStatusComboBox.setValue("All Status");

        // Add listeners for real-time filtering
        filterStatusComboBox.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        filterCategoryComboBox.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        // Real-time search
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        // Price fields listeners
        minPriceField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*\\.?\\d*")) {
                minPriceField.setText(oldVal);
            }
        });
        maxPriceField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*\\.?\\d*")) {
                maxPriceField.setText(oldVal);
            }
        });
    }

    private void loadCategories() {
        categories.clear();
        System.out.println("Loading categories...");

        // Default option - using constructor with isActive
        CategoryModel allCat = new CategoryModel(0, "All Categories", "", true);
        categories.add(allCat);

        // Load categories using 'name' column
        String query = "SELECT id, name, description, is_active FROM category";

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
                categories.add(cat);
                count++;
                System.out.println("  Loaded category: " + cat.getName());
            }

            System.out.println("Total categories loaded: " + count);

            if (filterCategoryComboBox != null) {
                filterCategoryComboBox.getItems().setAll(categories);
                filterCategoryComboBox.setValue(categories.get(0));
                System.out.println("Categories set in ComboBox");
            }

            if (count > 0) {
                System.out.println("✓ Categories loaded successfully!");
            } else {
                System.out.println("⚠ WARNING: No categories found in database!");
                System.out.println("→ Please add some categories to the database.");
            }

        } catch (SQLException e) {
            System.err.println("ERROR loading categories: " + e.getMessage());
            e.printStackTrace();
            showError("Error loading categories: " + e.getMessage());
        }
    }

    private void loadGigs() {
        allGigs.clear();
        System.out.println("Loading gigs...");

        // Load ALL gigs (no user_id filter)
        String query = """
            SELECT g.id, g.title, g.description, g.price, g.delivery_time,
                   g.image, g.status, c.id as cat_id, c.name as cat_name, c.description as cat_desc
            FROM gig g
            LEFT JOIN category c ON g.category_id = c.id
            ORDER BY g.id DESC
        """;

        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            System.out.println("Loading ALL gigs from database...");
            ResultSet rs = ps.executeQuery();

            int count = 0;
            while (rs.next()) {
                // Check if category exists
                int catId = rs.getInt("cat_id");
                CategoryModel category;

                if (catId > 0) {
                    category = new CategoryModel(
                            catId,
                            rs.getString("cat_name"),
                            rs.getString("cat_desc"),
                            true
                    );
                } else {
                    // Default category if no category assigned
                    category = new CategoryModel(0, "Uncategorized", "No category", true);
                }

                LocalDateTime deliveryTime = rs.getTimestamp("delivery_time").toLocalDateTime();
                String dbStatus = rs.getString("status");

                // Calculate dynamic status based on delivery time
                String actualStatus = calculateDynamicStatus(deliveryTime, dbStatus);

                GigModel gig = new GigModel(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getDouble("price"),
                        deliveryTime,
                        rs.getString("image"),
                        actualStatus  // Use dynamic status
                );
                gig.setCategory(category);

                allGigs.add(gig);
                count++;
                System.out.println("  Loaded gig: " + gig.getTitle() + " (Status: " + actualStatus + ")");
            }

            System.out.println("Total gigs loaded: " + count);

            applyFilters();

        } catch (SQLException e) {
            System.err.println("ERROR loading gigs: " + e.getMessage());
            e.printStackTrace();
            showError("Error loading gigs: " + e.getMessage());
        }
    }

    /**
     * Calculate dynamic status based on delivery time
     * If delivery_time < current time → status becomes "EXPIRED"
     */
    private String calculateDynamicStatus(LocalDateTime deliveryTime, String dbStatus) {
        LocalDateTime now = LocalDateTime.now();

        if (deliveryTime.isBefore(now)) {
            System.out.println("    → Gig expired (delivery: " + deliveryTime + " < now: " + now + ")");
            return "EXPIRED";
        }

        // Return original status from database
        return dbStatus;
    }

    // ==================== FILTERS ====================

    @FXML
    public void applyFilters() {
        if (searchField == null || filterCategoryComboBox == null || filterStatusComboBox == null) {
            return; // Components not yet initialized
        }

        String searchText = searchField.getText().toLowerCase().trim();
        CategoryModel selectedCategory = filterCategoryComboBox.getValue();
        String selectedStatus = filterStatusComboBox.getValue();

        String minPriceStr = minPriceField.getText().trim();
        String maxPriceStr = maxPriceField.getText().trim();

        double minPrice = minPriceStr.isEmpty() ? 0 : parseDouble(minPriceStr, 0);
        double maxPrice = maxPriceStr.isEmpty() ? Double.MAX_VALUE : parseDouble(maxPriceStr, Double.MAX_VALUE);

        List<GigModel> filtered = allGigs.stream()
                .filter(gig -> searchText.isEmpty() ||
                        gig.getTitle().toLowerCase().contains(searchText) ||
                        gig.getDescription().toLowerCase().contains(searchText))
                .filter(gig -> selectedCategory == null || selectedCategory.getId() == 0 ||
                        gig.getCategory().getId() == selectedCategory.getId())
                .filter(gig -> selectedStatus.equals("All Status") ||
                        gig.getStatus().equalsIgnoreCase(selectedStatus))
                .filter(gig -> gig.getPrice() >= minPrice && gig.getPrice() <= maxPrice)
                .toList();

        displayGigs(filtered);
    }

    @FXML
    public void clearFilters() {
        searchField.clear();
        filterCategoryComboBox.setValue(categories.get(0));
        filterStatusComboBox.setValue("All Status");
        minPriceField.clear();
        maxPriceField.clear();
        displayGigs(allGigs);
    }

    // ==================== DISPLAY ====================

    private void displayGigs(List<GigModel> gigs) {
        if (gigsContainer == null || emptyState == null) {
            System.out.println("Display containers not yet initialized");
            return;
        }

        gigsContainer.getChildren().clear();

        if (gigs.isEmpty()) {
            emptyState.setVisible(true);
            emptyState.setManaged(true);
        } else {
            emptyState.setVisible(false);
            emptyState.setManaged(false);

            for (GigModel gig : gigs) {
                gigsContainer.getChildren().add(createGigCard(gig));
            }
        }
    }

    private VBox createGigCard(GigModel gig) {
        VBox card = new VBox();
        card.getStyleClass().add("gig-card");
        card.setPrefWidth(320);
        card.setMaxWidth(320);
        card.setStyle("-fx-cursor: hand;");

        // Image Preview
        StackPane imagePane = new StackPane();
        imagePane.getStyleClass().add("gig-card-image");
        imagePane.setPrefHeight(180);
        imagePane.setMaxHeight(180);

        if (gig.getImage() != null && !gig.getImage().isEmpty()) {
            try {
                ImageView imageView = new ImageView(new Image(gig.getImage(), 320, 180, false, true));
                imageView.setFitWidth(320);
                imageView.setFitHeight(180);
                imageView.setPreserveRatio(false);
                imagePane.getChildren().add(imageView);
            } catch (Exception e) {
                Label placeholder = new Label("📷");
                placeholder.setStyle("-fx-font-size: 48; -fx-text-fill: #9ca3af;");
                imagePane.getChildren().add(placeholder);
            }
        } else {
            Label placeholder = new Label("📷");
            placeholder.setStyle("-fx-font-size: 48; -fx-text-fill: #9ca3af;");
            imagePane.getChildren().add(placeholder);
        }

        // Content
        VBox content = new VBox(12);
        content.getStyleClass().add("gig-card-content");
        content.setPadding(new Insets(16));

        // Title
        Label title = new Label(gig.getTitle());
        title.getStyleClass().add("gig-card-title");
        title.setWrapText(true);
        title.setMaxHeight(50);

        // Description
        Label description = new Label(gig.getDescription());
        description.getStyleClass().add("gig-card-description");
        description.setWrapText(true);
        description.setMaxHeight(40);

        // Category & Status
        HBox badges = new HBox(8);
        badges.setAlignment(Pos.CENTER_LEFT);

        Label categoryBadge = new Label(gig.getCategory().getName());
        categoryBadge.getStyleClass().add("gig-card-category");

        Label statusBadge = new Label(gig.getStatus());

        // Dynamic styling based on status
        if (gig.getStatus().equalsIgnoreCase("EXPIRED")) {
            statusBadge.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #991b1b; " +
                               "-fx-font-size: 11; -fx-font-weight: 700; -fx-padding: 6 12; " +
                               "-fx-background-radius: 20;");
        } else if (gig.getStatus().equalsIgnoreCase("active")) {
            statusBadge.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #16a34a; " +
                               "-fx-font-size: 11; -fx-font-weight: 700; -fx-padding: 6 12; " +
                               "-fx-background-radius: 20;");
        } else {
            statusBadge.setStyle("-fx-background-color: #fef3c7; -fx-text-fill: #92400e; " +
                               "-fx-font-size: 11; -fx-font-weight: 700; -fx-padding: 6 12; " +
                               "-fx-background-radius: 20;");
        }

        badges.getChildren().addAll(categoryBadge, statusBadge);

        // Price & Actions
        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER_LEFT);

        Label price = new Label(String.format("%.2f TND", gig.getPrice()));
        price.getStyleClass().add("gig-card-price");
        HBox.setHgrow(price, Priority.ALWAYS);

        Button editBtn = new Button("✏️");
        editBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; " +
                        "-fx-padding: 8 12; -fx-background-radius: 6; -fx-cursor: hand;");
        editBtn.setOnAction(e -> showEditGigDialog(gig));

        Button deleteBtn = new Button("🗑️");
        deleteBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; " +
                          "-fx-padding: 8 12; -fx-background-radius: 6; -fx-cursor: hand;");
        deleteBtn.setOnAction(e -> deleteGig(gig));

        // Disable actions for EXPIRED gigs
        if (gig.getStatus().equalsIgnoreCase("EXPIRED")) {
            editBtn.setDisable(true);
            editBtn.setStyle("-fx-background-color: #9ca3af; -fx-text-fill: white; " +
                            "-fx-padding: 8 12; -fx-background-radius: 6; -fx-opacity: 0.5;");
            deleteBtn.setDisable(false); // Allow deletion of expired gigs

            // Add expired tooltip
            Tooltip expiredTooltip = new Tooltip("⚠️ This gig has expired and cannot be edited");
            Tooltip.install(editBtn, expiredTooltip);
        }

        footer.getChildren().addAll(price, editBtn, deleteBtn);

        content.getChildren().addAll(title, description, badges, new Separator(), footer);
        card.getChildren().addAll(imagePane, content);

        return card;
    }

    private void updateStats() {
        if (totalGigsLabel == null || activeGigsLabel == null || totalRevenueLabel == null) {
            System.out.println("Stats labels not yet initialized");
            return;
        }

        totalGigsLabel.setText(String.valueOf(allGigs.size()));

        long activeCount = allGigs.stream()
                .filter(g -> g.getStatus().equalsIgnoreCase("active"))
                .count();
        activeGigsLabel.setText(String.valueOf(activeCount));

        double totalRevenue = allGigs.stream()
                .mapToDouble(GigModel::getPrice)
                .sum();
        totalRevenueLabel.setText(String.format("%.2f TND", totalRevenue));
    }

    // ==================== CRUD OPERATIONS ====================

    @FXML
    public void showAddGigDialog() {
        showGigDialog(null);
    }

    private void showEditGigDialog(GigModel gig) {
        showGigDialog(gig);
    }

    private void showGigDialog(GigModel gigToEdit) {
        Dialog<GigModel> dialog = new Dialog<>();
        dialog.setTitle(gigToEdit == null ? "Add New Gig" : "Edit Gig");
        dialog.setHeaderText(gigToEdit == null ? "Create a new service offering" : "Update your gig");

        // Buttons
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Form
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));
        grid.setStyle("-fx-background-color: white;");

        TextField titleField = new TextField();
        titleField.setPromptText("Enter gig title");
        if (gigToEdit != null) titleField.setText(gigToEdit.getTitle());

        TextArea descField = new TextArea();
        descField.setPromptText("Describe your service...");
        descField.setPrefRowCount(4);
        if (gigToEdit != null) descField.setText(gigToEdit.getDescription());

        TextField priceField = new TextField();
        priceField.setPromptText("Minimum 10.00 DT");
        if (gigToEdit != null) priceField.setText(String.valueOf(gigToEdit.getPrice()));

        // Allow only numeric input with one decimal point
        priceField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*\\.?\\d{0,2}")) {
                priceField.setText(oldVal);
            }
        });

        ComboBox<CategoryModel> categoryCombo = new ComboBox<>();
        categoryCombo.getItems().setAll(categories.subList(1, categories.size())); // Skip "All"
        if (gigToEdit != null && gigToEdit.getCategory() != null) {
            categoryCombo.setValue(gigToEdit.getCategory());
        }

        DatePicker deliveryDatePicker = new DatePicker();
        Spinner<Integer> deliveryHourSpinner = new Spinner<>(0, 23, 12);
        if (gigToEdit != null) {
            deliveryDatePicker.setValue(gigToEdit.getDeliveryTime().toLocalDate());
            deliveryHourSpinner.getValueFactory().setValue(gigToEdit.getDeliveryTime().getHour());
        }

        TextField imageField = new TextField();
        imageField.setPromptText("Image URL or path");
        imageField.setEditable(false);
        if (gigToEdit != null) imageField.setText(gigToEdit.getImage());

        Button browseBtn = new Button("Browse...");
        browseBtn.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Image");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
            );
            File file = fileChooser.showOpenDialog(dialog.getOwner());
            if (file != null) {
                imageField.setText(file.toURI().toString());
            }
        });

        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("active", "inactive");
        statusCombo.setValue(gigToEdit != null ? gigToEdit.getStatus() : "active");

        // Error labels (always visible with min height)
        Label titleError = new Label();
        titleError.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12; -fx-font-weight: 600;");
        titleError.setMinHeight(18);
        titleError.setWrapText(true);

        Label descError = new Label();
        descError.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12; -fx-font-weight: 600;");
        descError.setMinHeight(18);
        descError.setWrapText(true);

        Label priceError = new Label();
        priceError.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12; -fx-font-weight: 600;");
        priceError.setMinHeight(18);
        priceError.setWrapText(true);

        Label categoryError = new Label();
        categoryError.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12; -fx-font-weight: 600;");
        categoryError.setMinHeight(18);
        categoryError.setWrapText(true);

        Label dateError = new Label();
        dateError.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12; -fx-font-weight: 600;");
        dateError.setMinHeight(18);
        dateError.setWrapText(true);

        // Layout
        int row = 0;
        grid.add(new Label("Title:"), 0, row);
        grid.add(titleField, 1, row++);
        grid.add(titleError, 1, row++);

        grid.add(new Label("Description:"), 0, row);
        grid.add(descField, 1, row++);
        grid.add(descError, 1, row++);

        grid.add(new Label("Price (TND):"), 0, row);
        grid.add(priceField, 1, row++);
        grid.add(priceError, 1, row++);

        grid.add(new Label("Category:"), 0, row);
        grid.add(categoryCombo, 1, row++);
        grid.add(categoryError, 1, row++);

        grid.add(new Label("Delivery Date:"), 0, row);
        HBox dateBox = new HBox(10, deliveryDatePicker, new Label("Hour:"), deliveryHourSpinner);
        grid.add(dateBox, 1, row++);
        grid.add(dateError, 1, row++);

        grid.add(new Label("Image:"), 0, row);
        HBox imageBox = new HBox(10, imageField, browseBtn);
        HBox.setHgrow(imageField, Priority.ALWAYS);
        grid.add(imageBox, 1, row++);

        grid.add(new Label("Status:"), 0, row);
        grid.add(statusCombo, 1, row++);

        dialog.getDialogPane().setContent(grid);

        // Disable Save button initially and enable it when form is valid
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);

        // Validation function
        Runnable validateForm = () -> {
            boolean valid = true;
            System.out.println("=== Validating form ===");

            // Clear errors
            titleError.setText("");
            descError.setText("");
            priceError.setText("");
            categoryError.setText("");
            dateError.setText("");

            // Validate Title
            if (titleField.getText().trim().isEmpty()) {
                titleError.setText("✗ Title is required");
                valid = false;
                System.out.println("  ✗ Title empty");
            } else {
                System.out.println("  ✓ Title OK: " + titleField.getText());
            }

            // Validate Description
            if (descField.getText().trim().isEmpty()) {
                descError.setText("✗ Description is required");
                valid = false;
                System.out.println("  ✗ Description empty");
            } else {
                System.out.println("  ✓ Description OK");
            }

            // Validate Price
            try {
                double price = Double.parseDouble(priceField.getText().trim());
                if (price < 10) {
                    priceError.setText("✗ Price must be at least 10 DT");
                    valid = false;
                    System.out.println("  ✗ Price < 10 DT: " + price);
                } else {
                    System.out.println("  ✓ Price OK: " + price);
                }
            } catch (NumberFormatException e) {
                if (!priceField.getText().trim().isEmpty()) {
                    priceError.setText("✗ Invalid price format");
                    System.out.println("  ✗ Invalid price format: " + priceField.getText());
                } else {
                    priceError.setText("✗ Price is required");
                    System.out.println("  ✗ Price empty");
                }
                valid = false;
            }

            // Validate Category
            if (categoryCombo.getValue() == null) {
                categoryError.setText("✗ Please select a category");
                valid = false;
                System.out.println("  ✗ Category not selected");
            } else {
                System.out.println("  ✓ Category OK: " + categoryCombo.getValue().getName());
            }

            // Validate Date
            if (deliveryDatePicker.getValue() == null) {
                dateError.setText("✗ Delivery date is required");
                valid = false;
                System.out.println("  ✗ Date not selected");
            } else {
                LocalDateTime deliveryTime = LocalDateTime.of(
                        deliveryDatePicker.getValue(),
                        java.time.LocalTime.of(deliveryHourSpinner.getValue(), 0)
                );
                if (deliveryTime.isBefore(LocalDateTime.now())) {
                    dateError.setText("✗ Delivery time must be in the future");
                    valid = false;
                    System.out.println("  ✗ Date in past: " + deliveryTime);
                } else {
                    System.out.println("  ✓ Date OK: " + deliveryTime);
                }
            }

            System.out.println("  Form valid: " + valid);
            System.out.println("  Save button disabled: " + !valid);
            saveButton.setDisable(!valid);
        };

        // Add listeners for real-time validation
        titleField.textProperty().addListener((obs, oldVal, newVal) -> validateForm.run());
        descField.textProperty().addListener((obs, oldVal, newVal) -> validateForm.run());
        priceField.textProperty().addListener((obs, oldVal, newVal) -> validateForm.run());
        categoryCombo.valueProperty().addListener((obs, oldVal, newVal) -> validateForm.run());
        deliveryDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> validateForm.run());
        deliveryHourSpinner.valueProperty().addListener((obs, oldVal, newVal) -> validateForm.run());

        // Initial validation
        validateForm.run();

        // Result conversion
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                // Create/Update Gig
                LocalDateTime deliveryTime = LocalDateTime.of(
                        deliveryDatePicker.getValue(),
                        java.time.LocalTime.of(deliveryHourSpinner.getValue(), 0)
                );

                double price = Double.parseDouble(priceField.getText().trim());

                GigModel gig = new GigModel(
                        gigToEdit != null ? gigToEdit.getId() : 0,
                        titleField.getText().trim(),
                        descField.getText().trim(),
                        price,
                        deliveryTime,
                        imageField.getText().trim(),
                        statusCombo.getValue()
                );
                gig.setCategory(categoryCombo.getValue());

                return gig;
            }
            return null;
        });

        Optional<GigModel> result = dialog.showAndWait();
        result.ifPresent(gig -> {
            if (gigToEdit == null) {
                saveGig(gig);
            } else {
                updateGig(gig);
            }
        });
    }

    private void saveGig(GigModel gig) {
        // Insert without user_id
        String query = """
            INSERT INTO gig (title, description, price, delivery_time, image, status, category_id)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setString(1, gig.getTitle());
            ps.setString(2, gig.getDescription());
            ps.setDouble(3, gig.getPrice());
            ps.setTimestamp(4, Timestamp.valueOf(gig.getDeliveryTime()));
            ps.setString(5, gig.getImage());
            ps.setString(6, gig.getStatus());
            ps.setInt(7, gig.getCategory().getId());

            ps.executeUpdate();
            showSuccess("Gig added successfully! 🎉");
            loadGigs();
            updateStats();

        } catch (SQLException e) {
            System.err.println("ERROR saving gig: " + e.getMessage());
            e.printStackTrace();
            showError("Error saving gig: " + e.getMessage());
        }
    }

    private void updateGig(GigModel gig) {
        String query = """
            UPDATE gig
            SET title = ?, description = ?, price = ?, delivery_time = ?,
                image = ?, status = ?, category_id = ?
            WHERE id = ?
        """;

        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setString(1, gig.getTitle());
            ps.setString(2, gig.getDescription());
            ps.setDouble(3, gig.getPrice());
            ps.setTimestamp(4, Timestamp.valueOf(gig.getDeliveryTime()));
            ps.setString(5, gig.getImage());
            ps.setString(6, gig.getStatus());
            ps.setInt(7, gig.getCategory().getId());
            ps.setInt(8, gig.getId());

            ps.executeUpdate();
            showSuccess("Gig updated successfully! ✓");
            loadGigs();
            updateStats();

        } catch (SQLException e) {
            showError("Error updating gig: " + e.getMessage());
        }
    }

    private void deleteGig(GigModel gig) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Gig");
        alert.setHeaderText("Are you sure?");
        alert.setContentText("This will permanently delete \"" + gig.getTitle() + "\"");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try (Connection conn = MyDataBase.getConnection();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM gig WHERE id = ?")) {

                ps.setInt(1, gig.getId());
                ps.executeUpdate();

                showSuccess("Gig deleted successfully! 🗑️");
                loadGigs();
                updateStats();

            } catch (SQLException e) {
                showError("Error deleting gig: " + e.getMessage());
            }
        }
    }

    // ==================== NAVIGATION ====================

    @FXML
    public void onRefresh() {
        loadGigs();
        updateStats();
        showSuccess("Refreshed! ⟳");
    }

    @FXML
    public void onGoBack() {
        try {
            App.setRoot("profile");
        } catch (Exception e) {
            showError("Error navigating to profile: " + e.getMessage());
        }
    }

    @FXML
    public void onExit() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Exit");
        alert.setHeaderText("Are you sure you want to exit?");
        alert.setContentText("You will be logged out.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            Stage stage = (Stage) gigsContainer.getScene().getWindow();
            stage.close();
        }
    }

    // ==================== UTILITIES ====================

    private void showSuccess(String message) {
        if (messageLabel == null) {
            System.out.println("SUCCESS: " + message);
            return;
        }
        messageLabel.setText("✓ " + message);
        messageLabel.setStyle("-fx-background-color: #d1fae5; -fx-text-fill: #065f46; " +
                             "-fx-padding: 12 20; -fx-background-radius: 8; -fx-font-weight: 600;");
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
    }

    private void showError(String message) {
        if (messageLabel == null) {
            System.err.println("ERROR: " + message);
            return;
        }
        messageLabel.setText("✗ " + message);
        messageLabel.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #991b1b; " +
                             "-fx-padding: 12 20; -fx-background-radius: 8; -fx-font-weight: 600;");
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
    }

    private double parseDouble(String value, double defaultValue) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}



























