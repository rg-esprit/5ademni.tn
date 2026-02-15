package com.khademni.controller;

import com.khademni.App;
import com.khademni.model.GigModel;
import com.khademni.model.CategoryModel;
import com.khademni.utils.MyDataBase;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class GigController {

    // ===== FIELDS =====
    @FXML private TextField titleField;
    @FXML private TextArea descriptionArea;
    @FXML private TextField priceField;
    @FXML private DatePicker deliveryDatePicker;
    @FXML private Spinner<Integer> deliveryHourSpinner;
    @FXML private Spinner<Integer> deliveryMinuteSpinner;
    @FXML private TextField imageField;
    @FXML private Button selectImageButton;

    @FXML private ComboBox<CategoryModel> categoryComboBox;
    @FXML private ComboBox<String> statusComboBox;

    @FXML private TableView<GigModel> gigTable;
    @FXML private TableColumn<GigModel, Integer> colId;
    @FXML private TableColumn<GigModel, String> colTitle;
    @FXML private TableColumn<GigModel, Double> colPrice;
    @FXML private TableColumn<GigModel, String> colStatus;
    @FXML private TableColumn<GigModel, String> colCategory;

    private ObservableList<GigModel> gigList = FXCollections.observableArrayList();

    // ===== INITIALIZE =====
    @FXML
    public void initialize() {
        configureTable();
        configureDateTimePickers();
        configurePriceField();
        configureCategoryComboBox();
        loadCategories();
        loadGigs();

        statusComboBox.setItems(FXCollections.observableArrayList("ACTIVE", "INACTIVE"));

        // Click row to fill form
        gigTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selectedGig) -> {
            if (selectedGig != null) {
                fillForm(selectedGig);
            }
        });
    }

    // ===== CONFIGURE DATE TIME PICKERS =====
    private void configureDateTimePickers() {
        // Vérifier que les champs existent
        if (deliveryDatePicker == null) {
            System.err.println("ATTENTION: deliveryDatePicker est null. Le fichier FXML doit être recompilé.");
            return;
        }

        // DatePicker - désactiver les dates passées
        deliveryDatePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now()));
            }
        });

        // Spinner pour les heures (0-23)
        if (deliveryHourSpinner != null) {
            SpinnerValueFactory<Integer> hourValueFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, LocalTime.now().getHour());
            deliveryHourSpinner.setValueFactory(hourValueFactory);
            deliveryHourSpinner.setEditable(true);
        }

        // Spinner pour les minutes (0-59)
        if (deliveryMinuteSpinner != null) {
            SpinnerValueFactory<Integer> minuteValueFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0, 5);
            deliveryMinuteSpinner.setValueFactory(minuteValueFactory);
            deliveryMinuteSpinner.setEditable(true);
        }
    }

    // ===== CONFIGURE PRICE FIELD =====
    private void configurePriceField() {
        // Limiter la saisie aux chiffres et au point décimal
        priceField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*(\\.\\d{0,2})?")) {
                priceField.setText(oldValue);
            }
        });
    }

    // ===== CONFIGURE CATEGORY COMBOBOX =====
    private void configureCategoryComboBox() {
        // Afficher uniquement le nom de la catégorie
        categoryComboBox.setConverter(new StringConverter<CategoryModel>() {
            @Override
            public String toString(CategoryModel category) {
                return category != null ? category.getName() : "";
            }

            @Override
            public CategoryModel fromString(String string) {
                return categoryComboBox.getItems().stream()
                    .filter(cat -> cat.getName().equals(string))
                    .findFirst()
                    .orElse(null);
            }
        });
    }

    // ===== SELECT IMAGE FROM PC =====
    @FXML
    private void selectImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner une image");

        // Filtrer uniquement les images PNG et JPG
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"),
            new FileChooser.ExtensionFilter("PNG", "*.png"),
            new FileChooser.ExtensionFilter("JPG", "*.jpg", "*.jpeg")
        );

        // Ouvrir le dialogue de sélection
        File selectedFile = fileChooser.showOpenDialog(selectImageButton.getScene().getWindow());

        if (selectedFile != null) {
            // Stocker le chemin absolu du fichier
            imageField.setText(selectedFile.getAbsolutePath());
        }
    }

    // ===== CONFIG TABLE =====
    private void configureTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colCategory.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getCategory().getName()
                ));
    }

    // ===== LOAD CATEGORIES =====
    private void loadCategories() {
        try (Connection conn = MyDataBase.getConnection()) {

            String query = "SELECT * FROM category WHERE is_active = true";
            PreparedStatement ps = conn.prepareStatement(query);
            ResultSet rs = ps.executeQuery();

            ObservableList<CategoryModel> categories = FXCollections.observableArrayList();

            while (rs.next()) {
                CategoryModel c = new CategoryModel(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getBoolean("is_active")
                );
                categories.add(c);
            }

            categoryComboBox.setItems(categories);

        } catch (Exception e) {
            showAlert("Error loading categories.");
            e.printStackTrace();
        }
    }

    // ===== LOAD GIGS =====
    private void loadGigs() {
        gigList.clear();

        try (Connection conn = MyDataBase.getConnection()) {

            String query = """
                    SELECT g.*, c.id as cid, c.name as cname, c.description as cdesc, c.is_active as cactive
                    FROM gig g
                    JOIN category c ON g.category_id = c.id
                    """;

            PreparedStatement ps = conn.prepareStatement(query);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {

                CategoryModel category = new CategoryModel(
                        rs.getInt("cid"),
                        rs.getString("cname"),
                        rs.getString("cdesc"),
                        rs.getBoolean("cactive")
                );

                // Convertir le Timestamp SQL en LocalDateTime
                Timestamp timestamp = rs.getTimestamp("delivery_time");
                LocalDateTime deliveryTime = timestamp != null ? timestamp.toLocalDateTime() : null;

                GigModel gig = new GigModel(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getDouble("price"),
                        deliveryTime,
                        rs.getString("image"),
                        rs.getString("status")
                );

                // Définir la catégorie après la construction
                gig.setCategory(category);

                gigList.add(gig);
            }

            gigTable.setItems(gigList);

        } catch (Exception e) {
            showAlert("Erreur lors du chargement des gigs:\n\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    // ===== ADD GIG =====
    @FXML
    private void addGig() {

        if (!validateInputs()) return;

        try (Connection conn = MyDataBase.getConnection()) {

            String query = "INSERT INTO gig (title, description, price, delivery_time, image, status, category_id) VALUES (?, ?, ?, ?, ?, ?, ?)";

            PreparedStatement ps = conn.prepareStatement(query);

            ps.setString(1, titleField.getText().trim());
            ps.setString(2, descriptionArea.getText().trim());
            ps.setDouble(3, Double.parseDouble(priceField.getText()));

            // Convertir la date et l'heure en LocalDateTime
            LocalDate date = deliveryDatePicker.getValue();
            int hour = deliveryHourSpinner != null ? deliveryHourSpinner.getValue() : 0;
            int minute = deliveryMinuteSpinner != null ? deliveryMinuteSpinner.getValue() : 0;
            LocalDateTime deliveryTime = LocalDateTime.of(date, LocalTime.of(hour, minute));
            ps.setTimestamp(4, Timestamp.valueOf(deliveryTime));

            ps.setString(5, imageField.getText());
            ps.setString(6, statusComboBox.getValue());
            ps.setInt(7, categoryComboBox.getValue().getId());

            ps.executeUpdate();

            showAlert("Gig ajouté avec succès !");
            clearForm();
            loadGigs();

        } catch (Exception e) {
            showAlert("Erreur lors de l'ajout du gig.");
            e.printStackTrace();
        }
    }

    // ===== UPDATE GIG =====
    @FXML
    private void updateGig() {

        GigModel selectedGig = gigTable.getSelectionModel().getSelectedItem();
        if (selectedGig == null) {
            showAlert("Select a gig to update.");
            return;
        }

        if (!validateInputs()) return;

        try (Connection conn = MyDataBase.getConnection()) {

            String query = "UPDATE gig SET title=?, description=?, price=?, delivery_time=?, image=?, status=?, category_id=? WHERE id=?";

            PreparedStatement ps = conn.prepareStatement(query);

            ps.setString(1, titleField.getText().trim());
            ps.setString(2, descriptionArea.getText().trim());
            ps.setDouble(3, Double.parseDouble(priceField.getText()));

            // Convertir la date et l'heure en LocalDateTime
            LocalDate date = deliveryDatePicker.getValue();
            int hour = deliveryHourSpinner != null ? deliveryHourSpinner.getValue() : 0;
            int minute = deliveryMinuteSpinner != null ? deliveryMinuteSpinner.getValue() : 0;
            LocalDateTime deliveryTime = LocalDateTime.of(date, LocalTime.of(hour, minute));
            ps.setTimestamp(4, Timestamp.valueOf(deliveryTime));

            ps.setString(5, imageField.getText());
            ps.setString(6, statusComboBox.getValue());
            ps.setInt(7, categoryComboBox.getValue().getId());
            ps.setInt(8, selectedGig.getId());

            ps.executeUpdate();

            showAlert("Gig modifié avec succès !");
            clearForm();
            loadGigs();

        } catch (Exception e) {
            showAlert("Error updating gig.");
            e.printStackTrace();
        }
    }

    // ===== DELETE GIG =====
    @FXML
    private void deleteGig() {

        GigModel selectedGig = gigTable.getSelectionModel().getSelectedItem();
        if (selectedGig == null) {
            showAlert("Select a gig to delete.");
            return;
        }

        try (Connection conn = MyDataBase.getConnection()) {

            String query = "DELETE FROM gig WHERE id=?";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, selectedGig.getId());
            ps.executeUpdate();

            showAlert("Gig deleted successfully!");
            clearForm();
            loadGigs();

        } catch (Exception e) {
            showAlert("Error deleting gig.");
            e.printStackTrace();
        }
    }

    // ===== VALIDATION =====
    private boolean validateInputs() {
        StringBuilder errors = new StringBuilder();

        // 1. Validation du Title
        String title = titleField.getText().trim();
        if (title.isEmpty()) {
            errors.append("• Le titre est obligatoire.\n");
        } else if (title.length() < 5) {
            errors.append("• Le titre doit contenir au moins 5 caractères.\n");
        } else if (title.length() > 100) {
            errors.append("• Le titre ne doit pas dépasser 100 caractères.\n");
        }

        // 2. Validation de la Description
        String description = descriptionArea.getText().trim();
        if (description.isEmpty()) {
            errors.append("• La description est obligatoire.\n");
        } else if (description.length() < 20) {
            errors.append("• La description doit contenir au moins 20 caractères.\n");
        } else if (description.length() > 1000) {
            errors.append("• La description ne doit pas dépasser 1000 caractères.\n");
        }

        // 3. Validation du Prix (Currency)
        String priceText = priceField.getText().trim();
        if (priceText.isEmpty()) {
            errors.append("• Le prix est obligatoire.\n");
        } else {
            try {
                double price = Double.parseDouble(priceText);
                if (price <= 0) {
                    errors.append("• Le prix doit être supérieur à 0.\n");
                } else if (price > 1000000) {
                    errors.append("• Le prix ne doit pas dépasser 1,000,000.\n");
                }
            } catch (NumberFormatException e) {
                errors.append("• Le prix doit être un nombre valide.\n");
            }
        }

        // 4. Validation de la Date et Heure de livraison
        LocalDate deliveryDate = deliveryDatePicker.getValue();
        if (deliveryDate == null) {
            errors.append("• La date de livraison est obligatoire.\n");
        } else {
            int hour = deliveryHourSpinner != null ? deliveryHourSpinner.getValue() : 0;
            int minute = deliveryMinuteSpinner != null ? deliveryMinuteSpinner.getValue() : 0;
            LocalDateTime deliveryDateTime = LocalDateTime.of(deliveryDate, LocalTime.of(hour, minute));

            // Vérifier que la date/heure est supérieure à la date/heure actuelle
            if (deliveryDateTime.isBefore(LocalDateTime.now())) {
                errors.append("• La date et l'heure de livraison doivent être supérieures à la date actuelle.\n");
            }
        }

        // 5. Validation de l'URL/Chemin de l'image
        String imagePath = imageField.getText().trim();
        if (imagePath.isEmpty()) {
            errors.append("• L'image est obligatoire.\n");
        } else {
            String lowerPath = imagePath.toLowerCase();
            if (!lowerPath.endsWith(".png") && !lowerPath.endsWith(".jpg") && !lowerPath.endsWith(".jpeg")) {
                errors.append("• L'image doit être au format PNG ou JPG.\n");
            }
            // Vérifier que le fichier existe
            File imageFile = new File(imagePath);
            if (!imageFile.exists()) {
                errors.append("• Le fichier image n'existe pas.\n");
            }
        }

        // 6. Validation de la Catégorie
        if (categoryComboBox.getValue() == null) {
            errors.append("• La catégorie est obligatoire.\n");
        }

        // 7. Validation du Statut
        if (statusComboBox.getValue() == null) {
            errors.append("• Le statut est obligatoire.\n");
        }

        // Afficher les erreurs s'il y en a
        if (errors.length() > 0) {
            showAlert("Erreurs de validation :\n\n" + errors.toString());
            return false;
        }

        return true;
    }

    // ===== FILL FORM =====
    private void fillForm(GigModel gig) {
        titleField.setText(gig.getTitle());
        descriptionArea.setText(gig.getDescription());
        priceField.setText(String.valueOf(gig.getPrice()));

        // Remplir la date et l'heure
        LocalDateTime deliveryTime = gig.getDeliveryTime();
        if (deliveryTime != null) {
            deliveryDatePicker.setValue(deliveryTime.toLocalDate());
            if (deliveryHourSpinner != null) {
                deliveryHourSpinner.getValueFactory().setValue(deliveryTime.getHour());
            }
            if (deliveryMinuteSpinner != null) {
                deliveryMinuteSpinner.getValueFactory().setValue(deliveryTime.getMinute());
            }
        }

        imageField.setText(gig.getImage());
        statusComboBox.setValue(gig.getStatus());
        categoryComboBox.setValue(gig.getCategory());
    }

    // ===== CLEAR FORM =====
    private void clearForm() {
        titleField.clear();
        descriptionArea.clear();
        priceField.clear();
        deliveryDatePicker.setValue(null);
        if (deliveryHourSpinner != null) {
            deliveryHourSpinner.getValueFactory().setValue(LocalTime.now().getHour());
        }
        if (deliveryMinuteSpinner != null) {
            deliveryMinuteSpinner.getValueFactory().setValue(0);
        }
        imageField.clear();
        categoryComboBox.setValue(null);
        statusComboBox.setValue(null);
    }

    // ===== ALERT =====
    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ===== NAVIGATION METHODS =====
    
    /**
     * Refresh the gig table by reloading data from the database
     */
    @FXML
    private void onRefresh() {
        loadGigs();
        loadCategories();
        clearForm();
        showAlert("Données rafraîchies avec succès !");
    }

    /**
     * Navigate back to the profile page
     */
    @FXML
    private void onGoBack() {
        try {
            App.setRoot("profile");
        } catch (IOException e) {
            showAlert("Erreur lors de la navigation vers le profil.");
            e.printStackTrace();
        }
    }

    /**
     * Exit the application
     */
    @FXML
    private void onExit() {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirmation");
        confirmAlert.setHeaderText("Quitter l'application");
        confirmAlert.setContentText("Êtes-vous sûr de vouloir quitter l'application ?");

        if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            System.exit(0);
        }
    }
}
