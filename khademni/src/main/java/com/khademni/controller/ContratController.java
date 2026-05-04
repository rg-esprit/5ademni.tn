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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.animation.FadeTransition;
import javafx.util.Duration;
import com.khademni.service.StripeService;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.concurrent.Task;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.Optional;

import java.io.File;
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

public class ContratController {

    @FXML private VBox contratListContainer;
    @FXML private VBox contratFormView;
    @FXML private TextField searchField;
    @FXML private Label statTotal;
    @FXML private Label statPaye;
    @FXML private Label statEnAttente;
    @FXML private Label countLabel;
    @FXML private TextField freelancerSearchField;
    @FXML private VBox freelancerSearchResults;
    @FXML private HBox selectedFreelancerBox;
    @FXML private Label selectedFreelancerInitials;
    @FXML private Label selectedFreelancerName;
    @FXML private Label selectedFreelancerEmail;
    @FXML private TextField titleField;
    @FXML private TextField priceField;
    @FXML private TextArea descriptionField;
    @FXML private DatePicker dateContratPicker;
    @FXML private Label errorLabel;
    @FXML private TextField numTelephoneField;

    private ContratModel selectedContrat;
    private ContratModel contratToEdit;
    private int selectedFreelancerId = -1;
    private ObservableList<ContratModel> contratList = FXCollections.observableArrayList();
    private ExchangeRateService exchangeRateService = ExchangeRateService.getInstance();
    private StripeService stripeService = new StripeService();

    @FXML
    public void initialize() {
        exchangeRateService = ExchangeRateService.getInstance();
        setupSearch();
        setupFreelancerSearch();
        loadContrats();
    }

    private void renderContractList(List<ContratModel> contrats) {
        contratListContainer.getChildren().clear();
        countLabel.setText(contrats.size() + " contrats trouves");
        int delay = 0;
        for (ContratModel item : contrats) {
            VBox card = createListCard(item);
            card.setOpacity(0);
            FadeTransition ft = new FadeTransition(Duration.millis(400), card);
            ft.setFromValue(0); ft.setToValue(1);
            ft.setDelay(Duration.millis(delay));
            ft.play();
            contratListContainer.getChildren().add(card);
            delay += 60;
        }
    }

