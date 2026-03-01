package com.khademni.controller;

import com.khademni.App;
import com.khademni.model.CategoryModel;
import com.khademni.model.GigModel;
import com.khademni.utils.MyDataBase;
import com.khademni.utils.FreeAIService;
import com.khademni.utils.FlaskAPIClient;
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

        // Price fields - use TextFormatter instead of setText in listener to avoid IndexOutOfBoundsException
        minPriceField.setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.isEmpty() || newText.matches("\\d*\\.?\\d*")) return change;
            return null;
        }));
        maxPriceField.setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.isEmpty() || newText.matches("\\d*\\.?\\d*")) return change;
            return null;
        }));
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
                // cellFactory: affiche seulement le nom
                javafx.util.Callback<javafx.scene.control.ListView<CategoryModel>, ListCell<CategoryModel>> catCellFact =
                    lv -> new ListCell<CategoryModel>() {
                        @Override protected void updateItem(CategoryModel item, boolean empty) {
                            super.updateItem(item, empty);
                            setText(empty || item == null ? null : item.getName());
                        }
                    };
                filterCategoryComboBox.setCellFactory(catCellFact);
                filterCategoryComboBox.setButtonCell(catCellFact.call(null));
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
        card.setMinWidth(320);
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
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(gigToEdit == null ? "Add New Gig" : "Edit Gig");

        // Scrollable content wrapper
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        // Custom Dialog Content
        VBox dialogContent = new VBox(24);
        dialogContent.setAlignment(Pos.TOP_LEFT);
        dialogContent.setPadding(new Insets(32));
        dialogContent.setStyle(
            "-fx-background-color: white;" +
            "-fx-min-width: 650;" +
            "-fx-max-width: 650;"
        );

        // Header
        VBox headerBox = new VBox(8);
        headerBox.setAlignment(Pos.TOP_LEFT);
        Label titleLabel = new Label(gigToEdit == null ? "💼 Create New Gig" : "✎ Edit Gig");
        titleLabel.setStyle(
            "-fx-font-size: 26;" +
            "-fx-font-weight: 700;" +
            "-fx-text-fill: #1e293b;"
        );

        Label subtitleLabel = new Label(gigToEdit == null ?
            "Create a new service offering for clients to purchase" :
            "Update your gig information");
        subtitleLabel.setStyle(
            "-fx-font-size: 14;" +
            "-fx-text-fill: #64748b;"
        );
        subtitleLabel.setWrapText(true);

        headerBox.getChildren().addAll(titleLabel, subtitleLabel);

        // Form Fields
        VBox formBox = new VBox(20);
        formBox.setAlignment(Pos.TOP_LEFT);
        formBox.setFillWidth(true);

        // Create references for components (will be initialized below)
        final ComboBox<CategoryModel>[] categoryComboRef = new ComboBox[1];
        final TextField[] priceFieldRef = new TextField[1]; // ← Pour accès dans autoPredictPrice

        // Title Field
        VBox titleBox = new VBox(8);
        titleBox.setAlignment(Pos.TOP_LEFT);
        Label titleFieldLabel = new Label("Gig Title *");
        titleFieldLabel.setStyle("-fx-font-size: 13; -fx-font-weight: 600; -fx-text-fill: #374151;");

        TextField titleField = new TextField(gigToEdit != null ? gigToEdit.getTitle() : "");
        titleField.setPromptText("I will create a professional website for you");
        titleField.setStyle(
            "-fx-font-size: 14; -fx-padding: 12 16;" +
            "-fx-background-color: #f9fafb; -fx-border-color: #e5e7eb;" +
            "-fx-border-radius: 10; -fx-background-radius: 10; -fx-border-width: 1.5;"
        );

        Label titleError = new Label();
        titleError.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12;");
        titleError.setVisible(false);
        titleError.setManaged(false);

        titleBox.getChildren().addAll(titleFieldLabel, titleField, titleError);

        // Description Field
        VBox descBox = new VBox(8);
        descBox.setAlignment(Pos.TOP_LEFT);
        Label descFieldLabel = new Label("Description *");
        descFieldLabel.setStyle("-fx-font-size: 13; -fx-font-weight: 600; -fx-text-fill: #374151;");

        TextArea descField = new TextArea(gigToEdit != null ? gigToEdit.getDescription() : "");
        descField.setPromptText("Describe your service in detail...");
        descField.setPrefRowCount(4);
        descField.setWrapText(true);
        descField.setStyle(
            "-fx-font-size: 14; -fx-padding: 12 16;" +
            "-fx-background-color: #f9fafb; -fx-border-color: #e5e7eb;" +
            "-fx-border-radius: 10; -fx-background-radius: 10; -fx-border-width: 1.5;"
        );

        // Generate Description Button
        Button generateDescBtn = new Button("✨ Generate Description with AI");
        generateDescBtn.setStyle(
            "-fx-background-color: linear-gradient(to right, #10b981, #059669);" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 13;" +
            "-fx-font-weight: 600;" +
            "-fx-padding: 10 20;" +
            "-fx-background-radius: 8;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(16,185,129,0.3), 8, 0, 0, 2);"
        );
        generateDescBtn.setMaxWidth(Double.MAX_VALUE);
        generateDescBtn.setOnAction(e -> {
            String title = titleField.getText().trim();
            if (title.isEmpty()) {
                showError("Please enter a gig title first!");
                return;
            }

            String categoryName = "General";
            if (categoryComboRef[0] != null && categoryComboRef[0].getValue() != null) {
                categoryName = categoryComboRef[0].getValue().getName();
            }

            generateDescBtn.setDisable(true);
            generateDescBtn.setText("⏳ Generating with AI...");

            final String finalCategoryName = categoryName;

            // Run in background thread
            new Thread(() -> {
                try {
                    System.out.println("🚀 Starting FREE AI generation for gig...");
                    System.out.println("   Title: " + title);
                    System.out.println("   Category: " + finalCategoryName);

                    // Utilise le service AI GRATUIT (pas besoin de quota/crédit)
                    com.khademni.utils.FreeAIService freeAI = new com.khademni.utils.FreeAIService();
                    String generatedDesc = freeAI.generateGigDescription(title, finalCategoryName);

                    System.out.println("✅ AI generation completed!");

                    // Update UI on JavaFX thread
                    javafx.application.Platform.runLater(() -> {
                        descField.setText(generatedDesc);
                        generateDescBtn.setDisable(false);
                        generateDescBtn.setText("✨ Generate Description with AI");
                        showSuccess("Description generated with FREE AI! ✨");
                    });
                } catch (Exception ex) {
                    System.err.println("❌ Error during AI generation:");
                    ex.printStackTrace();

                    javafx.application.Platform.runLater(() -> {
                        generateDescBtn.setDisable(false);
                        generateDescBtn.setText("✨ Generate Description with AI");
                        showError("Error generating description: " + ex.getMessage());
                    });
                }
            }).start();
        });

        Label descError = new Label();
        descError.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12;");
        descError.setVisible(false);
        descError.setManaged(false);

        descBox.getChildren().addAll(descFieldLabel, descField, generateDescBtn, descError);

        // Price & Category Row
        HBox priceAndCategoryBox = new HBox(16);
        priceAndCategoryBox.setAlignment(Pos.TOP_LEFT);

        // Price Field
        VBox priceBox = new VBox(8);
        priceBox.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(priceBox, Priority.ALWAYS);
        Label priceFieldLabel = new Label("Price (TND) *");
        priceFieldLabel.setStyle("-fx-font-size: 13; -fx-font-weight: 600; -fx-text-fill: #374151;");

        TextField priceField = new TextField(gigToEdit != null ? String.valueOf(gigToEdit.getPrice()) : "");
        priceFieldRef[0] = priceField; // ← Stocker la référence pour autoPredictPrice
        priceField.setPromptText("Minimum 10.00 DT");
        priceField.setStyle(
            "-fx-font-size: 14; -fx-padding: 12 16;" +
            "-fx-background-color: #f9fafb; -fx-border-color: #e5e7eb;" +
            "-fx-border-radius: 10; -fx-background-radius: 10; -fx-border-width: 1.5;"
        );
        // Flag thread-safe pour désactiver le listener pendant l'auto-remplissage par IA
        final java.util.concurrent.atomic.AtomicBoolean aiSettingPrice = new java.util.concurrent.atomic.AtomicBoolean(false);
        // Use TextFormatter to safely filter input without recursive setText calls
        priceField.setTextFormatter(new TextFormatter<>(change -> {
            if (aiSettingPrice.get()) return change; // Allow AI to set any value
            String newText = change.getControlNewText();
            if (newText.isEmpty() || newText.matches("\\d*\\.?\\d*")) {
                return change; // Accept valid input
            }
            return null; // Reject invalid input
        }));

        Label priceError = new Label();
        priceError.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12;");
        priceError.setVisible(false);
        priceError.setManaged(false);

        // 🤖 Label hint IA (affiché sous le champ prix quand l'IA suggère un prix)
        Label aiPriceHint = new Label();
        aiPriceHint.setStyle(
            "-fx-text-fill: #16a34a; -fx-font-size: 12; -fx-font-weight: 600;" +
            "-fx-background-color: #f0fdf4; -fx-padding: 4 8;" +
            "-fx-background-radius: 6; -fx-border-color: #bbf7d0; -fx-border-radius: 6;"
        );
        aiPriceHint.setVisible(false);
        aiPriceHint.setManaged(false);
        aiPriceHint.setWrapText(true);

        priceBox.getChildren().addAll(priceFieldLabel, priceField, aiPriceHint, priceError);

        // Category Field
        VBox categoryBox = new VBox(8);
        categoryBox.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(categoryBox, Priority.ALWAYS);
        Label categoryFieldLabel = new Label("Category *");
        categoryFieldLabel.setStyle("-fx-font-size: 13; -fx-font-weight: 600; -fx-text-fill: #374151;");

        ComboBox<CategoryModel> categoryCombo = new ComboBox<>();
        categoryComboRef[0] = categoryCombo; // Store reference for generate button
        categoryCombo.setMaxWidth(Double.MAX_VALUE);
        categoryCombo.setPromptText("Select a category");
        List<CategoryModel> filteredCats = categories.stream().filter(c -> c.getId() != 0).toList();
        categoryCombo.getItems().setAll(filteredCats);

        // cellFactory: affiche seulement le nom
        javafx.util.Callback<javafx.scene.control.ListView<CategoryModel>, ListCell<CategoryModel>> cellFactory =
            lv -> new ListCell<CategoryModel>() {
                @Override protected void updateItem(CategoryModel item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getName());
                }
            };
        categoryCombo.setCellFactory(cellFactory);
        categoryCombo.setButtonCell(cellFactory.call(null));

        // Sélection en mode édition: matcher par ID
        if (gigToEdit != null && gigToEdit.getCategory() != null) {
            final int editCatId = gigToEdit.getCategory().getId();
            filteredCats.stream()
                .filter(c -> c.getId() == editCatId)
                .findFirst()
                .ifPresent(categoryCombo::setValue);
        }
        categoryCombo.setStyle(
            "-fx-background-color: #f9fafb; -fx-border-color: #e5e7eb;" +
            "-fx-border-radius: 10; -fx-background-radius: 10;"
        );

        Label categoryError = new Label();
        categoryError.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12;");
        categoryError.setVisible(false);
        categoryError.setManaged(false);

        categoryBox.getChildren().addAll(categoryFieldLabel, categoryCombo, categoryError);

        // Référence pour DatePicker (déclaré avant autoPredictPrice car autoPredictPrice en a besoin)
        final DatePicker[] deliveryDatePickerRef = new DatePicker[1];

        // 💰 AUTO-SUGGESTION DE PRIX: Prédiction basée sur catégorie + description
        // Définit le Runnable APRÈS l'initialisation de priceField et categoryCombo
        final Label[] aiPriceHintRef = {aiPriceHint};
        Runnable autoPredictPrice = () -> {
            String desc = descField.getText().trim();
            CategoryModel selectedCategory = categoryComboRef[0].getValue();

            if (selectedCategory != null && desc.length() >= 10) {
                System.out.println("💰 Prédiction automatique de prix...");
                System.out.println("   Catégorie DB: " + selectedCategory.getName());

                // Mapper le nom DB -> nom ML model
                String dbCatName = selectedCategory.getName().toLowerCase();
                String mlCategory;
                if (dbCatName.contains("dev") || dbCatName.contains("code") || dbCatName.contains("program")
                    || dbCatName.contains("web") || dbCatName.contains("app") || dbCatName.contains("software")
                    || dbCatName.contains("deploy") || dbCatName.contains("automati") || dbCatName.contains("backend")
                    || dbCatName.contains("frontend") || dbCatName.contains("full")) {
                    mlCategory = "Development";
                } else if (dbCatName.contains("design") || dbCatName.contains("graphic") || dbCatName.contains("logo")
                    || dbCatName.contains("ui") || dbCatName.contains("ux") || dbCatName.contains("photo")) {
                    mlCategory = "Design";
                } else if (dbCatName.contains("market") || dbCatName.contains("seo") || dbCatName.contains("social")
                    || dbCatName.contains("ads") || dbCatName.contains("campaign")) {
                    mlCategory = "Marketing";
                } else if (dbCatName.contains("writ") || dbCatName.contains("blog") || dbCatName.contains("content")
                    || dbCatName.contains("copy") || dbCatName.contains("article")) {
                    mlCategory = "Writing";
                } else if (dbCatName.contains("video") || dbCatName.contains("film") || dbCatName.contains("animat")
                    || dbCatName.contains("motion") || dbCatName.contains("edit")) {
                    mlCategory = "Video";
                } else if (dbCatName.contains("game") || dbCatName.contains("unity") || dbCatName.contains("unreal")) {
                    mlCategory = "Development";
                } else if (dbCatName.contains("n8n") || dbCatName.contains("workflow") || dbCatName.contains("operation")) {
                    mlCategory = "Development";
                } else {
                    mlCategory = "Development"; // par défaut
                }

                System.out.println("   Catégorie ML: " + mlCategory);
                System.out.println("   Description length: " + desc.length());

                // ─── Références capturées (JavaFX thread, pas besoin de Platform.runLater ici) ───
                final Label hintLabel = aiPriceHintRef[0];
                final String finalMlCategory = mlCategory;
                final int descLen = desc.length();

                // Calculer le nombre de jours depuis le DatePicker (sur JavaFX thread)
                final int deliveryDays;
                if (deliveryDatePickerRef[0] != null && deliveryDatePickerRef[0].getValue() != null) {
                    long days = java.time.temporal.ChronoUnit.DAYS.between(
                        java.time.LocalDate.now(), deliveryDatePickerRef[0].getValue());
                    deliveryDays = (int) Math.max(1, days);
                } else {
                    deliveryDays = 7;
                }
                System.out.println("   Delivery days: " + deliveryDays);

                // ─── ÉTAPE 1 : Prix local INSTANTANÉ (JavaFX thread, pas de délai) ─────────────
                double localPrice = Math.max(computeLocalPrice(finalMlCategory, descLen, deliveryDays), 10.0);
                System.out.println("💵 Prix local instantané: " + localPrice + " TND → affiché immédiatement");
                if (priceFieldRef[0] != null) {
                    aiSettingPrice.set(true);
                    priceFieldRef[0].setText(String.format(java.util.Locale.US, "%.2f", localPrice));
                    aiSettingPrice.set(false);
                    priceFieldRef[0].setStyle(
                        "-fx-font-size: 14; -fx-padding: 12 16;" +
                        "-fx-background-color: #f0fdf4; -fx-border-color: #22c55e;" +
                        "-fx-border-radius: 10; -fx-background-radius: 10; -fx-border-width: 2;"
                    );
                }
                if (hintLabel != null) {
                    hintLabel.setText(String.format("🤖 AI estimate: %.2f TND (refining...)", localPrice));
                    hintLabel.setVisible(true);
                    hintLabel.setManaged(true);
                }

                // ─── ÉTAPE 2 : Raffiner avec l'API Flask en background ───────────────────────
                final double localPriceFinal = localPrice;
                new Thread(() -> {
                    try {
                        double apiPrice = FlaskAPIClient.predictPrice(finalMlCategory, descLen, deliveryDays);
                        // Si le modèle Flask retourne un prix fixe suspect (modèle non entraîné), garder le local
                        if (Math.abs(apiPrice - 1523.55) < 0.5) {
                            System.out.println("⚠️  Prix Flask fixe détecté → prix local conservé: " + localPriceFinal);
                            javafx.application.Platform.runLater(() -> {
                                if (hintLabel != null) {
                                    hintLabel.setText(String.format("🤖 AI (local estimate): %.2f TND", localPriceFinal));
                                    hintLabel.setVisible(true);
                                    hintLabel.setManaged(true);
                                }
                            });
                        } else {
                            final double refinedPrice = Math.max(apiPrice, 10.0);
                            System.out.println("💵 Prix raffiné API Flask: " + refinedPrice + " TND");
                            javafx.application.Platform.runLater(() -> {
                                if (priceFieldRef[0] != null) {
                                    aiSettingPrice.set(true);
                                    priceFieldRef[0].setText(String.format(java.util.Locale.US, "%.2f", refinedPrice));
                                    aiSettingPrice.set(false);
                                    priceFieldRef[0].setStyle(
                                        "-fx-font-size: 14; -fx-padding: 12 16;" +
                                        "-fx-background-color: #ecfdf5; -fx-border-color: #10b981;" +
                                        "-fx-border-radius: 10; -fx-background-radius: 10; -fx-border-width: 2;"
                                    );
                                    System.out.println("✅ Prix ML affiché dans l'interface: " + refinedPrice + " TND");
                                }
                                if (hintLabel != null) {
                                    hintLabel.setText(String.format("✅ AI (ML model): %.2f TND", refinedPrice));
                                    hintLabel.setVisible(true);
                                    hintLabel.setManaged(true);
                                }
                            });
                        }
                    } catch (Exception apiEx) {
                        System.out.println("⚠️  API Flask indisponible → prix local conservé: " + localPriceFinal);
                        javafx.application.Platform.runLater(() -> {
                            if (hintLabel != null) {
                                hintLabel.setText(String.format("🤖 AI (local estimate): %.2f TND", localPriceFinal));
                                hintLabel.setVisible(true);
                                hintLabel.setManaged(true);
                            }
                        });
                    }
                }).start();
            }
        };

        // 🤖 AUTO-CLASSIFICATION: Prédiction de catégorie basée sur le titre
        // Définit le Runnable APRÈS l'initialisation de categoryComboRef
        Runnable autoPredictCategory = () -> {
            String title = titleField.getText().trim();
            String desc = descField.getText().trim();

            // Fonctionne avec titre seul (5+ caractères) OU titre + description
            if (!title.isEmpty() && title.length() >= 5) {
                System.out.println("🤖 Classification automatique de catégorie...");
                System.out.println("   Titre: " + title);
                if (!desc.isEmpty()) {
                    System.out.println("   Description: " + desc);
                }

                try {
                    FreeAIService freeAI = new FreeAIService();
                    String predicted = freeAI.predictCategory(title, desc);

                    if (predicted != null && categoryComboRef[0] != null) {
                        System.out.println("🎯 Catégorie prédite: " + predicted);

                        // Trouve la catégorie par correspondance flexible
                        CategoryModel matchedCategory = null;

                        // 1. Essai correspondance exacte (ignore case)
                        for (CategoryModel cat : categoryComboRef[0].getItems()) {
                            if (cat.getName().equalsIgnoreCase(predicted)) {
                                matchedCategory = cat;
                                System.out.println("   ✓ Correspondance exacte: " + cat.getName());
                                break;
                            }
                        }

                        // 2. Si pas trouvé, essai correspondance partielle (basé sur dataset réel)
                        if (matchedCategory == null) {
                            String predictedLower = predicted.toLowerCase();
                            for (CategoryModel cat : categoryComboRef[0].getItems()) {
                                String catNameLower = cat.getName().toLowerCase();

                                // Correspondance intelligente basée sur les 5 catégories principales du dataset
                                boolean matches = false;

                                // DESIGN (18 exemples dans dataset)
                                if (predictedLower.contains("design") && catNameLower.contains("design")) matches = true;
                                if (predictedLower.contains("graphic") && catNameLower.contains("graph")) matches = true;
                                if (predictedLower.contains("logo") && catNameLower.contains("design")) matches = true;
                                if (predictedLower.contains("ui") && catNameLower.contains("design")) matches = true;

                                // DEVELOPMENT (15 exemples dans dataset)
                                if (predictedLower.contains("development") && catNameLower.contains("dev")) matches = true;
                                if (predictedLower.contains("web") && catNameLower.contains("web")) matches = true;
                                if (predictedLower.contains("front") && catNameLower.contains("front")) matches = true;
                                if (predictedLower.contains("back") && catNameLower.contains("back")) matches = true;
                                if (predictedLower.contains("mobile") && catNameLower.contains("mobile")) matches = true;
                                if (predictedLower.contains("app") && catNameLower.contains("dev")) matches = true;

                                // MARKETING (9 exemples dans dataset)
                                if (predictedLower.contains("marketing") && catNameLower.contains("market")) matches = true;
                                if (predictedLower.contains("seo") && catNameLower.contains("market")) matches = true;
                                if (predictedLower.contains("social") && catNameLower.contains("market")) matches = true;

                                // WRITING (7 exemples dans dataset)
                                if (predictedLower.contains("writing") && catNameLower.contains("writ")) matches = true;
                                if (predictedLower.contains("content") && catNameLower.contains("writ")) matches = true;

                                // VIDEO (9 exemples dans dataset)
                                if (predictedLower.contains("video") && catNameLower.contains("video")) matches = true;
                                if (predictedLower.contains("animation") && catNameLower.contains("video")) matches = true;
                                if (predictedLower.contains("editing") && catNameLower.contains("video")) matches = true;

                                // AUTOMATION/DATA (bonus)
                                if (predictedLower.contains("automation") && catNameLower.contains("autom")) matches = true;
                                if (predictedLower.contains("data") && catNameLower.contains("data")) matches = true;

                                if (matches) {
                                    matchedCategory = cat;
                                    System.out.println("   ✓ Correspondance partielle: " + cat.getName() + " ≈ " + predicted);
                                    break;
                                }
                            }
                        }

                        // 3. Sélectionne la catégorie trouvée
                        if (matchedCategory != null) {
                            CategoryModel finalCat = matchedCategory;
                            javafx.application.Platform.runLater(() -> {
                                categoryComboRef[0].setValue(finalCat);
                                System.out.println("✅ Catégorie auto-sélectionnée: " + finalCat.getName());
                                showSuccess("🤖 Catégorie suggérée: " + finalCat.getName());
                            });
                        } else {
                            System.out.println("⚠️  Catégorie '" + predicted + "' non trouvée dans la liste");
                            System.out.println("   Catégories disponibles:");
                            for (CategoryModel cat : categoryComboRef[0].getItems()) {
                                System.out.println("   - " + cat.getName());
                            }
                        }
                    } else {
                        if (predicted == null) {
                            System.out.println("⚠️  Aucune catégorie détectée (mots-clés insuffisants)");
                        }
                    }
                } catch (Exception e) {
                    System.err.println("❌ Erreur lors de la classification: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        };

        // Écoute les changements de titre (délai via Timer, compatible Dialog)
        final java.util.concurrent.atomic.AtomicReference<java.util.Timer> titleTimerRef =
            new java.util.concurrent.atomic.AtomicReference<>(null);
        titleField.textProperty().addListener((obs, old, newVal) -> {
            if (newVal != null && newVal.trim().length() >= 5) {
                java.util.Timer oldT = titleTimerRef.getAndSet(null);
                if (oldT != null) oldT.cancel();
                java.util.Timer t = new java.util.Timer("cat-title", true);
                titleTimerRef.set(t);
                t.schedule(new java.util.TimerTask() {
                    @Override public void run() {
                        javafx.application.Platform.runLater(autoPredictCategory);
                    }
                }, 800L);
            }
        });

        // Écoute les changements de description (délai via Timer, compatible Dialog)
        final java.util.concurrent.atomic.AtomicReference<java.util.Timer> descTimerRef =
            new java.util.concurrent.atomic.AtomicReference<>(null);

        // 💰 Timer Java pour prédiction prix (plus fiable dans Dialog que PauseTransition)
        final java.util.concurrent.atomic.AtomicReference<java.util.Timer> priceTimerRef =
            new java.util.concurrent.atomic.AtomicReference<>(null);

        // Helper pour déclencher la prédiction de prix avec délai
        final Runnable schedulePricePrediction = () -> {
            java.util.Timer old2 = priceTimerRef.getAndSet(null);
            if (old2 != null) old2.cancel();
            java.util.Timer t = new java.util.Timer("price-predict", true);
            priceTimerRef.set(t);
            t.schedule(new java.util.TimerTask() {
                @Override public void run() {
                    javafx.application.Platform.runLater(autoPredictPrice);
                }
            }, 1000L);
        };

        descField.textProperty().addListener((obs, old, newVal) -> {
            if (titleField.getText() != null && titleField.getText().trim().length() >= 5) {
                java.util.Timer oldD = descTimerRef.getAndSet(null);
                if (oldD != null) oldD.cancel();
                java.util.Timer dt = new java.util.Timer("cat-desc", true);
                descTimerRef.set(dt);
                dt.schedule(new java.util.TimerTask() {
                    @Override public void run() {
                        javafx.application.Platform.runLater(autoPredictCategory);
                    }
                }, 800L);
            }

            // 💰 Déclenche aussi la prédiction de prix si conditions remplies
            if (newVal != null && newVal.trim().length() >= 10 && categoryComboRef[0] != null && categoryComboRef[0].getValue() != null) {
                schedulePricePrediction.run();
            }
        });

        // 💰 Écoute les changements de catégorie pour la prédiction de prix
        categoryComboRef[0].valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && descField.getText() != null && descField.getText().trim().length() >= 10) {
                schedulePricePrediction.run();
            }
        });

        priceAndCategoryBox.getChildren().addAll(priceBox, categoryBox);

        // Delivery Date & Time
        VBox deliveryBox = new VBox(8);
        deliveryBox.setAlignment(Pos.TOP_LEFT);
        Label deliveryFieldLabel = new Label("Delivery Date & Time *");
        deliveryFieldLabel.setStyle("-fx-font-size: 13; -fx-font-weight: 600; -fx-text-fill: #374151;");

        HBox dateTimeBox = new HBox(12);
        dateTimeBox.setAlignment(Pos.CENTER_LEFT);
        DatePicker deliveryDatePicker = new DatePicker();
        deliveryDatePicker.setPromptText("Select date");
        deliveryDatePickerRef[0] = deliveryDatePicker; // Stocker référence pour autoPredictPrice
        if (gigToEdit != null) {
            deliveryDatePicker.setValue(gigToEdit.getDeliveryTime().toLocalDate());
        }
        deliveryDatePicker.setStyle("-fx-background-color: #f9fafb; -fx-border-color: #e5e7eb; -fx-border-radius: 10; -fx-background-radius: 10;");
        HBox.setHgrow(deliveryDatePicker, Priority.ALWAYS);
        // Listener: re-déclenche la prédiction de prix si desc + catégorie déjà remplis
        deliveryDatePicker.valueProperty().addListener((obs, oldDate, newDate) -> {
            if (newDate != null && categoryComboRef[0] != null
                    && categoryComboRef[0].getValue() != null
                    && descField.getText() != null
                    && descField.getText().trim().length() >= 10) {
                schedulePricePrediction.run();
            }
        });

        Spinner<Integer> deliveryHourSpinner = new Spinner<>(0, 23, 12);
        deliveryHourSpinner.setPrefWidth(100);
        if (gigToEdit != null) {
            deliveryHourSpinner.getValueFactory().setValue(gigToEdit.getDeliveryTime().getHour());
        }
        deliveryHourSpinner.setStyle("-fx-background-color: #f9fafb;");

        Label hourLabel = new Label("h");
        hourLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #6b7280;");

        dateTimeBox.getChildren().addAll(deliveryDatePicker, deliveryHourSpinner, hourLabel);

        Label dateError = new Label();
        dateError.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12;");
        dateError.setVisible(false);
        dateError.setManaged(false);

        deliveryBox.getChildren().addAll(deliveryFieldLabel, dateTimeBox, dateError);

        // Image Upload
        VBox imageBox = new VBox(8);
        imageBox.setAlignment(Pos.TOP_LEFT);
        Label imageFieldLabel = new Label("Image");
        imageFieldLabel.setStyle("-fx-font-size: 13; -fx-font-weight: 600; -fx-text-fill: #374151;");

        TextField imageField = new TextField(gigToEdit != null ? gigToEdit.getImage() : "");
        imageField.setPromptText("Image path or URL");
        imageField.setEditable(false);
        imageField.setStyle(
            "-fx-font-size: 14; -fx-padding: 12 16;" +
            "-fx-background-color: #f9fafb; -fx-border-color: #e5e7eb;" +
            "-fx-border-radius: 10; -fx-background-radius: 10; -fx-border-width: 1.5;"
        );

        Button browseBtn = new Button("📁 Browse Files");
        browseBtn.setStyle(
            "-fx-background-color: #f3f4f6; -fx-text-fill: #374151;" +
            "-fx-font-size: 13; -fx-font-weight: 600; -fx-padding: 10 20;" +
            "-fx-background-radius: 8; -fx-cursor: hand;"
        );
        browseBtn.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Gig Image");
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
            );
            File file = fileChooser.showOpenDialog(dialog.getOwner());
            if (file != null) {
                imageField.setText(file.toURI().toString());
            }
        });

        HBox imageBrowseBox = new HBox(12, imageField, browseBtn);
        imageBrowseBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(imageField, Priority.ALWAYS);

        imageBox.getChildren().addAll(imageFieldLabel, imageBrowseBox);

        // Status
        VBox statusBox = new VBox(8);
        statusBox.setAlignment(Pos.TOP_LEFT);
        Label statusFieldLabel = new Label("Status");
        statusFieldLabel.setStyle("-fx-font-size: 13; -fx-font-weight: 600; -fx-text-fill: #374151;");

        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("active", "inactive");
        statusCombo.setValue(gigToEdit != null ? gigToEdit.getStatus() : "active");
        statusCombo.setMaxWidth(200);
        statusCombo.setStyle("-fx-background-color: #f9fafb; -fx-border-color: #e5e7eb; -fx-border-radius: 10; -fx-background-radius: 10;");

        statusBox.getChildren().addAll(statusFieldLabel, statusCombo);

        // Validation Function (must be defined BEFORE adding listeners)
        Runnable validateForm = () -> {
            boolean titleValid = true;
            boolean descValid = true;
            boolean priceValid = true;
            boolean categoryValid = true;
            boolean dateValid = true;

            // Title
            if (titleField.getText().trim().isEmpty()) {
                titleError.setText("✗ Title is required");
                titleError.setVisible(true);
                titleError.setManaged(true);
                titleValid = false;
            } else {
                titleError.setVisible(false);
                titleError.setManaged(false);
            }

            // Description
            if (descField.getText().trim().isEmpty()) {
                descError.setText("✗ Description is required");
                descError.setVisible(true);
                descError.setManaged(true);
                descValid = false;
            } else {
                descError.setVisible(false);
                descError.setManaged(false);
            }

            // Price
            try {
                double price = Double.parseDouble(priceField.getText().trim().replace(',', '.'));
                if (price < 10.0) {
                    priceError.setText("✗ Minimum price is 10.00 TND");
                    priceError.setVisible(true);
                    priceError.setManaged(true);
                    priceValid = false;
                } else {
                    priceError.setVisible(false);
                    priceError.setManaged(false);
                }
            } catch (NumberFormatException e) {
                priceError.setText("✗ Invalid price format");
                priceError.setVisible(true);
                priceError.setManaged(true);
                priceValid = false;
            }

            // Category
            if (categoryCombo.getValue() == null) {
                categoryError.setText("✗ Category is required");
                categoryError.setVisible(true);
                categoryError.setManaged(true);
                categoryValid = false;
            } else {
                categoryError.setVisible(false);
                categoryError.setManaged(false);
            }

            // Date
            if (deliveryDatePicker.getValue() == null) {
                dateError.setText("✗ Delivery date is required");
                dateError.setVisible(true);
                dateError.setManaged(true);
                dateValid = false;
            } else if (deliveryDatePicker.getValue().isBefore(java.time.LocalDate.now())) {
                dateError.setText("✗ Delivery date must be in the future");
                dateError.setVisible(true);
                dateError.setManaged(true);
                dateValid = false;
            } else {
                dateError.setVisible(false);
                dateError.setManaged(false);
            }
        };

        // Add listeners
        titleField.textProperty().addListener((obs, old, newVal) -> validateForm.run());
        descField.textProperty().addListener((obs, old, newVal) -> validateForm.run());
        priceField.textProperty().addListener((obs, old, newVal) -> validateForm.run());
        categoryComboRef[0].valueProperty().addListener((obs, old, newVal) -> validateForm.run());
        deliveryDatePicker.valueProperty().addListener((obs, old, newVal) -> validateForm.run());

        // Assemble form (AFTER validation is defined)
        formBox.getChildren().addAll(titleBox, descBox, priceAndCategoryBox, deliveryBox, imageBox, statusBox);

        // Separator
        Separator separator = new Separator();
        separator.setStyle("-fx-background-color: #e5e7eb;");

        // Action Buttons
        HBox buttonBox = new HBox(12);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle(
            "-fx-background-color: #f3f4f6; -fx-text-fill: #374151;" +
            "-fx-font-size: 14; -fx-font-weight: 600; -fx-padding: 12 32;" +
            "-fx-background-radius: 10; -fx-cursor: hand;"
        );
        cancelBtn.setOnAction(e -> {
            dialog.setResult(ButtonType.CANCEL);
            dialog.close();
        });

        Button saveBtn = new Button(gigToEdit == null ? "💼 Create Gig" : "💾 Save Changes");
        saveBtn.setStyle(
            "-fx-background-color: linear-gradient(to bottom right, #6366f1, #8b5cf6);" +
            "-fx-text-fill: white; -fx-font-size: 14; -fx-font-weight: 700;" +
            "-fx-padding: 12 32; -fx-background-radius: 10; -fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(99,102,241,0.3), 12, 0, 0, 4);"
        );
        saveBtn.setOnAction(e -> {
            validateForm.run();

            String title = titleField.getText().trim();
            String desc = descField.getText().trim();
            String priceStr = priceField.getText().trim();
            CategoryModel category = categoryCombo.getValue();
            java.time.LocalDate date = deliveryDatePicker.getValue();
            int hour = deliveryHourSpinner.getValue();
            String image = imageField.getText().trim();
            String status = statusCombo.getValue();

            if (!title.isEmpty() && !desc.isEmpty() && category != null && date != null) {
                try {
                    double price = Double.parseDouble(priceStr.replace(',', '.'));
                    if (price >= 10.0 && !date.isBefore(java.time.LocalDate.now())) {
                        LocalDateTime deliveryTime = LocalDateTime.of(date, java.time.LocalTime.of(hour, 0));

                        if (saveGig(gigToEdit, title, desc, price, deliveryTime, image, status, category.getId())) {
                            dialog.setResult(ButtonType.OK);
                            dialog.close();
                            loadGigs();
                            updateStats();
                        }
                    }
                } catch (NumberFormatException ex) {
                    // Invalid price format already handled
                }
            }
        });

        buttonBox.getChildren().addAll(cancelBtn, saveBtn);

        // Assemble Dialog
        dialogContent.getChildren().addAll(headerBox, formBox, separator, buttonBox);
        scrollPane.setContent(dialogContent);

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setContent(scrollPane);
        dialogPane.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 16;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 40, 0, 0, 10);"
        );

        // Remove default buttons
        dialogPane.getButtonTypes().clear();

        // 🚀 En mode édition : déclencher la prédiction de prix immédiatement
        // (catégorie + description déjà remplis via gigToEdit)
        if (gigToEdit != null) {
            final Runnable spp = schedulePricePrediction;
            javafx.application.Platform.runLater(() -> {
                if (descField.getText() != null && descField.getText().trim().length() >= 10
                        && categoryComboRef[0] != null && categoryComboRef[0].getValue() != null) {
                    spp.run();
                }
            });
        }

        dialog.showAndWait();
    }

    private boolean saveGig(GigModel gigToEdit, String title, String desc, double price, LocalDateTime deliveryTime,
                           String image, String status, int categoryId) {
        String query;

        if (gigToEdit == null) {
            // Create new gig
            query = """
                INSERT INTO gig (title, description, price, delivery_time, image, status, category_id)
                VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        } else {
            // Update existing gig
            query = """
                UPDATE gig 
                SET title = ?, description = ?, price = ?, delivery_time = ?, image = ?, status = ?, category_id = ?
                WHERE id = ?
            """;
        }

        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setString(1, title);
            ps.setString(2, desc);
            ps.setDouble(3, price);
            ps.setTimestamp(4, Timestamp.valueOf(deliveryTime));
            ps.setString(5, image.isEmpty() ? null : image);
            ps.setString(6, status);
            ps.setInt(7, categoryId);

            if (gigToEdit != null) {
                ps.setInt(8, gigToEdit.getId());
            }

            ps.executeUpdate();

            if (gigToEdit == null) {
                showSuccess("✅ Gig created successfully! 🎉");
            } else {
                showSuccess("✅ Gig updated successfully! 💾");
            }

            return true;

        } catch (SQLException e) {
            System.err.println("ERROR saving gig: " + e.getMessage());
            e.printStackTrace();
            showError("❌ Error saving gig: " + e.getMessage());
            return false;
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

    /**
     * Calcul local du prix si l'API Flask est indisponible.
     * Basé sur des règles simples: catégorie + longueur description + jours livraison.
     */
    private double computeLocalPrice(String mlCategory, int descLen, int deliveryDays) {
        // Base price par catégorie (tunisian freelance market)
        double base;
        double perChar;
        double perDay;
        switch (mlCategory) {
            case "Development": base = 300; perChar = 2.5; perDay = 40; break;
            case "Design":      base = 100; perChar = 1.2; perDay = 25; break;
            case "Marketing":   base = 150; perChar = 1.5; perDay = 30; break;
            case "Video":       base = 120; perChar = 1.3; perDay = 35; break;
            case "Writing":     base =  50; perChar = 0.8; perDay = 10; break;
            default:            base = 200; perChar = 1.5; perDay = 30; break;
        }
        // Formule: base + longueur × perChar + jours × perDay
        double price = base + (perChar * descLen) + (perDay * deliveryDays);
        // Arrondir à 2 décimales
        price = Math.round(price * 100.0) / 100.0;
        System.out.println("   [Local] cat=" + mlCategory
            + " base=" + base
            + " descLen=" + descLen + " (+" + String.format("%.0f", perChar * descLen) + ")"
            + " days=" + deliveryDays + " (+" + String.format("%.0f", perDay * deliveryDays) + ")"
            + " → prix=" + String.format("%.2f", price) + " TND");
        return price;
    }
}





