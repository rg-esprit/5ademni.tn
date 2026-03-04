package com.khademni.controller;

import com.khademni.App;
import com.khademni.model.ContratModel;
import com.khademni.model.UserModel;
import com.khademni.service.ExchangeRateService;
import com.khademni.service.StripeService;
import com.khademni.utils.MyDataBase;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.util.Duration;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.event.ActionEvent;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

public class PaiementController {

    @FXML
    private FlowPane bentoGrid;
    @FXML
    private HBox floatingActionBar;
    @FXML
    private Button payBtn;
    @FXML
    private ComboBox<String> currencyCombo;
    @FXML
    private Label convertedAmountLabel;

    private ObservableList<ContratModel> contratList = FXCollections.observableArrayList();
    private final StripeService stripeService = new StripeService();
    private final ExchangeRateService exchangeRateService = ExchangeRateService.getInstance();
    private ContratModel selectedContrat;

    @FXML
    public void initialize() {
        setupCurrencyCombo();
        loadContrats();
    }

    private void setupCurrencyCombo() {
        currencyCombo.setItems(FXCollections.observableArrayList("EUR", "USD", "GBP"));
        currencyCombo.setValue("EUR");
        currencyCombo.valueProperty().addListener((obs, oldVal, newVal) -> updateConvertedAmount());
    }

    private void updateConvertedAmount() {
        if (selectedContrat == null || currencyCombo.getValue() == null) {
            convertedAmountLabel.setText("");
            return;
        }
        String currency = currencyCombo.getValue();
        double prixTND = selectedContrat.getPrix();
        double converted = exchangeRateService.convertPrice(prixTND, currency);
        if (converted < 0) {
            convertedAmountLabel.setText("Taux indisponible");
        } else {
            convertedAmountLabel.setText(String.format("%.2f TND = %.2f %s", prixTND, converted, currency));
        }
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
                "-fx-text-fill: #6b7280; -fx-font-size: 13; -fx-font-weight: 600;");
        freelancer.setStyle(
                "-fx-text-fill: #6b7280; -fx-font-size: 13; -fx-font-weight: 600;");
        meta.getChildren().addAll(client, freelancer);

        // Show phone number if available
        if (item.getNumTelephone() != null && !item.getNumTelephone().isBlank()) {
            Label phoneLabel = new Label("Tel: " + item.getNumTelephone());
            phoneLabel.setStyle("-fx-text-fill: #6c5ce7; -fx-font-size: 12; -fx-font-weight: 700;");
            meta.getChildren().add(phoneLabel);
        }

        card.getChildren().addAll(header, title, meta);

        card.setOnMouseClicked(e -> {
            bentoGrid.getChildren().forEach(n -> n.getStyleClass().remove("card-selected"));
            card.getStyleClass().add("card-selected");
            selectedContrat = item;
            floatingActionBar.setVisible(true);
            floatingActionBar.setManaged(true);
            updateConvertedAmount();
            UserModel currentUser = App.getCurrentUser();
            boolean isClient = currentUser != null && currentUser.getId() == item.getIdClient();
            boolean alreadyPaid = "PAYE".equalsIgnoreCase(item.getStatut());
            payBtn.setDisable(!isClient || alreadyPaid);
            payBtn.setText(isClient ? "Payer" : "Consultation uniquement");
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
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    private void updateStatus(ContratModel contrat, String newStatus) {
        Task<Boolean> updateTask = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                String query = "UPDATE contrats SET statut = ? WHERE id = ?";
                try (Connection conn = MyDataBase.getConnection();
                        PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setString(1, newStatus);
                    stmt.setInt(2, contrat.getIdContrat());
                    int updated = stmt.executeUpdate();
                    return updated > 0;
                }
            }
        };

        updateTask.setOnSucceeded(evt -> {
            Boolean success = updateTask.getValue();
            if (success != null && success) {
                showAlert(Alert.AlertType.INFORMATION, "Succes", "Statut mis a jour !");
                loadContrats();
            } else {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Aucun contrat mis a jour.");
            }
        });

        updateTask.setOnFailed(evt -> {
            Throwable ex = updateTask.getException();
            ex.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible de mettre a jour le statut: " + (ex == null ? "" : ex.getMessage()));
        });

        new Thread(updateTask, "update-status-thread").start();
    }

    @FXML
    private void goToContrats() throws IOException {
        App.setRoot("contrat");
    }

