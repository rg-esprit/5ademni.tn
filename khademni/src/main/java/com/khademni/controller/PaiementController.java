package com.khademni.controller;

import com.khademni.App;
import com.khademni.model.ContratModel;
import com.khademni.model.UserModel;
import com.khademni.service.StripeService;
import com.khademni.utils.MyDataBase;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.event.ActionEvent;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class PaiementController {

    @FXML
    private TableView<ContratModel> contratStatusTable;
    @FXML
    private TableColumn<ContratModel, Integer> colId;
    @FXML
    private TableColumn<ContratModel, String> colFreelancer;
    @FXML
    private TableColumn<ContratModel, String> colClient;
    @FXML
    private TableColumn<ContratModel, String> colStatut;

    @FXML
    private HBox floatingActionBar;
    @FXML
    private Button fabPayeBtn;
    @FXML
    private Button fabAttenteBtn;
    @FXML
    private Button fabAnnulerBtn;

    private ObservableList<ContratModel> contratList = FXCollections.observableArrayList();
    private final StripeService stripeService = new StripeService();

    @FXML
    public void initialize() {
        // Show sequential row number (1, 2, 3...) instead of database ID
        colId.setCellFactory(col -> new TableCell<ContratModel, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText("");
                } else {
                    ContratModel contrat = getTableView().getItems().get(getIndex());
                    int originalIndex = contratList.indexOf(contrat) + 1;
                    setText(String.valueOf(originalIndex));
                }
            }
        });
        colFreelancer.setCellValueFactory(new PropertyValueFactory<>("freelancerName"));
        colClient.setCellValueFactory(new PropertyValueFactory<>("clientName"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

        setupFloatingActionBar();
        loadContrats();
    }

    private void loadContrats() {
        contratList.clear();
        UserModel currentUser = App.getCurrentUser();
        if (currentUser == null)
            return;
        int currentUserId = currentUser.getId();

        String query = "SELECT c.*, " +
                "u1.first_name as cfname, u1.last_name as clname, u1.unique_id as cuid, " +
                "u2.first_name as ffname, u2.last_name as flname, u2.unique_id as fuid " +
                "FROM contrats c " +
                "LEFT JOIN users u1 ON c.client_id = u1.id " +
                "LEFT JOIN users u2 ON c.freelancer_id = u2.id " +
                "WHERE c.client_id = ? OR c.freelancer_id = ?";

        try (Connection conn = MyDataBase.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, currentUserId);
            pstmt.setInt(2, currentUserId);

            try (ResultSet rs = pstmt.executeQuery()) {

                while (rs.next()) {
                    java.sql.Date sqlDate = rs.getDate("date_contrat");
                    java.time.LocalDate localDate = sqlDate == null ? null : sqlDate.toLocalDate();

                    String cfName = rs.getString("cfname");
                    String clName = rs.getString("clname");
                    String ffName = rs.getString("ffname");
                    String flName = rs.getString("flname");

                    String clientName = (cfName != null && clName != null) ? (cfName + " " + clName) : "Unknown Client";
                    String freelancerName = (ffName != null && flName != null) ? (ffName + " " + flName)
                            : "Unknown Freelancer";

                    contratList.add(new ContratModel(
                            rs.getInt("id"),
                            rs.getInt("client_id"),
                            rs.getInt("freelancer_id"),
                            rs.getInt("cuid"),
                            rs.getInt("fuid"),
                            clientName,
                            freelancerName,
                            rs.getString("titre"),
                            rs.getString("description"),
                            rs.getDouble("prix"),
                            localDate,
                            rs.getString("statut")));
                }
            } // Close inner ResultSet try
            contratStatusTable.setItems(contratList);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les contrats : " + e.getMessage());
        }
    }

    private void setupFloatingActionBar() {
        contratStatusTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
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
    private void onFabPaye() {
        ContratModel selected = contratStatusTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            updateStatus(selected, "PAYE");
        }
    }

    @FXML
    private void onFabAttente() {
        ContratModel selected = contratStatusTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            updateStatus(selected, "EN_ATTENTE");
        }
    }

    @FXML
    private void onFabAnnuler() {
        ContratModel selected = contratStatusTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            updateStatus(selected, "ANNULE");
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
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Statut mis à jour !");
                loadContrats();
            } else {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Aucun contrat mis à jour.");
            }
        });

        updateTask.setOnFailed(evt -> {
            Throwable ex = updateTask.getException();
            ex.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible de mettre à jour le statut: " + (ex == null ? "" : ex.getMessage()));
        });

        new Thread(updateTask, "update-status-thread").start();
    }

    @FXML
    private void goToContrats() throws IOException {
        App.setRoot("contrat");
    }

    @FXML
    public void handlePay(ActionEvent event) {
        ContratModel selectedContrat = contratStatusTable.getSelectionModel().getSelectedItem();

        if (selectedContrat == null) {
            try {
                App.setRoot("paiement_form");
            } catch (IOException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le formulaire de paiement.");
            }
            return;
        }

        if ("PAYE".equalsIgnoreCase(selectedContrat.getStatut())) {
            showAlert(Alert.AlertType.INFORMATION, "Déjà Payé", "Ce contrat est déjà marqué comme payé.");
            return;
        }

        String description = "Paiement Contrat ID: " + selectedContrat.getIdContrat() + " - "
                + selectedContrat.getTitre();
        double amount = selectedContrat.getPrix();

        showAlert(Alert.AlertType.INFORMATION, "Génération du lien",
                "Génération du lien de paiement Stripe pour " + amount + " DT...");

        Task<String> paymentTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                return stripeService.createCheckoutSession(amount, description, selectedContrat.getIdContrat());
            }
        };

        paymentTask.setOnSucceeded(evt -> {
            String paymentUrl = paymentTask.getValue();

            // Open payment page inside the application using an embedded WebView
            try {
                Stage webStage = new Stage();
                webStage.initModality(Modality.APPLICATION_MODAL);
                webStage.setTitle("Paiement Stripe");

                WebView webView = new WebView();
                WebEngine engine = webView.getEngine();
                engine.load(paymentUrl);

                // Listen for navigation to detect success/failure redirects
                engine.locationProperty().addListener((obs, oldLoc, newLoc) -> {
                    if (newLoc == null)
                        return;
                    String lower = newLoc.toLowerCase();
                    if (lower.contains("/success") && lower.contains("session_id")) {
                        // Extract Stripe checkout session_id from query if present and verify
                        String sessionId = extractQueryParam(newLoc, "session_id");
                        String cIdStr = extractQueryParam(newLoc, "contrat_id");

                        Stage progressStage = new Stage();
                        ProgressIndicator pi = new ProgressIndicator();
                        Scene ps = new Scene(pi, 120, 120);
                        progressStage.initOwner(webStage);
                        progressStage.initModality(Modality.APPLICATION_MODAL);
                        progressStage.setScene(ps);
                        progressStage.setTitle("Vérification du paiement...");
                        progressStage.show();

                        Task<Boolean> verifyTask = new Task<>() {
                            @Override
                            protected Boolean call() throws Exception {
                                if (sessionId == null || sessionId.isBlank()) {
                                    return false;
                                }
                                boolean verified = stripeService.verifySession(sessionId);
                                if (verified) {
                                    // Insert into payments table
                                    String insertSQL = "INSERT INTO payments (contrat_id, stripe_session_id, amount, status) VALUES (?, ?, ?, 'PAID')";
                                    try (Connection conn = MyDataBase.getConnection();
                                            PreparedStatement stmt = conn.prepareStatement(insertSQL)) {
                                        stmt.setInt(1, Integer.parseInt(cIdStr != null ? cIdStr : "0"));
                                        stmt.setString(2, sessionId);
                                        stmt.setDouble(3, amount);
                                        stmt.executeUpdate();
                                    }
                                }
                                return verified;
                            }
                        };

                        verifyTask.setOnSucceeded(v -> {
                            Boolean ok = verifyTask.getValue();
                            progressStage.close();
                            if (ok != null && ok) {
                                if (selectedContrat != null)
                                    updateStatus(selectedContrat, "PAYE");
                                showAlert(Alert.AlertType.INFORMATION, "Paiement réussi",
                                        "Le paiement a été vérifié et enregistré.");
                            } else {
                                showAlert(Alert.AlertType.ERROR, "Vérification échouée",
                                        "Le paiement n'a pas pu être vérifié.");
                            }
                            webStage.close();
                        });

                        verifyTask.setOnFailed(v -> {
                            progressStage.close();
                            Throwable ex = verifyTask.getException();
                            ex.printStackTrace();
                            showAlert(Alert.AlertType.ERROR, "Vérification erreur",
                                    "Erreur lors de la vérification du paiement: "
                                            + (ex == null ? "" : ex.getMessage()));
                            webStage.close();
                        });

                        new Thread(verifyTask, "stripe-verify-thread").start();

                    } else if (lower.contains("/cancel")) {
                        showAlert(Alert.AlertType.ERROR, "Paiement Annulé", "Vous avez annulé le paiement.");
                        webStage.close();
                    }
                });

                Scene scene = new Scene(webView, 900, 700);
                webStage.setScene(scene);
                webStage.show();
            } catch (Exception ex) {
                // fallback to external browser if embedded view fails
                if (App.getAppHostServices() != null) {
                    App.getAppHostServices().showDocument(paymentUrl);
                    showAlert(Alert.AlertType.INFORMATION, "Redirection",
                            "Le lien de paiement a été ouvert dans votre navigateur.");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Erreur de Navigation",
                            "Impossible d'ouvrir le navigateur automatiquement. Voici le lien : " + paymentUrl);
                }
            }
        });

        paymentTask.setOnFailed(evt -> {
            Throwable ex = paymentTask.getException();
            ex.printStackTrace();
            Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Erreur Stripe",
                    "Une erreur est survenue lors de la génération du paiement : "
                            + (ex == null ? "" : ex.getMessage())));
        });

        new Thread(paymentTask, "stripe-payment-thread").start();
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
