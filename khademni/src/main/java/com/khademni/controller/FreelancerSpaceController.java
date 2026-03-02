package com.khademni.controller;

import com.khademni.App;
import com.khademni.model.OffreModel;
import com.khademni.model.UserModel;
import com.khademni.service.OffreService;
import com.khademni.service.ServiceFactory;
import com.khademni.exception.BusinessException;
import com.khademni.utils.PasswordUtil;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import com.khademni.service.ZenQuoteService;

import java.io.FileOutputStream;
import java.io.IOException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FreelancerSpaceController {

    @FXML
    private TableView<OffreModel> offreTable;
    @FXML
    private TableColumn<OffreModel, Integer> colId;
    @FXML
    private TableColumn<OffreModel, Integer> colUserId;
    @FXML
    private TableColumn<OffreModel, String> colTitre;
    @FXML
    private TableColumn<OffreModel, Double> colPrix;
    @FXML
    private TableColumn<OffreModel, String> colStatut;
    @FXML
    private TableColumn<OffreModel, LocalDateTime> colDate;
    @FXML
    private TableColumn<OffreModel, LocalDateTime> colDateLimite;
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
    private Label quoteLabel;
    @FXML
    private Label quoteAuthor;
    @FXML
    private VBox quoteContainer;
    @FXML
    private Button refreshQuoteBtn;

    private ObservableList<OffreModel> offreList = FXCollections.observableArrayList();
    private final ZenQuoteService zenQuoteService = new ZenQuoteService();
    private final OffreService offreService = ServiceFactory.getOffreService();

    @FXML
    public void initialize() {
        // Show sequential row number based on original list position (stable after
        // filtering)
        colId.setCellFactory(col -> new TableCell<OffreModel, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText("");
                } else {
                    OffreModel offre = getTableView().getItems().get(getIndex());
                    int originalIndex = offreList.indexOf(offre) + 1;
                    setText(String.valueOf(originalIndex));
                }
            }
        });
        colUserId.setCellValueFactory(new PropertyValueFactory<>("userUniqueId"));
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colPrix.setCellValueFactory(new PropertyValueFactory<>("prix"));
        colPrix.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null)
                    setText("");
                else
                    setText(item == 0 ? "----" : String.valueOf(item));
            }
        });
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateCreation"));
        colDateLimite.setCellValueFactory(new PropertyValueFactory<>("dateLimite"));
        colDateLimite.setCellFactory(tc -> new TableCell<>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.toLocalDate().format(formatter));
            }
        });

        setupFloatingActionBar();
        setupSearch();
        loadOffres();
        loadMotivationalQuote();
    }

    private void loadMotivationalQuote() {
        updateQuoteUI(zenQuoteService.getCurrentQuote(), zenQuoteService.getCurrentAuthor());
        zenQuoteService.fetchNewQuote((quote, author) -> updateQuoteUI(quote, author));
    }

    private void updateQuoteUI(String quote, String author) {
        if (quoteLabel != null && quote != null) {
            quoteLabel.setText("\"" + quote + "\"");
        }
        if (quoteAuthor != null && author != null) {
            quoteAuthor.setText("— " + author);
        }
        if (refreshQuoteBtn != null) {
            refreshQuoteBtn.setDisable(false);
        }
    }

    @FXML
    private void refreshQuote() {
        if (refreshQuoteBtn != null)
            refreshQuoteBtn.setDisable(true);
        if (quoteLabel != null)
            quoteLabel.setText("Chargement d'une nouvelle inspiration...");
        if (quoteAuthor != null)
            quoteAuthor.setText("");
        zenQuoteService.fetchNewQuote((quote, author) -> updateQuoteUI(quote, author));
    }

    private void setupSearch() {
        FilteredList<OffreModel> filteredData = new FilteredList<>(offreList, p -> true);
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(offre -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase();
                // Match by sequential row number (displayed ID) or titre
                int rowNumber = offreList.indexOf(offre) + 1;
                return String.valueOf(rowNumber).contains(lowerCaseFilter) ||
                        offre.getTitre().toLowerCase().contains(lowerCaseFilter);
            });
        });
        offreTable.setItems(filteredData);
    }

    private void loadOffres() {
        offreList.clear();
        UserModel currentUser = App.getCurrentUser();
        if (currentUser == null)
            return;

        try {
            offreList.addAll(offreService.getUserOffres(currentUser.getId()));
        } catch (BusinessException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    private void setupFloatingActionBar() {
        offreTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                floatingActionBar.setVisible(true);
                floatingActionBar.setManaged(true);
            } else {
                floatingActionBar.setVisible(false);
                floatingActionBar.setManaged(false);
            }
        });
    }

    @FXML
    private void onFabEdit() {
        OffreModel selected = offreTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            handleEdit(selected);
        }
    }

    @FXML
    private void onFabDelete() {
        OffreModel selected = offreTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            handleDelete(selected);
        }
    }

    @FXML
    private void onFabPdf() {
        OffreModel selected = offreTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            generatePDF(selected);
        }
    }

    private void handleEdit(OffreModel offre) {
        if (!confirmPassword())
            return;
        OffreFormController.editMode = true;
        OffreFormController.selectedOffre = offre;
        OffreFormController.creationType = "OFFRE";
        try {
            App.setRoot("offre_form");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleDelete(OffreModel offre) {
        if (!confirmPassword())
            return;
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer cet offre ?", ButtonType.YES, ButtonType.NO);
        if (alert.showAndWait().get() == ButtonType.YES) {
            try {
                offreService.deleteOffre(offre.getId(), "OFFRE");
                loadOffres();
            } catch (BusinessException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
            }
        }
    }

    private void generatePDF(OffreModel offre) {
        String dest = System.getProperty("user.home") + "/Downloads/Offre_" + offre.getId() + ".pdf";
        try (FileOutputStream fos = new FileOutputStream(dest)) {
            Document document = new Document();
            PdfWriter.getInstance(document, fos);
            document.open();
            document.add(new Paragraph("5ademni.tn - Offre de Service"));
            document.add(new Paragraph("--------------------------------------------------"));
            document.add(new Paragraph("ID Offre: " + offre.getId()));
            document.add(new Paragraph("Freelancer ID: " + offre.getUserUniqueId()));
            document.add(new Paragraph("Titre: " + offre.getTitre()));
            document.add(new Paragraph("Prix: " + offre.getPrix() + " DT"));
            document.add(new Paragraph("Date: " + offre.getDateCreation()));
            document.add(new Paragraph("Statut: " + offre.getStatut()));
            document.add(new Paragraph("--------------------------------------------------"));
            document.add(new Paragraph("Description:"));
            document.add(new Paragraph(offre.getDescription()));
            document.close();
            showAlert(Alert.AlertType.INFORMATION, "Succès", "PDF généré dans Downloads");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur PDF: " + e.getMessage());
        }
    }

    @FXML
    private void goToAddOffer() throws IOException {
        OffreFormController.editMode = false;
        OffreFormController.creationType = "OFFRE";
        App.setRoot("offre_form");
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private boolean confirmPassword() {
        if (App.getCurrentUser() == null) {
            new Alert(Alert.AlertType.ERROR, "Aucun utilisateur connecté.").showAndWait();
            return false;
        }

        // Custom dark-themed dialog
        Stage dialogStage = new Stage();
        dialogStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialogStage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
        dialogStage.setTitle("Confirmation");

        // Root container
        javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(18);
        root.setAlignment(javafx.geometry.Pos.CENTER);
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #1e1e2f, #2a2a40); "
                + "-fx-background-radius: 20; -fx-padding: 32; -fx-border-radius: 20; "
                + "-fx-border-color: rgba(255,255,255,0.08); -fx-border-width: 1; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 30, 0, 0, 8);");
        root.setPrefWidth(380);

        // Lock icon
        Label lockIcon = new Label("\uD83D\uDD12");
        lockIcon.setStyle("-fx-font-size: 36px;");

        // Title
        Label title = new Label("Confirmation");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: white;");

        // Subtitle
        Label subtitle = new Label("Entrez votre mot de passe pour continuer");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #a0a0b8;");

        // Password field container
        javafx.scene.layout.HBox pwContainer = new javafx.scene.layout.HBox(8);
        pwContainer.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        pwContainer.setStyle(
                "-fx-background-color: rgba(255,255,255,0.07); -fx-background-radius: 10; -fx-padding: 4 12 4 12;");

        PasswordField pwField = new PasswordField();
        pwField.setPromptText("Mot de passe");
        pwField.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-prompt-text-fill: #6b6b80; "
                + "-fx-font-size: 14px; -fx-pref-height: 38;");
        javafx.scene.layout.HBox.setHgrow(pwField, javafx.scene.layout.Priority.ALWAYS);

        TextField pwVisible = new TextField();
        pwVisible.setPromptText("Mot de passe");
        pwVisible.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-prompt-text-fill: #6b6b80; "
                + "-fx-font-size: 14px; -fx-pref-height: 38;");
        javafx.scene.layout.HBox.setHgrow(pwVisible, javafx.scene.layout.Priority.ALWAYS);
        pwVisible.setVisible(false);
        pwVisible.setManaged(false);
        pwVisible.textProperty().bindBidirectional(pwField.textProperty());

        Button toggleBtn = new Button("\uD83D\uDC41");
        toggleBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #8b8ba0; -fx-font-size: 16px; -fx-cursor: hand;");
        toggleBtn.setOnAction(e -> {
            boolean showing = pwVisible.isVisible();
            pwVisible.setVisible(!showing);
            pwVisible.setManaged(!showing);
            pwField.setVisible(showing);
            pwField.setManaged(showing);
        });

        pwContainer.getChildren().addAll(pwField, pwVisible, toggleBtn);

        // Buttons
        javafx.scene.layout.HBox btnBox = new javafx.scene.layout.HBox(12);
        btnBox.setAlignment(javafx.geometry.Pos.CENTER);

        Button okBtn = new Button("Confirmer");
        okBtn.setStyle("-fx-background-color: linear-gradient(to right, #6c5ce7, #a855f7); -fx-text-fill: white; "
                + "-fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 10; "
                + "-fx-padding: 10 32; -fx-cursor: hand;");

        Button cancelBtn = new Button("Annuler");
        cancelBtn.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-text-fill: #a0a0b8; "
                + "-fx-font-size: 14px; -fx-background-radius: 10; -fx-padding: 10 32; -fx-cursor: hand;");

        btnBox.getChildren().addAll(okBtn, cancelBtn);

        root.getChildren().addAll(lockIcon, title, subtitle, pwContainer, btnBox);

        Scene scene = new Scene(root);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        dialogStage.setScene(scene);

        // Result holder
        final boolean[] confirmed = { false };

        okBtn.setOnAction(e -> {
            String entered = pwField.getText();
            if (entered == null || entered.isEmpty())
                return;
            String storedHash = App.getCurrentUser().getPassword();
            if (PasswordUtil.checkPassword(entered, storedHash)) {
                confirmed[0] = true;
                dialogStage.close();
            } else {
                subtitle.setText("❌ Mot de passe incorrect !");
                subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #ef4444; -fx-font-weight: bold;");
                pwField.clear();
            }
        });

        cancelBtn.setOnAction(e -> dialogStage.close());

        pwField.setOnAction(e -> okBtn.fire());

        dialogStage.showAndWait();
        return confirmed[0];
    }
}