    @FXML
    public void handlePay(ActionEvent event) {
        if (selectedContrat == null) {
            try {
                App.setRoot("paiement_form");
            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le formulaire de paiement.");
            }
            return;
        }

        if ("PAYE".equalsIgnoreCase(selectedContrat.getStatut())) {
            showAlert(Alert.AlertType.INFORMATION, "Deja Paye", "Ce contrat est deja marque comme paye.");
            return;
        }

        // Retrieve the expected phone from the contract (for post-payment verification)
        String expectedPhone = getContratPhone(selectedContrat);

        // Go directly to Stripe checkout
        proceedWithStripePayment(expectedPhone);
    }

    /**
     * Retrieves the phone number for this contract from the contrats table.
     */
    private String getContratPhone(ContratModel contrat) {
        String query = "SELECT num_telephone FROM contrats WHERE id = ? AND num_telephone IS NOT NULL AND num_telephone != ''";
        try (Connection conn = MyDataBase.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, contrat.getIdContrat());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("num_telephone");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Shows a failure page in a new window when the phone number doesn't match.
     */
    private void showPaymentFailurePage() {
        Stage failureStage = new Stage();
        failureStage.initModality(Modality.APPLICATION_MODAL);
        failureStage.setTitle("Echec du Paiement");

        VBox root = new VBox(24);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-padding: 60; -fx-background-color: linear-gradient(to bottom, #fef2f2, #ffffff);");

        Label icon = new Label("X");
        icon.setStyle("-fx-font-size: 72; -fx-text-fill: #dc2626; -fx-font-weight: 900;");

        Label title = new Label("Echec du Paiement");
        title.setStyle("-fx-font-size: 28; -fx-font-weight: 900; -fx-text-fill: #dc2626;");

        Label message = new Label(
                "Le numero de telephone saisi ne correspond pas\nau numero enregistre pour ce contrat.");
        message.setStyle("-fx-font-size: 15; -fx-text-fill: #6b7280; -fx-text-alignment: center;");
        message.setWrapText(true);

        VBox infoBox = new VBox(8);
        infoBox.setAlignment(Pos.CENTER);
        infoBox.setStyle("-fx-background-color: #fef2f2; -fx-padding: 20; -fx-background-radius: 12; " +
                "-fx-border-color: #fecaca; -fx-border-radius: 12;");
        infoBox.setMaxWidth(400);
        Label infoTitle = new Label("Pourquoi cette verification ?");
        infoTitle.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #991b1b;");
        Label infoText = new Label(
                "Pour votre securite, le paiement necessite le numero de telephone\nqui a ete associe au contrat lors de sa creation.");
        infoText.setStyle("-fx-font-size: 12; -fx-text-fill: #7f1d1d; -fx-text-alignment: center;");
        infoText.setWrapText(true);
        infoBox.getChildren().addAll(infoTitle, infoText);

        Button closeBtn = new Button("Fermer et reessayer");
        closeBtn.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-background-radius: 50; -fx-padding: 14 40; -fx-font-size: 14; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> failureStage.close());

        root.getChildren().addAll(icon, title, message, infoBox, closeBtn);

        Scene scene = new Scene(root, 550, 500);
        failureStage.setScene(scene);
        failureStage.show();
    }

    /**
     * Shows a success page in a new window after payment + phone verification.
     */
    private void showPaymentSuccessPage() {
        Stage successStage = new Stage();
        successStage.initModality(Modality.APPLICATION_MODAL);
        successStage.setTitle("Paiement avec Succes");

        VBox root = new VBox(24);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-padding: 60; -fx-background-color: linear-gradient(to bottom, #f0fdf4, #ffffff);");

        Label icon = new Label("OK");
        icon.setStyle("-fx-font-size: 48; -fx-text-fill: #16a34a; -fx-font-weight: 900;");

        Label title = new Label("Paiement avec Succes");
        title.setStyle("-fx-font-size: 28; -fx-font-weight: 900; -fx-text-fill: #16a34a;");

        Label message = new Label(
                "Le paiement a ete effectue et verifie avec succes !\nLe numero de telephone correspond au contrat.");
        message.setStyle("-fx-font-size: 15; -fx-text-fill: #6b7280; -fx-text-alignment: center;");
        message.setWrapText(true);

        VBox infoBox = new VBox(8);
        infoBox.setAlignment(Pos.CENTER);
        infoBox.setStyle("-fx-background-color: #f0fdf4; -fx-padding: 20; -fx-background-radius: 12; " +
                "-fx-border-color: #bbf7d0; -fx-border-radius: 12;");
        infoBox.setMaxWidth(400);

        Label contractInfo = new Label("Contrat : " + selectedContrat.getTitre());
        contractInfo.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #166534;");

        Label amountInfo = new Label("Montant : " + String.format("%.2f", selectedContrat.getPrix()) + " DT");
        amountInfo.setStyle("-fx-font-size: 14; -fx-text-fill: #166534;");

        Label securityInfo = new Label("Verification de securite reussie");
        securityInfo.setStyle("-fx-font-size: 12; -fx-text-fill: #15803d;");

        Label statusInfo = new Label("Statut du contrat : PAYE");
        statusInfo.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #166534;");

        infoBox.getChildren().addAll(contractInfo, amountInfo, securityInfo, statusInfo);

        Button closeBtn = new Button("Fermer");
        closeBtn.setStyle("-fx-background-color: #16a34a; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-background-radius: 50; -fx-padding: 14 40; -fx-font-size: 14; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> successStage.close());

        root.getChildren().addAll(icon, title, message, infoBox, closeBtn);

        Scene scn = new Scene(root, 550, 500);
        successStage.setScene(scn);
        successStage.show();
    }

    private void proceedWithStripePayment(String expectedPhone) {
        String description = "Paiement Contrat ID: " + selectedContrat.getIdContrat() + " - "
                + selectedContrat.getTitre();
        double amountTND = selectedContrat.getPrix();
        String currency = currencyCombo.getValue() != null ? currencyCombo.getValue().toLowerCase() : "eur";

        // Always convert from TND to the selected Stripe-supported currency
        double chargeAmount = exchangeRateService.convertPrice(amountTND, currency.toUpperCase());
        if (chargeAmount < 0) {
            showAlert(Alert.AlertType.ERROR, "Erreur de conversion",
                    "Impossible de convertir le montant en " + currency.toUpperCase()
                            + ". Taux de change indisponible.");
            return;
        }

        showAlert(Alert.AlertType.INFORMATION, "Generation du lien",
                "Generation du lien de paiement Stripe pour " + String.format("%.2f", chargeAmount) + " "
                        + currency.toUpperCase()
                        + "...\n\nVeuillez saisir le numero de telephone du contrat dans le champ 'Contact details'.");

        Task<String> paymentTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                return stripeService.createCheckoutSession(chargeAmount, currency, description,
                        selectedContrat.getIdContrat(), expectedPhone);
            }
        };

        paymentTask.setOnSucceeded(evt -> {
            String paymentUrl = paymentTask.getValue();

            try {
                Stage webStage = new Stage();
                webStage.initModality(Modality.APPLICATION_MODAL);
                webStage.setTitle("Paiement Stripe");

                WebView webView = new WebView();
                WebEngine engine = webView.getEngine();
                engine.load(paymentUrl);

                engine.locationProperty().addListener((obs, oldLoc, newLoc) -> {
                    if (newLoc == null)
                        return;
                    String lower = newLoc.toLowerCase();
                    if (lower.contains("/success") && lower.contains("session_id")) {
                        String sessionId = extractQueryParam(newLoc, "session_id");
                        String cIdStr = extractQueryParam(newLoc, "contrat_id");

                        Stage progressStage = new Stage();
                        ProgressIndicator pi = new ProgressIndicator();
                        Scene ps = new Scene(pi, 120, 120);
                        progressStage.initOwner(webStage);
                        progressStage.initModality(Modality.APPLICATION_MODAL);
                        progressStage.setScene(ps);
                        progressStage.setTitle("Verification du paiement...");
                        progressStage.show();

                        // Use verifySessionWithPhone to check both payment AND phone
                        Task<Object[]> verifyTask = new Task<>() {
                            @Override
                            protected Object[] call() throws Exception {
                                if (sessionId == null || sessionId.isBlank()) {
                                    return new Object[] { false, false, "" };
                                }
                                Object[] result = stripeService.verifySessionWithPhone(sessionId);
                                boolean paid = (boolean) result[0];
                                if (paid) {
                                    String insertSQL = "INSERT INTO payments (contrat_id, stripe_session_id, amount, status) VALUES (?, ?, ?, 'PAID')";
                                    try (Connection conn = MyDataBase.getConnection();
                                            PreparedStatement stmt = conn.prepareStatement(insertSQL)) {
                                        stmt.setInt(1, Integer.parseInt(cIdStr != null ? cIdStr : "0"));
                                        stmt.setString(2, sessionId);
                                        stmt.setDouble(3, amountTND);
                                        stmt.executeUpdate();
                                    }
                                }
                                return result;
                            }
                        };

                        verifyTask.setOnSucceeded(v -> {
                            Object[] result = verifyTask.getValue();
                            boolean paid = (boolean) result[0];
                            boolean phoneMatch = (boolean) result[1];
                            progressStage.close();
                            webStage.close();

                            if (paid && phoneMatch) {
                                // SUCCESS: Payment verified AND phone matches
                                if (selectedContrat != null) {
                                    updateStatus(selectedContrat, "PAYE");
                                    creditFreelancerBalance(selectedContrat.getIdFreelancer(),
                                            selectedContrat.getPrix());
                                    floatingActionBar.setVisible(false);

                                    UserModel currentUser = App.getCurrentUser();
                                    if (currentUser != null && currentUser.getEmail() != null
                                            && !currentUser.getEmail().isBlank()) {
                                        Task<Void> emailTask = new Task<>() {
                                            @Override
                                            protected Void call() throws Exception {
                                                String body = String.format(
                                                        "Bonjour %s,\n\nNous confirmons le paiement de %.2f DT pour le contrat : %s.\n\nMerci de votre confiance, l'equipe Khademni.",
                                                        currentUser.getFirstName(), amountTND, selectedContrat.getTitre());
                                                com.khademni.utils.EmailService.sendEmail(currentUser.getEmail(),
                                                        "Confirmation de paiement - Khademni", body);
                                                return null;
                                            }
                                        };
                                        new Thread(emailTask, "email-thread").start();
                                    }
                                }
                                showPaymentSuccessPage();
                            } else if (paid && !phoneMatch) {
                                // PAYMENT OK but PHONE MISMATCH
                                showPaymentFailurePage();
                            } else {
                                showAlert(Alert.AlertType.ERROR, "Verification echouee",
                                        "Le paiement n'a pas pu etre verifie.");
                            }
                        });

                        verifyTask.setOnFailed(v -> {
                            progressStage.close();
                            Throwable ex = verifyTask.getException();
                            if (ex != null)
                                ex.printStackTrace();
                            showAlert(Alert.AlertType.ERROR, "Verification erreur",
                                    "Erreur lors de la verification du paiement: "
                                            + (ex == null ? "" : ex.getMessage()));
                            webStage.close();
                        });

                        new Thread(verifyTask, "stripe-verify-thread").start();

                    } else if (lower.contains("/cancel")) {
                        showAlert(Alert.AlertType.ERROR, "Paiement Annule", "Vous avez annule le paiement.");
                        webStage.close();
                    }
                });

                Scene scene = new Scene(webView, 900, 700);
                webStage.setScene(scene);
                webStage.show();
            } catch (Exception ex) {
                if (App.getAppHostServices() != null) {
                    App.getAppHostServices().showDocument(paymentUrl);
                    showAlert(Alert.AlertType.INFORMATION, "Redirection",
                            "Le lien de paiement a ete ouvert dans votre navigateur.");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Erreur de Navigation",
                            "Impossible d'ouvrir le navigateur automatiquement. Voici le lien : " + paymentUrl);
                }
            }
        });

        paymentTask.setOnFailed(evt -> {
            Throwable ex = paymentTask.getException();
            if (ex != null)
                ex.printStackTrace();
            Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Erreur Stripe",
                    "Une erreur est survenue lors de la generation du paiement : "
                            + (ex == null ? "" : ex.getMessage())));
        });

        new Thread(paymentTask, "stripe-payment-thread").start();
    }

    /**
     * Credits the freelancer's balance in the users table after a successful payment.
     */
    private void creditFreelancerBalance(int freelancerId, double amount) {
        String sql = "UPDATE users SET balance = balance + ? WHERE id = ?";
        try (Connection conn = MyDataBase.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, amount);
            stmt.setInt(2, freelancerId);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                System.out.println("[Payment] Credited " + amount + " DT to freelancer ID " + freelancerId);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Le paiement a ete effectue mais le solde du freelancer n'a pas pu etre credite: "
                            + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private String extractQueryParam(String url, String name) {
        try {
            URI uri = new URI(url);
            String query = uri.getQuery();
            if (query == null)
                return null;
            String[] pairs = query.split("&");
            for (String p : pairs) {
                int idx = p.indexOf('=');
                if (idx > 0) {
                    String key = URLDecoder.decode(p.substring(0, idx), StandardCharsets.UTF_8);
                    if (name.equals(key)) {
                        return URLDecoder.decode(p.substring(idx + 1), StandardCharsets.UTF_8);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
