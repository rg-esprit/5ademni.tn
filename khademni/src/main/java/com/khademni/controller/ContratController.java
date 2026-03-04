package com.khademni.controller;

import com.khademni.App;
import com.khademni.model.ContratModel;
import com.khademni.model.UserModel;
import com.khademni.utils.MyDataBase;
import com.khademni.service.ExchangeRateService;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.util.Duration;

import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;

public class ContratController {

    @FXML
    private FlowPane bentoGrid;
    @FXML
    private HBox floatingActionBar;
    @FXML
    private Button fabEditBtn;
    @FXML
    private Button fabDeleteBtn;
    @FXML
    private Button fabPdfBtn;
    @FXML
    private TextField searchField;
    @FXML
    private TabPane mainTabPane;

    @FXML
    private TextField freelancerSearchField;
    @FXML
    private VBox freelancerSearchResults;
    @FXML
    private HBox selectedFreelancerBox;
    @FXML
    private Label selectedFreelancerInitials;
    @FXML
    private Label selectedFreelancerName;
    @FXML
    private Label selectedFreelancerEmail;
    @FXML
    private TextField titleField;
    @FXML
    private TextField priceField;
    @FXML
    private TextArea descriptionField;
    @FXML
    private DatePicker dateContratPicker;

    @FXML
    private Label errorLabel;
    @FXML
    private TextField numTelephoneField;
    @FXML
    private Label statTotal;
    @FXML
    private Label statPaye;
    @FXML
    private Label statEnAttente;