    private VBox createListCard(ContratModel item) {
        VBox card = new VBox();
        card.getStyleClass().add("list-card");
        card.setPadding(new Insets(20, 24, 20, 24));

        // === HEADER ROW ===
        HBox header = new HBox(14);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-cursor: hand;");

        // Avatar circle
        String initials = getInitials(item.getClientName());
        Label avatar = new Label(initials);
        avatar.setStyle("-fx-background-color: #6c5ce7; -fx-text-fill: white; -fx-font-weight: 900; " +
                "-fx-font-size: 14; -fx-padding: 12 14; -fx-background-radius: 100; -fx-min-width: 44; -fx-min-height: 44; -fx-alignment: center;");

        // Title area
        VBox titleArea = new VBox(4);
        HBox.setHgrow(titleArea, Priority.ALWAYS);

        Label badge = new Label(formatStatut(item.getStatut()));
        if ("PAYE".equalsIgnoreCase(item.getStatut())) {
            badge.getStyleClass().add("badge-success");
        } else {
            badge.getStyleClass().add("badge-warning");
        }

        Label title = new Label(item.getTitre() != null ? item.getTitre() : "Sans titre");
        title.setStyle("-fx-font-size: 16; -fx-font-weight: 800; -fx-text-fill: #1f2937;");
        title.setWrapText(true);

        Label clientInfo = new Label("Client: " + safe(item.getClientName()) + " - " + safe(item.getFreelancerName()));
        clientInfo.setStyle("-fx-font-size: 12; -fx-text-fill: #6b7280; -fx-font-weight: 600;");

        titleArea.getChildren().addAll(badge, title, clientInfo);

        // Price area
        VBox priceArea = new VBox(4);
        priceArea.setAlignment(Pos.CENTER_RIGHT);
        Label price = new Label(String.format("%,.0f TND", item.getPrix()));
        price.setStyle("-fx-font-size: 18; -fx-font-weight: 900; -fx-text-fill: #6c5ce7;");

        Label expandLabel = new Label("\u25BC Actions");
        expandLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #9ca3af; -fx-font-weight: 600;");
        priceArea.getChildren().addAll(price, expandLabel);

        header.getChildren().addAll(avatar, titleArea, priceArea);

        // === ACTION PANEL (hidden by default) ===
        VBox actionPanel = new VBox(16);
        actionPanel.getStyleClass().add("action-panel");
        actionPanel.setVisible(false);
        actionPanel.setManaged(false);

        // Info row
        HBox infoRow = new HBox(40);
        VBox descBox = new VBox(6);
        HBox.setHgrow(descBox, Priority.ALWAYS);
        Label descLabel = new Label("DESCRIPTION");
        descLabel.getStyleClass().add("info-label");
        Label descValue = new Label(item.getDescription() != null ? item.getDescription() : "Aucune description");
        descValue.getStyleClass().add("info-value");
        descValue.setWrapText(true);
        descBox.getChildren().addAll(descLabel, descValue);

        VBox phoneBox = new VBox(6);
        Label phoneLabel = new Label("TELEPHONE");
        phoneLabel.getStyleClass().add("info-label");
        Label phoneValue = new Label(item.getNumTelephone() != null ? item.getNumTelephone() : "Non specifie");
        phoneValue.getStyleClass().add("phone-value");
        phoneBox.getChildren().addAll(phoneLabel, phoneValue);

        infoRow.getChildren().addAll(descBox, phoneBox);

        // Action row with buttons
        HBox actionRow = new HBox(10);
        actionRow.setAlignment(Pos.CENTER_LEFT);
        actionRow.setPadding(new Insets(8, 0, 0, 0));

        // Currency converter
        HBox converter = new HBox(8);
        converter.setAlignment(Pos.CENTER_LEFT);
        converter.setStyle("-fx-background-color: white; -fx-padding: 6 14; -fx-background-radius: 10; -fx-border-color: #e5e7eb; -fx-border-radius: 10;");
        ComboBox<String> currCombo = new ComboBox<>(FXCollections.observableArrayList("TND", "EUR", "USD"));
        currCombo.setValue("EUR");
        currCombo.setStyle("-fx-background-color: transparent; -fx-font-weight: 600; -fx-font-size: 12;");
        double eurInit = exchangeRateService.convertPrice(item.getPrix(), "EUR");
        Label convLabel = new Label(String.format("| %,.0f TND = %.2f EUR", item.getPrix(), eurInit > 0 ? eurInit : item.getPrix() * 0.30));
        convLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #6b7280;");
        currCombo.valueProperty().addListener((o, ov, nv) -> {
            if ("TND".equals(nv)) { convLabel.setText(String.format("| %,.0f TND", item.getPrix())); }
            else {
                double c = exchangeRateService.convertPrice(item.getPrix(), nv);
                convLabel.setText(c > 0 ? String.format("| %,.0f TND = %.2f %s", item.getPrix(), c, nv) : "| Taux indisponible");
            }
        });
        converter.getChildren().addAll(currCombo, convLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button payBtn = makeBtn("\uD83D\uDCB3 Payer", "#7c3aed", "white");
        payBtn.setOnAction(e -> handlePay(item));
        Button editBtn = makeBtn("\u270F\uFE0F Editer", "white", "#374151");
        editBtn.setStyle(editBtn.getStyle() + "-fx-border-color: #e5e7eb; -fx-border-radius: 10;");
        editBtn.setOnAction(e -> handleEdit(item));
        Button pdfBtn = makeBtn("\uD83D\uDCC4 PDF", "white", "#374151");
        pdfBtn.setStyle(pdfBtn.getStyle() + "-fx-border-color: #e5e7eb; -fx-border-radius: 10;");
        pdfBtn.setOnAction(e -> generatePDF(item));
        Button auditBtn = makeBtn("\u2728 Audit IA", "#f3f0ff", "#7c3aed");
        auditBtn.setOnAction(e -> showAlert(Alert.AlertType.INFORMATION, "Audit IA", "Service IA non disponible dans cette version."));
        Button delBtn = makeBtn("\uD83D\uDDD1 Supprimer", "white", "#ef4444");
        delBtn.setStyle(delBtn.getStyle() + "-fx-border-color: #fecaca; -fx-border-radius: 10;");
        delBtn.setOnAction(e -> handleDelete(item));

        actionRow.getChildren().addAll(converter, spacer, payBtn, editBtn, pdfBtn, auditBtn, delBtn);
        actionPanel.getChildren().addAll(infoRow, actionRow);

        card.getChildren().addAll(header, actionPanel);

        // Toggle expand/collapse
        header.setOnMouseClicked(e -> {
            boolean show = !actionPanel.isVisible();
            // Collapse others
            for (var node : contratListContainer.getChildren()) {
                if (node instanceof VBox) {
                    VBox c2 = (VBox) node;
                    c2.getStyleClass().remove("list-card-selected");
                    if (c2.getChildren().size() > 1) {
                        c2.getChildren().get(1).setVisible(false);
                        c2.getChildren().get(1).setManaged(false);
                    }
                }
            }
            if (show) {
                actionPanel.setVisible(true);
                actionPanel.setManaged(true);
                card.getStyleClass().add("list-card-selected");
                expandLabel.setText("\u25B2 Reduire");
                selectedContrat = item;
            } else {
                expandLabel.setText("\u25BC Actions");
                selectedContrat = null;
            }
        });

        return card;
    }

    private Button makeBtn(String text, String bg, String fg) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + fg + "; -fx-font-weight: 700; " +
                "-fx-font-size: 12; -fx-padding: 8 16; -fx-background-radius: 10; -fx-cursor: hand;");
        return b;
    }

    private String getInitials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] p = name.trim().split("\\s+");
        if (p.length >= 2) return ("" + p[0].charAt(0) + p[1].charAt(0)).toUpperCase();
        return ("" + p[0].charAt(0)).toUpperCase();
    }

    private String safe(String s) { return s != null ? s : "N/A"; }

    private String formatStatut(String s) {
        if (s == null) return "Inconnu";
        if ("EN_ATTENTE".equalsIgnoreCase(s)) return "En attente";
        if ("PAYE".equalsIgnoreCase(s)) return "Actif";
        return s;
    }

    

    private void setupSearch() {
        searchField.textProperty().addListener((obs, o, n) -> {
            String f = n == null ? "" : n.toLowerCase().trim();
            List<ContratModel> filtered = contratList.stream()
                    .filter(c -> (c.getTitre() != null && c.getTitre().toLowerCase().contains(f))
                            || (c.getDescription() != null && c.getDescription().toLowerCase().contains(f)))
                    .collect(Collectors.toList());
            renderContractList(filtered);
        });
    }

    private void setupFreelancerSearch() {
        freelancerSearchField.textProperty().addListener((obs, o, n) -> {
            if (n == null || n.trim().length() < 2) {
                freelancerSearchResults.setVisible(false);
                freelancerSearchResults.setManaged(false);
                return;
            }
            searchFreelancers(n.trim());
        });
    }

    private void searchFreelancers(String query) {
        UserModel cu = App.getCurrentUser();
        if (cu == null) return;
        new Thread(() -> {
            try {
                String sql = "SELECT id, first_name, last_name, email FROM users WHERE id != ? AND (LOWER(first_name) LIKE ? OR LOWER(last_name) LIKE ? OR LOWER(CONCAT(first_name, ' ', last_name)) LIKE ?) LIMIT 8";
                try (Connection conn = MyDataBase.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
                    String p = "%" + query.toLowerCase() + "%";
                    stmt.setInt(1, cu.getId()); stmt.setString(2, p); stmt.setString(3, p); stmt.setString(4, p);
                    ResultSet rs = stmt.executeQuery();
                    List<Object[]> results = new ArrayList<>();
                    while (rs.next()) results.add(new Object[]{rs.getInt("id"), rs.getString("first_name"), rs.getString("last_name"), rs.getString("email")});
                    Platform.runLater(() -> {
                        freelancerSearchResults.getChildren().clear();
                        for (Object[] r : results) {
                            int uid = (int) r[0]; String fn = (String) r[1]; String ln = (String) r[2]; String em = (String) r[3];
                            HBox row = new HBox(12);
                            row.setAlignment(Pos.CENTER_LEFT);
                            row.setStyle("-fx-padding: 10 16; -fx-cursor: hand;");
                            Label av = new Label(("" + fn.charAt(0) + ln.charAt(0)).toUpperCase());
                            av.setStyle("-fx-background-color: #6c5ce7; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 10; -fx-background-radius: 50;");
                            VBox info = new VBox(2);
                            info.getChildren().addAll(new Label(fn + " " + ln), new Label(em));
                            HBox.setHgrow(info, Priority.ALWAYS);
                            row.getChildren().addAll(av, info);
                            row.setOnMouseClicked(ev -> selectFreelancer(uid, fn, ln, em));
                            row.setOnMouseEntered(ev -> row.setStyle("-fx-padding: 10 16; -fx-cursor: hand; -fx-background-color: #f3f0ff;"));
                            row.setOnMouseExited(ev -> row.setStyle("-fx-padding: 10 16; -fx-cursor: hand;"));
                            freelancerSearchResults.getChildren().add(row);
                        }
                        freelancerSearchResults.setVisible(true);
                        freelancerSearchResults.setManaged(true);
                    });
                }
            } catch (SQLException e) { e.printStackTrace(); }
        }, "freelancer-search").start();
    }

    private void selectFreelancer(int uid, String fn, String ln, String email) {
        selectedFreelancerId = uid;
        selectedFreelancerInitials.setText(("" + fn.charAt(0) + ln.charAt(0)).toUpperCase());
        selectedFreelancerName.setText(fn + " " + ln);
        selectedFreelancerEmail.setText(email);
        selectedFreelancerBox.setVisible(true); selectedFreelancerBox.setManaged(true);
        freelancerSearchField.setVisible(false); freelancerSearchField.setManaged(false);
        freelancerSearchResults.setVisible(false); freelancerSearchResults.setManaged(false);
    }

    @FXML private void clearFreelancerSelection() {
        selectedFreelancerId = -1;
        selectedFreelancerBox.setVisible(false); selectedFreelancerBox.setManaged(false);
        freelancerSearchField.setVisible(true); freelancerSearchField.setManaged(true);
        freelancerSearchField.clear();
    }

    private void loadContrats() {
        contratList.clear();
        UserModel cu = App.getCurrentUser();
        if (cu == null) return;
        String q = "SELECT c.*, u1.first_name as cfname, u1.last_name as clname, u2.first_name as ffname, u2.last_name as flname FROM contrats c LEFT JOIN users u1 ON c.client_id = u1.id LEFT JOIN users u2 ON c.freelancer_id = u2.id WHERE (c.client_id = ? OR c.freelancer_id = ?) AND c.statut = 'EN_ATTENTE'";
        try (Connection conn = MyDataBase.getConnection(); PreparedStatement ps = conn.prepareStatement(q)) {
            ps.setInt(1, cu.getId()); ps.setInt(2, cu.getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                contratList.add(new ContratModel(rs.getInt("id"), rs.getInt("client_id"), rs.getInt("freelancer_id"),
                        (rs.getString("cfname") != null ? rs.getString("cfname") : "") + " " + (rs.getString("clname") != null ? rs.getString("clname") : ""),
                        (rs.getString("ffname") != null ? rs.getString("ffname") : "") + " " + (rs.getString("flname") != null ? rs.getString("flname") : ""),
                        rs.getString("titre"), rs.getString("description"), rs.getDouble("prix"),
                        rs.getDate("date_contrat") != null ? rs.getDate("date_contrat").toLocalDate() : java.time.LocalDate.now(),
                        rs.getString("statut"), rs.getString("num_telephone")));
            }
            renderContractList(contratList);
            updateStats();
        } catch (SQLException e) { showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage()); }
    }

    private void updateStats() {
        statTotal.setText(String.valueOf(contratList.size()));
        statPaye.setText(String.valueOf(contratList.stream().filter(c -> "PAYE".equalsIgnoreCase(c.getStatut())).count()));
        statEnAttente.setText(String.valueOf(contratList.stream().filter(c -> "EN_ATTENTE".equalsIgnoreCase(c.getStatut())).count()));
    }

    private void handleEdit(ContratModel c) {
        contratToEdit = c;
        selectedFreelancerId = c.getIdFreelancer();
        String fn = c.getFreelancerName();
        selectedFreelancerInitials.setText(getInitials(fn));
        selectedFreelancerName.setText(fn != null ? fn : "?");
        selectedFreelancerEmail.setText("ID: " + c.getIdFreelancer());
        selectedFreelancerBox.setVisible(true); selectedFreelancerBox.setManaged(true);
        freelancerSearchField.setVisible(false); freelancerSearchField.setManaged(false);
        if (titleField != null) { titleField.setText(c.getTitre()); titleField.setDisable(true); }
        if (priceField != null) { priceField.setText(String.valueOf(c.getPrix())); priceField.setDisable(true); }
        descriptionField.setText(c.getDescription());
        dateContratPicker.setValue(c.getDateContrat());
        if (numTelephoneField != null && c.getNumTelephone() != null) {
            numTelephoneField.setText(c.getNumTelephone());
            numTelephoneField.setEditable(false);
        }
        contratFormView.setVisible(true); contratFormView.setManaged(true);
        contratListContainer.setVisible(false); contratListContainer.setManaged(false);
    }

    @FXML private void cancelForm() {
        resetForm();
        contratFormView.setVisible(false); contratFormView.setManaged(false);
        contratListContainer.setVisible(true); contratListContainer.setManaged(true);
    }

    @FXML private void saveContrat() {
        errorLabel.setVisible(false); errorLabel.setManaged(false);
        try {
            UserModel cu = App.getCurrentUser();
            if (cu == null) throw new Exception("Utilisateur non connecte.");
            if (contratToEdit == null) {
                String titre = titleField.getText().trim();
                String priceStr = priceField.getText().trim();
                if (selectedFreelancerId == -1) throw new Exception("Veuillez selectionner un freelancer.");
                if (titre.isEmpty()) throw new Exception("Veuillez saisir le titre.");
                if (priceStr.isEmpty()) throw new Exception("Veuillez saisir le prix.");
                double prix = Double.parseDouble(priceStr);
                if (prix <= 0) throw new Exception("Le prix doit etre superieur a zero.");
            }
            if (dateContratPicker.getValue() == null) throw new Exception("Veuillez remplir la date.");
            String phone = numTelephoneField.getText().trim();
            if (phone.isEmpty()) throw new Exception("Le telephone est obligatoire.");
            if (phone.replaceAll("[^0-9]", "").length() < 8) throw new Exception("Le telephone doit contenir au moins 8 chiffres.");

            if (contratToEdit == null) {
                String sql = "INSERT INTO contrats (client_id, freelancer_id, titre, description, prix, date_contrat, statut, num_telephone) VALUES (?, ?, ?, ?, ?, ?, 'EN_ATTENTE', ?)";
                try (Connection conn = MyDataBase.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, cu.getId()); stmt.setInt(2, selectedFreelancerId);
                    stmt.setString(3, titleField.getText().trim()); stmt.setString(4, descriptionField.getText());
                    stmt.setDouble(5, Double.parseDouble(priceField.getText().trim()));
                    stmt.setDate(6, java.sql.Date.valueOf(dateContratPicker.getValue()));
                    stmt.setString(7, phone); stmt.executeUpdate();
                }
            } else {
                String sql = "UPDATE contrats SET description = ?, date_contrat = ? WHERE id = ?";
                try (Connection conn = MyDataBase.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, descriptionField.getText());
                    stmt.setDate(2, java.sql.Date.valueOf(dateContratPicker.getValue()));
                    stmt.setInt(3, contratToEdit.getIdContrat()); stmt.executeUpdate();
                }
            }
            showAlert(Alert.AlertType.INFORMATION, "Succes", "Contrat sauvegarde.");
            cancelForm();
            loadContrats();
        } catch (Exception e) { showError(e.getMessage()); }
    }

    @FXML private void resetForm() {
        contratToEdit = null; selectedFreelancerId = -1;
        freelancerSearchField.clear(); freelancerSearchField.setVisible(true); freelancerSearchField.setManaged(true);
        freelancerSearchResults.setVisible(false); freelancerSearchResults.setManaged(false);
        selectedFreelancerBox.setVisible(false); selectedFreelancerBox.setManaged(false);
        if (titleField != null) { titleField.clear(); titleField.setDisable(false); }
        if (priceField != null) { priceField.clear(); priceField.setDisable(false); }
        descriptionField.clear(); numTelephoneField.clear(); numTelephoneField.setEditable(true);
        dateContratPicker.setValue(null);
    }

    private void handleDelete(ContratModel c) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer ce contrat ?", ButtonType.YES, ButtonType.NO);
        if (a.showAndWait().get() == ButtonType.YES) {
            try (Connection conn = MyDataBase.getConnection(); PreparedStatement stmt = conn.prepareStatement("DELETE FROM contrats WHERE id = ?")) {
                stmt.setInt(1, c.getIdContrat()); stmt.executeUpdate(); loadContrats();
            } catch (SQLException e) { showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage()); }
        }
    }

    private void generatePDF(ContratModel c) {
        String home = System.getProperty("user.home");
        File dl = new File(home, "Downloads");
        if (!dl.exists()) dl = new File(home, "Documents");
        try {
            String fn = ("PAYE".equalsIgnoreCase(c.getStatut()) ? "Contrat_Paye_" : "Contrat_") + c.getIdContrat() + ".pdf";
            File out = new File(dl, fn);
            com.lowagie.text.Document doc = new com.lowagie.text.Document(com.lowagie.text.PageSize.A4, 50, 50, 50, 50);
            com.lowagie.text.pdf.PdfWriter.getInstance(doc, new FileOutputStream(out));
            doc.open();
            java.awt.Color purple = new java.awt.Color(108, 92, 231);
            java.awt.Color dark = new java.awt.Color(26, 26, 26);
            java.awt.Color gray = new java.awt.Color(107, 114, 128);
            com.lowagie.text.Font tF = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 22, com.lowagie.text.Font.BOLD, java.awt.Color.WHITE);
            com.lowagie.text.Font sF = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 11, com.lowagie.text.Font.NORMAL, java.awt.Color.WHITE);
            com.lowagie.text.Font hF = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 16, com.lowagie.text.Font.BOLD, dark);
            com.lowagie.text.Font lF = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 10, com.lowagie.text.Font.BOLD, gray);
            com.lowagie.text.Font vF = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 12, com.lowagie.text.Font.NORMAL, dark);
            com.lowagie.text.Font pF = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 20, com.lowagie.text.Font.BOLD, purple);
            com.lowagie.text.Font smF = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9, com.lowagie.text.Font.NORMAL, gray);

            com.lowagie.text.pdf.PdfPTable ht = new com.lowagie.text.pdf.PdfPTable(1);
            ht.setWidthPercentage(100);
            com.lowagie.text.pdf.PdfPCell hc = new com.lowagie.text.pdf.PdfPCell();
            hc.setBackgroundColor(purple); hc.setPadding(20); hc.setBorder(0);
            hc.addElement(new com.lowagie.text.Paragraph("CONTRAT DE PRESTATION DE SERVICES", tF));
            hc.addElement(new com.lowagie.text.Paragraph("Reference: CT-2026-" + String.format("%04d", c.getIdContrat()), sF));
            ht.addCell(hc); doc.add(ht); doc.add(new com.lowagie.text.Paragraph(" "));
            doc.add(new com.lowagie.text.Paragraph(safe(c.getTitre()), hF));
            doc.add(new com.lowagie.text.Paragraph(" "));

            com.lowagie.text.pdf.PdfPTable it = new com.lowagie.text.pdf.PdfPTable(2);
            it.setWidthPercentage(100); it.setWidths(new float[]{30, 70});
            addRow(it, "Date", c.getDateContrat() != null ? c.getDateContrat().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "N/A", lF, vF);
            addRow(it, "Statut", safe(c.getStatut()), lF, vF);
            addRow(it, "Client", safe(c.getClientName()), lF, vF);
            addRow(it, "Freelancer", safe(c.getFreelancerName()), lF, vF);
            addRow(it, "Telephone", c.getNumTelephone() != null ? c.getNumTelephone() : "N/A", lF, vF);
            doc.add(it); doc.add(new com.lowagie.text.Paragraph(" "));
            doc.add(new com.lowagie.text.Paragraph("Montant: " + String.format("%.2f", c.getPrix()) + " TND", pF));
            double eurP = exchangeRateService.convertPrice(c.getPrix(), "EUR");
            double usdP = exchangeRateService.convertPrice(c.getPrix(), "USD");
            String cv = "";
            if (eurP > 0) cv += "~ " + String.format("%.2f", eurP) + " EUR";
            if (usdP > 0) cv += "  |  ~ " + String.format("%.2f", usdP) + " USD";
            if (!cv.isEmpty()) doc.add(new com.lowagie.text.Paragraph(cv, smF));
            doc.add(new com.lowagie.text.Paragraph(" "));
            doc.add(new com.lowagie.text.Paragraph("Description", hF));
            doc.add(new com.lowagie.text.Paragraph(safe(c.getDescription()), vF));
            doc.add(new com.lowagie.text.Paragraph(" ")); doc.add(new com.lowagie.text.Paragraph(" "));

            com.lowagie.text.pdf.PdfPTable sg = new com.lowagie.text.pdf.PdfPTable(2);
            sg.setWidthPercentage(100);
            com.lowagie.text.pdf.PdfPCell sc = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase("Signature Client\n\n\n_________________", vF));
            sc.setHorizontalAlignment(1); sc.setBorder(0);
            com.lowagie.text.pdf.PdfPCell sf = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase("Signature Freelancer\n\n\n_________________", vF));
            sf.setHorizontalAlignment(1); sf.setBorder(0);
            sg.addCell(sc); sg.addCell(sf); doc.add(sg);
            doc.add(new com.lowagie.text.Paragraph(" "));
            com.lowagie.text.Paragraph ft = new com.lowagie.text.Paragraph("5ademni.tn - Genere le " + java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), smF);
            ft.setAlignment(1); doc.add(ft);
            doc.close();
            showAlert(Alert.AlertType.INFORMATION, "Succes", "PDF genere: " + out.getName());
            try { java.awt.Desktop.getDesktop().open(out); } catch (Exception ex) {}
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur PDF", e.getMessage());
        }
    }

    private void addRow(com.lowagie.text.pdf.PdfPTable t, String l, String v, com.lowagie.text.Font lf, com.lowagie.text.Font vf) {
        com.lowagie.text.pdf.PdfPCell lc = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(l, lf));
        lc.setBackgroundColor(new java.awt.Color(249, 249, 249)); lc.setPadding(10);
        com.lowagie.text.pdf.PdfPCell vc = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(v, vf));
        vc.setPadding(10); t.addCell(lc); t.addCell(vc);
    }

    @FXML private void goToPaiement() throws IOException { App.setRoot("paiement"); }

    private void showAlert(Alert.AlertType t, String title, String msg) {
        Alert a = new Alert(t); a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.show();
    }

    private void showError(String msg) { errorLabel.setText(msg); errorLabel.setVisible(true); errorLabel.setManaged(true); }
    private void handlePay(ContratModel c) {
        UserModel cu = App.getCurrentUser();
        if (cu == null || cu.getEmail() == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Utilisateur non connecte ou email manquant.");
            return;
        }

        String otp = String.format("%06d", new Random().nextInt(1000000));
        try {
            com.khademni.utils.EmailService.sendEmail(cu.getEmail(), "Verification de Paiement - Khademni", 
                "Votre code de verification pour le paiement du contrat '" + c.getTitre() + "' est : " + otp);
            
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Verification par Email");
            dialog.setHeaderText("Un code de verification a ete envoye a " + cu.getEmail());
            dialog.setContentText("Veuillez saisir le code OTP :");

            Optional<String> result = dialog.showAndWait();
            if (result.isPresent() && result.get().equals(otp)) {
                proceedWithStripePayment(c);
            } else if (result.isPresent()) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Code OTP incorrect.");
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur Email", "Impossible d'envoyer l'email : " + e.getMessage());
        }
    }

    private void proceedWithStripePayment(ContratModel c) {
        String expectedPhone = c.getNumTelephone();
        String description = "Paiement Contrat ID: " + c.getIdContrat() + " - " + c.getTitre();
        double amountTND = c.getPrix();
        
        // Convert to EUR for Stripe (assuming Stripe doesn't support TND or you want EUR)
        double initialCharge = exchangeRateService.convertPrice(amountTND, "EUR");
        double tempCharge = (initialCharge < 0) ? (amountTND * 0.3) : initialCharge;
        if (tempCharge < 0.5) tempCharge = 0.5; // Stripe minimum
        final double chargeAmount = tempCharge;

        Task<String> paymentTask = new Task<>() {
            @Override protected String call() throws Exception {
                System.out.println("[ContratController] Calling Stripe for amount: " + chargeAmount + " EUR");
                return stripeService.createCheckoutSession(chargeAmount, "eur", description, c.getIdContrat(), expectedPhone);
            }
        };

        paymentTask.setOnSucceeded(evt -> {
            String url = paymentTask.getValue();
            if (url == null || url.isBlank()) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Lien Stripe non genere.");
                return;
            }
            Platform.runLater(() -> {
                Stage webStage = new Stage();
                webStage.initModality(Modality.APPLICATION_MODAL);
                webStage.setTitle("Paiement Stripe");
                WebView wv = new WebView();
                WebEngine engine = wv.getEngine();
                engine.load(url);
                engine.locationProperty().addListener((obs, oldLoc, newLoc) -> {
                    if (newLoc != null && newLoc.toLowerCase().contains("/success")) {
                        String sessionId = extractQueryParam(newLoc, "session_id");
                        verifyPayment(sessionId, c, webStage);
                    } else if (newLoc != null && newLoc.toLowerCase().contains("/cancel")) {
                        webStage.close();
                        showAlert(Alert.AlertType.WARNING, "Annule", "Paiement annule.");
                    }
                });
                webStage.setScene(new Scene(wv, 900, 700));
                webStage.show();
            });
        });

        paymentTask.setOnFailed(evt -> {
            Throwable ex = paymentTask.getException();
            if (ex != null) ex.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur Stripe", "Impossible de generer le paiement : " + (ex != null ? ex.getMessage() : "Inconnu"));
        });
        new Thread(paymentTask).start();
    }

    private void verifyPayment(String sessionId, ContratModel c, Stage webStage) {
        Task<Object[]> verifyTask = new Task<>() {
            @Override protected Object[] call() throws Exception {
                return stripeService.verifySessionWithPhone(sessionId);
            }
        };
        verifyTask.setOnSucceeded(v -> {
            Object[] res = verifyTask.getValue();
            boolean paid = (boolean) res[0];
            boolean phoneMatch = (boolean) res[1];
            if (paid && phoneMatch) {
                updateContratStatus(c, "PAYE");
                savePaymentRecord(c, sessionId);
                webStage.close();
                showAlert(Alert.AlertType.INFORMATION, "Succes", "Paiement effectue avec succes !");
                loadContrats();
            } else {
                showAlert(Alert.AlertType.ERROR, "Echec", "Verification echouee (Paiement ou Telephone).");
            }
        });
        new Thread(verifyTask).start();
    }

    private void updateContratStatus(ContratModel c, String status) {
        try (Connection conn = MyDataBase.getConnection(); 
             PreparedStatement ps = conn.prepareStatement("UPDATE contrats SET statut = ? WHERE id = ?")) {
            ps.setString(1, status);
            ps.setInt(2, c.getIdContrat());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void savePaymentRecord(ContratModel c, String sid) {
        try (Connection conn = MyDataBase.getConnection(); 
             PreparedStatement ps = conn.prepareStatement("INSERT INTO payments (contrat_id, stripe_session_id, amount, status) VALUES (?, ?, ?, 'PAID')")) {
            ps.setInt(1, c.getIdContrat());
            ps.setString(2, sid);
            ps.setDouble(3, c.getPrix());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private String extractQueryParam(String url, String name) {
        try {
            URI uri = new URI(url);
            String q = uri.getQuery();
            if (q == null) return null;
            for (String p : q.split("&")) {
                String[] kv = p.split("=");
                if (kv.length > 1 && URLDecoder.decode(kv[0], StandardCharsets.UTF_8).equals(name)) {
                    return URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                }
            }
        } catch (Exception e) {}
        return null;
    }
}