    private ContratModel selectedContrat;
    private ContratModel contratToEdit;
    private int selectedFreelancerId = -1;
    private ObservableList<ContratModel> contratList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        ExchangeRateService.getInstance();
        setupSearch();
        setupFreelancerSearch();
        loadContrats();
    }

    private void renderBentoGrid(List<ContratModel> contrats) {
        bentoGrid.getChildren().clear();
        int delay = 0;
        for (ContratModel item : contrats) {
            VBox card = createBentoCard(item);
            card.setOpacity(0);
            FadeTransition ft = new FadeTransition(Duration.millis(500), card);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.setDelay(Duration.millis(delay));
            ft.play();
            bentoGrid.getChildren().add(card);
            delay += 50;
        }
    }

    private VBox createBentoCard(ContratModel item) {
        VBox card = new VBox(20);
        card.getStyleClass().add("glass-card");
        card.setPrefWidth(384);
        card.setMinWidth(384);

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Label badge = new Label(item.getStatut());
        badge.getStyleClass().add("squishy-badge");
        if ("PAYE".equalsIgnoreCase(item.getStatut()))
            badge.setStyle("-fx-background-color: #27ae60;");

        Region s = new Region();
        HBox.setHgrow(s, Priority.ALWAYS);
        Label price = new Label(String.format("%.0f DT", item.getPrix()));
        price.getStyleClass().add("cyber-price");
        header.getChildren().addAll(badge, s, price);

        Label title = new Label(item.getTitre());
        title.getStyleClass().add("bento-card-title");
        title.setWrapText(true);

        VBox meta = new VBox(8);
        Label client = new Label("Client: " + item.getClientName());
        Label freelancer = new Label("Freelancer: " + item.getFreelancerName());
        client.setStyle(
                "-fx-text-fill: #6b7280; -fx-font-size: 12; -fx-font-weight: 600;");
        freelancer.setStyle(
                "-fx-text-fill: #6b7280; -fx-font-size: 12; -fx-font-weight: 600;");
        meta.getChildren().addAll(client, freelancer);

        // Show phone number if available
        if (item.getNumTelephone() != null && !item.getNumTelephone().isBlank()) {
            Label phoneLabel = new Label("Tel: " + item.getNumTelephone());
            phoneLabel.setStyle("-fx-text-fill: #6c5ce7; -fx-font-size: 12; -fx-font-weight: 700;");
            meta.getChildren().add(phoneLabel);
        }

        HBox footer = new HBox(15);
        footer.setAlignment(Pos.CENTER_LEFT);
        Label dateLabel = new Label(
                "Date: " + item.getDateContrat().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        dateLabel.setStyle(
                "-fx-text-fill: #9ca3af; -fx-font-size: 11;");
        footer.getChildren().add(dateLabel);

        card.getChildren().addAll(header, title, meta, footer);

        card.setOnMouseClicked(e -> {
            bentoGrid.getChildren().forEach(n -> n.getStyleClass().remove("card-selected"));
            card.getStyleClass().add("card-selected");
            selectedContrat = item;
            floatingActionBar.setVisible(true);
            floatingActionBar.setManaged(true);
        });

        card.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), card);
            st.setToX(1.02);
            st.setToY(1.02);
            st.play();
        });
        card.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), card);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });

        return card;
    }

    private void setupSearch() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            String filter = newValue == null ? "" : newValue.toLowerCase().trim();
            List<ContratModel> filtered = contratList.stream()
                    .filter(c -> c.getTitre().toLowerCase().contains(filter)
                            || c.getDescription().toLowerCase().contains(filter))
                    .collect(Collectors.toList());
            renderBentoGrid(filtered);
        });
    }

    private void setupFreelancerSearch() {
        freelancerSearchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.trim().length() < 2) {
                freelancerSearchResults.setVisible(false);
                freelancerSearchResults.setManaged(false);
                return;
            }
            searchFreelancers(newVal.trim());
        });
    }

    private void searchFreelancers(String query) {
        UserModel currentUser = App.getCurrentUser();
        if (currentUser == null) return;

        Thread searchThread = new Thread(() -> {
            try {
                String sql = "SELECT id, first_name, last_name, email FROM users " +
                        "WHERE id != ? AND (LOWER(first_name) LIKE ? OR LOWER(last_name) LIKE ? " +
                        "OR LOWER(CONCAT(first_name, ' ', last_name)) LIKE ?) LIMIT 8";
                try (Connection conn = MyDataBase.getConnection();
                        PreparedStatement stmt = conn.prepareStatement(sql)) {
                    String pattern = "%" + query.toLowerCase() + "%";
                    stmt.setInt(1, currentUser.getId());
                    stmt.setString(2, pattern);
                    stmt.setString(3, pattern);
                    stmt.setString(4, pattern);
                    ResultSet rs = stmt.executeQuery();

                    List<Object[]> results = new ArrayList<>();
                    while (rs.next()) {
                        results.add(new Object[] {
                                rs.getInt("id"),
                                rs.getString("first_name"),
                                rs.getString("last_name"),
                                rs.getString("email")
                        });
                    }

                    Platform.runLater(() -> {
                        freelancerSearchResults.getChildren().clear();
                        if (results.isEmpty()) {
                            Label noResult = new Label("Aucun utilisateur trouve");
                            noResult.setStyle("-fx-text-fill: #9ca3af; -fx-padding: 12 16; -fx-font-size: 13;");
                            freelancerSearchResults.getChildren().add(noResult);
                        } else {
                            for (Object[] row : results) {
                                int userId = (int) row[0];
                                String firstName = (String) row[1];
                                String lastName = (String) row[2];
                                String email = (String) row[3];

                                HBox item = new HBox(12);
                                item.setAlignment(Pos.CENTER_LEFT);
                                item.setStyle("-fx-padding: 10 16; -fx-cursor: hand;");

                                String initials = ("" + firstName.charAt(0) + lastName.charAt(0)).toUpperCase();
                                Label avatar = new Label(initials);
                                avatar.setStyle("-fx-background-color: #6c5ce7; -fx-text-fill: white; " +
                                        "-fx-font-weight: bold; -fx-padding: 6 10; -fx-background-radius: 50; -fx-font-size: 11;");

                                VBox info = new VBox(2);
                                Label nameLabel = new Label(firstName + " " + lastName);
                                nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #1a1a1a; -fx-font-size: 13;");
                                Label emailLabel = new Label(email);
                                emailLabel.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 11;");
                                info.getChildren().addAll(nameLabel, emailLabel);
                                HBox.setHgrow(info, Priority.ALWAYS);

                                item.getChildren().addAll(avatar, info);

                                item.setOnMouseEntered(e -> item.setStyle(
                                        "-fx-padding: 10 16; -fx-cursor: hand; -fx-background-color: #f3f0ff;"));
                                item.setOnMouseExited(e -> item.setStyle(
                                        "-fx-padding: 10 16; -fx-cursor: hand; -fx-background-color: transparent;"));

                                item.setOnMouseClicked(e -> selectFreelancer(userId, firstName, lastName, email));
                                freelancerSearchResults.getChildren().add(item);
                            }
                        }
                        freelancerSearchResults.setVisible(true);
                        freelancerSearchResults.setManaged(true);
                    });
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, "freelancer-search");
        searchThread.setDaemon(true);
        searchThread.start();
    }

    private void selectFreelancer(int userId, String firstName, String lastName, String email) {
        selectedFreelancerId = userId;
        String initials = ("" + firstName.charAt(0) + lastName.charAt(0)).toUpperCase();

        selectedFreelancerInitials.setText(initials);
        selectedFreelancerName.setText(firstName + " " + lastName);
        selectedFreelancerEmail.setText(email);

        selectedFreelancerBox.setVisible(true);
        selectedFreelancerBox.setManaged(true);

        freelancerSearchField.setVisible(false);
        freelancerSearchField.setManaged(false);
        freelancerSearchResults.setVisible(false);
        freelancerSearchResults.setManaged(false);
    }

    @FXML
    private void clearFreelancerSelection() {
        selectedFreelancerId = -1;
        selectedFreelancerBox.setVisible(false);
        selectedFreelancerBox.setManaged(false);
        freelancerSearchField.setVisible(true);
        freelancerSearchField.setManaged(true);
        freelancerSearchField.clear();
    }

    private void loadContrats() {
        contratList.clear();
        UserModel currentUser = App.getCurrentUser();
        if (currentUser == null)
            return;

        String query = "SELECT c.*, u1.first_name as cfname, u1.last_name as clname, " +
                "u2.first_name as ffname, u2.last_name as flname " +
                "FROM contrats c " +
                "LEFT JOIN users u1 ON c.client_id = u1.id " +
                "LEFT JOIN users u2 ON c.freelancer_id = u2.id " +
                "WHERE c.client_id = ? OR c.freelancer_id = ?";

        try (Connection conn = MyDataBase.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, currentUser.getId());
            pstmt.setInt(2, currentUser.getId());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                contratList.add(new ContratModel(
                        rs.getInt("id"), rs.getInt("client_id"), rs.getInt("freelancer_id"),
                        rs.getString("cfname") + " " + rs.getString("clname"),
                        rs.getString("ffname") + " " + rs.getString("flname"),
                        rs.getString("titre"), rs.getString("description"),
                        rs.getDouble("prix"), rs.getDate("date_contrat").toLocalDate(),
                        rs.getString("statut"), rs.getString("num_telephone")));
            }
            renderBentoGrid(contratList);
            updateStats();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    private void updateStats() {
        statTotal.setText(String.valueOf(contratList.size()));
        statPaye.setText(
                String.valueOf(contratList.stream().filter(c -> "PAYE".equalsIgnoreCase(c.getStatut())).count()));
        statEnAttente.setText(
                String.valueOf(contratList.stream().filter(c -> "EN_ATTENTE".equalsIgnoreCase(c.getStatut())).count()));
    }

    @FXML
    private void onFabEdit() {
        if (selectedContrat != null)
            handleEdit(selectedContrat);
    }

    @FXML
    private void onFabDelete() {
        if (selectedContrat != null)
            handleDelete(selectedContrat);
    }

    @FXML
    private void onFabPdf() {
        if (selectedContrat != null)
            generatePDF(selectedContrat);
    }

    private void handleEdit(ContratModel contrat) {
        this.contratToEdit = contrat;

        // Show selected freelancer as non-editable chip
        selectedFreelancerId = contrat.getIdFreelancer();
        String fName = contrat.getFreelancerName();
        if (fName != null && fName.contains(" ")) {
            String[] parts = fName.split(" ", 2);
            String initials = ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
            selectedFreelancerInitials.setText(initials);
        } else {
            selectedFreelancerInitials.setText("?");
        }
        selectedFreelancerName.setText(contrat.getFreelancerName());
        selectedFreelancerEmail.setText("ID: " + contrat.getIdFreelancer());
        selectedFreelancerBox.setVisible(true);
        selectedFreelancerBox.setManaged(true);
        freelancerSearchField.setVisible(false);
        freelancerSearchField.setManaged(false);

        if (titleField != null) {
            titleField.setText(contrat.getTitre());
            titleField.setDisable(true);
        }
        if (priceField != null) {
            priceField.setText(String.valueOf(contrat.getPrix()));
            priceField.setDisable(true);
        }

        descriptionField.setText(contrat.getDescription());
        dateContratPicker.setValue(contrat.getDateContrat());
        if (numTelephoneField != null && contrat.getNumTelephone() != null) {
            numTelephoneField.setText(contrat.getNumTelephone());
            numTelephoneField.setEditable(false);
            numTelephoneField.setOpacity(0.6);
            numTelephoneField.setTooltip(
                    new Tooltip("Le numero de telephone ne peut pas etre modifie apres la creation du contrat."));
        }
        mainTabPane.getSelectionModel().select(1);
    }

    @FXML
    private void saveContrat() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        try {
            UserModel currentUser = App.getCurrentUser();
            if (currentUser == null) {
                throw new Exception("Utilisateur non connecte.");
            }

            if (contratToEdit == null) {
                // Creating new contract — search selection + manual fields required
                String titre = titleField.getText().trim();
                String priceStr = priceField.getText().trim();

                if (selectedFreelancerId == -1) {
                    throw new Exception("Veuillez rechercher et selectionner un freelancer.");
                }
                if (titre.isEmpty()) {
                    throw new Exception("Veuillez saisir le titre du contrat.");
                }
                if (priceStr.isEmpty()) {
                    throw new Exception("Veuillez saisir le prix.");
                }

                double prix;
                try {
                    prix = Double.parseDouble(priceStr);
                } catch (NumberFormatException e) {
                    throw new Exception("Le prix doit etre un nombre valide.");
                }

                if (prix <= 0) {
                    throw new Exception("Le prix doit etre superieur a zero.");
                }
            }

            if (dateContratPicker.getValue() == null) {
                throw new Exception("Veuillez remplir la date d'expiration.");
            }

            String numTelephone = numTelephoneField.getText().trim();
            if (numTelephone.isEmpty()) {
                throw new Exception("Le numero de telephone est obligatoire pour le paiement Stripe.");
            }
            String digitsOnly = numTelephone.replaceAll("[^0-9]", "");
            if (digitsOnly.length() < 8) {
                throw new Exception("Le numero de telephone doit contenir au moins 8 chiffres.");
            }

            if (contratToEdit == null) {
                // INSERT new contract
                String titre = titleField.getText().trim();
                double prix = Double.parseDouble(priceField.getText().trim());

                String insertSQL = "INSERT INTO contrats (client_id, freelancer_id, titre, description, prix, date_contrat, statut, num_telephone) "
                        + "VALUES (?, ?, ?, ?, ?, ?, 'EN_ATTENTE', ?)";
                try (Connection conn = MyDataBase.getConnection();
                        PreparedStatement stmt = conn.prepareStatement(insertSQL)) {
                    stmt.setInt(1, currentUser.getId());
                    stmt.setInt(2, selectedFreelancerId);
                    stmt.setString(3, titre);
                    stmt.setString(4, descriptionField.getText());
                    stmt.setDouble(5, prix);
                    stmt.setDate(6, java.sql.Date.valueOf(dateContratPicker.getValue()));
                    stmt.setString(7, numTelephone);
                    stmt.executeUpdate();
                }
            } else {
                // UPDATE existing contract (only description, date)
                String updateSQL = "UPDATE contrats SET description = ?, date_contrat = ? WHERE id = ?";
                try (Connection conn = MyDataBase.getConnection();
                        PreparedStatement stmt = conn.prepareStatement(updateSQL)) {
                    stmt.setString(1, descriptionField.getText());
                    stmt.setDate(2, java.sql.Date.valueOf(dateContratPicker.getValue()));
                    stmt.setInt(3, contratToEdit.getIdContrat());
                    stmt.executeUpdate();
                }
            }

            showAlert(Alert.AlertType.INFORMATION, "Succes", "Protocole initialise avec succes.");
            resetForm();
            loadContrats();
            mainTabPane.getSelectionModel().select(0);
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void resetForm() {
        contratToEdit = null;
        selectedFreelancerId = -1;

        // Reset freelancer search
        freelancerSearchField.clear();
        freelancerSearchField.setVisible(true);
        freelancerSearchField.setManaged(true);
        freelancerSearchResults.setVisible(false);
        freelancerSearchResults.setManaged(false);
        selectedFreelancerBox.setVisible(false);
        selectedFreelancerBox.setManaged(false);

        if (titleField != null) {
            titleField.clear();
            titleField.setDisable(false);
        }
        if (priceField != null) {
            priceField.clear();
            priceField.setDisable(false);
        }
        descriptionField.clear();
        numTelephoneField.clear();
        numTelephoneField.setEditable(true);
        numTelephoneField.setOpacity(1.0);
        numTelephoneField.setTooltip(null);
        dateContratPicker.setValue(null);
    }

    private void handleDelete(ContratModel contrat) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Terminer ce protocole ?", ButtonType.YES, ButtonType.NO);
        if (alert.showAndWait().get() == ButtonType.YES) {
            try (Connection conn = MyDataBase.getConnection();
                    PreparedStatement stmt = conn.prepareStatement("DELETE FROM contrats WHERE id = ?")) {
                stmt.setInt(1, contrat.getIdContrat());
                stmt.executeUpdate();
                loadContrats();
                floatingActionBar.setVisible(false);
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
            }
        }
    }

    private void generatePDF(ContratModel contrat) {
        String path = System.getProperty("user.home") + "/Downloads/Contract_" + contrat.getIdContrat() + ".pdf";
        try (FileOutputStream fos = new FileOutputStream(path)) {
            Document document = new Document();
            PdfWriter.getInstance(document, fos);
            document.open();
            document.add(new Paragraph("LEGAL PROTOCOL - 5ADEMNI.TN"));
            document.add(new Paragraph("--------------------------------------------------"));
            document.add(new Paragraph("Title: " + contrat.getTitre()));
            document.add(new Paragraph("Client: " + contrat.getClientName()));
            document.add(new Paragraph("Freelancer: " + contrat.getFreelancerName()));
            document.add(new Paragraph("Amount: " + contrat.getPrix() + " DT"));
            document.add(new Paragraph("Status: " + contrat.getStatut()));
            document.close();
            showAlert(Alert.AlertType.INFORMATION, "Succes", "PDF exporte.");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    @FXML
    private void goToPaiement() throws IOException {
        App.setRoot("paiement");
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.show();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
}
