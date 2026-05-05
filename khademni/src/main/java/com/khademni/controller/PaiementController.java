package com.khademni.controller;

import com.khademni.App;
import com.khademni.model.PaymentModel;
import com.khademni.model.UserModel;
import com.khademni.service.ExchangeRateService;
import com.khademni.utils.CustomAlert;
import com.khademni.utils.MyDataBase;
import javafx.animation.FadeTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.application.Platform;
import javafx.util.Duration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PaiementController {

    @FXML
    private VBox paiementListContainer;
    @FXML
    private TextField searchField;
    @FXML
    private Label paymentCountLabel;
    @FXML
    private Label totalPaidLabel;
    @FXML
    private Label countLabel;

    private ObservableList<PaymentModel> paymentList = FXCollections.observableArrayList();
    private ExchangeRateService exchangeRateService = ExchangeRateService.getInstance();

    @FXML
    public void initialize() {
        setupSearch();
        loadPayments();
    }

    private void setupSearch() {
        searchField.textProperty().addListener((obs, o, n) -> {
            String f = n == null ? "" : n.toLowerCase().trim();
            List<PaymentModel> filtered = paymentList.stream()
                    .filter(p -> (p.getContractTitle() != null && p.getContractTitle().toLowerCase().contains(f))
                            || (p.getStatus() != null && p.getStatus().toLowerCase().contains(f)))
                    .collect(Collectors.toList());
            renderList(filtered);
        });
    }

    private void renderList(List<PaymentModel> list) {
        paiementListContainer.getChildren().clear();
        if (list.isEmpty()) {
            Label empty = new Label("Aucun paiement trouvé.");
            empty.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 16;");
            paiementListContainer.getChildren().add(empty);
            countLabel.setText("0 transaction");
            return;
        }
        int delay = 0;
        for (PaymentModel p : list) {
            VBox card = createPaymentCard(p);
            card.setOpacity(0);
            FadeTransition ft = new FadeTransition(Duration.millis(400), card);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.setDelay(Duration.millis(delay));
            ft.play();
            paiementListContainer.getChildren().add(card);
            delay += 60;
        }
        countLabel.setText(list.size() + (list.size() > 1 ? " transactions" : " transaction"));
    }

    private VBox createPaymentCard(PaymentModel item) {
        VBox card = new VBox(0);
        card.getStyleClass().add("list-card");

        // === HEADER AREA ===
        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-padding: 24; -fx-cursor: hand;");

        // Logo Area (Bento style)
        StackPane logoBox = new StackPane();
        logoBox.setPrefSize(48, 48);
        logoBox.setMinSize(48, 48);
        logoBox.setStyle("-fx-background-color: white; -fx-background-radius: 12; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 2);");

        String titleText = item.getContractTitle() != null ? item.getContractTitle() : "Unknown";
        String cleanName = titleText.toLowerCase()
                .replaceAll("(?i)(inc|corp|ltd|llc|group|solutions|tech|client|customer)\\.?$", "")
                .trim();
        
        String domain = "placeholder.com";
        String lowerTitle = titleText.toLowerCase();
        
        if (lowerTitle.contains("netflix")) domain = "netflix.com";
        else if (lowerTitle.contains("google")) domain = "google.com";
        else if (lowerTitle.contains("apple")) domain = "apple.com";
        else if (lowerTitle.contains("microsoft")) domain = "microsoft.com";
        else if (lowerTitle.contains("amazon")) domain = "amazon.com";
        else if (lowerTitle.contains("facebook") || lowerTitle.contains("meta")) domain = "facebook.com";
        
        if (domain.equals("placeholder.com")) {
            String[] words = cleanName.split("\\s+");
            if (words.length > 0 && !words[0].isEmpty()) {
                domain = words[0] + ".com";
            }
        }
        
        String logoUrl = "https://www.google.com/s2/favicons?domain=" + domain + "&sz=128";

        ImageView logoView = new ImageView();
        logoView.setFitWidth(32);
        logoView.setFitHeight(32);
        logoView.setPreserveRatio(true);

        Label initialsFallback = new Label(getInitials(titleText));
        initialsFallback.setStyle("-fx-text-fill: #10b981; -fx-font-weight: 900; -fx-font-size: 14;");

        // Load image
        Image img = new Image(logoUrl, true);
        
        final String finalDomain = domain;
        
        img.progressProperty().addListener((obs, old, progress) -> {
            if (progress.doubleValue() == 1.0) {
                Platform.runLater(() -> {
                    if (!img.isError() && img.getWidth() > 1) {
                        logoView.setImage(img);
                        logoView.setVisible(true);
                        initialsFallback.setVisible(false);
                    }
                });
            }
        });

        logoView.setVisible(false);
        initialsFallback.setVisible(true);
        logoBox.getChildren().addAll(initialsFallback, logoView);

        // Title Area
        VBox titleArea = new VBox(4);
        HBox.setHgrow(titleArea, Priority.ALWAYS);
        Label title = new Label(item.getContractTitle());
        title.setStyle("-fx-font-size: 16; -fx-font-weight: 800; -fx-text-fill: #1f2937;");
        Label subtitle = new Label("ID Transaction: #" + item.getId());
        subtitle.setStyle("-fx-font-size: 12; -fx-text-fill: #6b7280;");
        titleArea.getChildren().addAll(title, subtitle);

        // Price Area
        VBox priceArea = new VBox(4);
        priceArea.setAlignment(Pos.CENTER_RIGHT);
        Label price = new Label(String.format("%,.0f TND", item.getAmount()));
        price.setStyle("-fx-font-size: 18; -fx-font-weight: 900; -fx-text-fill: #10b981;");

        Label expandLabel = new Label("v Details");
        expandLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #9ca3af; -fx-font-weight: 600;");
        priceArea.getChildren().addAll(price, expandLabel);

        header.getChildren().addAll(logoBox, titleArea, priceArea);

        // === ACTION PANEL (hidden) ===
        VBox actionPanel = new VBox(16);
        actionPanel.setStyle("-fx-padding: 0 24 24 24;");
        actionPanel.setVisible(false);
        actionPanel.setManaged(false);

        // Info row
        HBox infoRow = new HBox(40);
        
        VBox statusBox = new VBox(6);
        Label statusLabel = new Label("STATUT");
        statusLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #9ca3af; -fx-font-weight: 800;");
        Label statusValue = new Label(item.getStatus());
        statusValue.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold; -fx-font-size: 13;");
        statusBox.getChildren().addAll(statusLabel, statusValue);

        VBox dateBox = new VBox(6);
        Label dateLabel = new Label("DATE PAIEMENT");
        dateLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #9ca3af; -fx-font-weight: 800;");
        Label dateValue = new Label(
                item.getCreatedAt() != null ? item.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                        : "N/A");
        dateValue.setStyle("-fx-font-size: 13; -fx-text-fill: #374151;");
        dateBox.getChildren().addAll(dateLabel, dateValue);

        infoRow.getChildren().addAll(statusBox, dateBox);

        // Action row - only PDF button
        HBox actionRow = new HBox(10);
        actionRow.setAlignment(Pos.CENTER_LEFT);
        actionRow.setPadding(new Insets(8, 0, 0, 0));

        // Currency info
        HBox converter = new HBox(8);
        converter.setAlignment(Pos.CENTER_LEFT);
        converter.setStyle(
                "-fx-background-color: #f9fafb; -fx-padding: 6 14; -fx-background-radius: 10; -fx-border-color: #e5e7eb; -fx-border-radius: 10;");
        double eurVal = exchangeRateService.convertPrice(item.getAmount(), "EUR");
        Label convLabel = new Label(
                String.format("%,.0f TND = %.2f EUR", item.getAmount(), eurVal > 0 ? eurVal : item.getAmount() * 0.30));
        convLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #6b7280;");
        converter.getChildren().add(convLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button pdfBtn = new Button("\uD83D\uDCC4 Telecharger PDF");
        pdfBtn.setStyle("-fx-background-color: #111827; -fx-text-fill: white; -fx-font-weight: 700; " +
                "-fx-font-size: 13; -fx-padding: 10 24; -fx-background-radius: 10; -fx-cursor: hand;");
        pdfBtn.setOnAction(e -> generatePDF(item));

        actionRow.getChildren().addAll(converter, spacer, pdfBtn);
        actionPanel.getChildren().addAll(infoRow, actionRow);
        card.getChildren().addAll(header, actionPanel);

        // Toggle
        header.setOnMouseClicked(e -> {
            boolean show = !actionPanel.isVisible();
            if (show) {
                actionPanel.setVisible(true);
                actionPanel.setManaged(true);
                card.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0;");
                expandLabel.setText("^ Reduire");
            } else {
                actionPanel.setVisible(false);
                actionPanel.setManaged(false);
                card.setStyle("-fx-background-color: white; -fx-border-color: transparent;");
                expandLabel.setText("v Details");
            }
        });

        return card;
    }

    private void loadPayments() {
        paymentList.clear();
        UserModel cu = App.getCurrentUser();
        if (cu == null)
            return;
        String q = "SELECT p.*, c.titre FROM payments p JOIN contrats c ON p.contrat_id = c.id WHERE (c.client_id = ? OR c.freelancer_id = ?) ORDER BY p.created_at DESC";
        try (Connection conn = MyDataBase.getConnection(); PreparedStatement ps = conn.prepareStatement(q)) {
            ps.setInt(1, cu.getId());
            ps.setInt(2, cu.getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                PaymentModel p = new PaymentModel(
                    rs.getInt("id"),
                    rs.getInt("contrat_id"),
                    rs.getString("stripe_session_id"),
                    rs.getDouble("amount"),
                    rs.getString("status"),
                    rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : LocalDateTime.now()
                );
                p.setContractTitle(rs.getString("titre"));
                paymentList.add(p);
            }
            renderList(paymentList);
            updateStats();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateStats() {
        paymentCountLabel.setText(String.valueOf(paymentList.size()));
        double total = paymentList.stream().mapToDouble(PaymentModel::getAmount).sum();
        totalPaidLabel.setText(String.format("%,.0f TND", total));
    }

    private void generatePDF(PaymentModel item) {
        String home = System.getProperty("user.home");
        File dl = new File(home, "Downloads");
        if (!dl.exists())
            dl = new File(home, "Documents");
        try {
            String fn = "Recu_Paiement_" + item.getId() + ".pdf";
            File out = new File(dl, fn);
            com.lowagie.text.Document doc = new com.lowagie.text.Document(com.lowagie.text.PageSize.A4, 50, 50, 50, 50);
            com.lowagie.text.pdf.PdfWriter.getInstance(doc, new FileOutputStream(out));
            doc.open();
            java.awt.Color green = new java.awt.Color(16, 185, 129);
            java.awt.Color dark = new java.awt.Color(26, 26, 26);
            java.awt.Color gray = new java.awt.Color(107, 114, 128);
            com.lowagie.text.Font tF = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 22,
                    com.lowagie.text.Font.BOLD, java.awt.Color.WHITE);
            com.lowagie.text.Font sF = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 11,
                    com.lowagie.text.Font.NORMAL, java.awt.Color.WHITE);
            com.lowagie.text.Font hF = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 16,
                    com.lowagie.text.Font.BOLD, dark);
            com.lowagie.text.Font lF = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 10,
                    com.lowagie.text.Font.BOLD, gray);
            com.lowagie.text.Font vF = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 12,
                    com.lowagie.text.Font.NORMAL, dark);
            com.lowagie.text.Font pF = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 20,
                    com.lowagie.text.Font.BOLD, green);
            com.lowagie.text.Font smF = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9,
                    com.lowagie.text.Font.NORMAL, gray);

            com.lowagie.text.pdf.PdfPTable ht = new com.lowagie.text.pdf.PdfPTable(1);
            ht.setWidthPercentage(100);
            com.lowagie.text.pdf.PdfPCell hc = new com.lowagie.text.pdf.PdfPCell();
            hc.setBackgroundColor(new java.awt.Color(17, 24, 39));
            hc.setPadding(20);
            hc.setBorder(0);
            hc.addElement(new com.lowagie.text.Paragraph("RECU DE PAIEMENT", tF));
            hc.addElement(
                    new com.lowagie.text.Paragraph("Reference Transaction: #" + item.getId(), sF));
            ht.addCell(hc);
            doc.add(ht);
            doc.add(new com.lowagie.text.Paragraph(" "));

            com.lowagie.text.pdf.PdfPTable it = new com.lowagie.text.pdf.PdfPTable(2);
            it.setWidthPercentage(100);
            it.setWidths(new float[] { 30, 70 });
            addRow(it, "Contrat", safe(item.getContractTitle()), lF, vF);
            addRow(it, "Date Paiement",
                    item.getCreatedAt() != null ? item.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                            : "N/A",
                    lF, vF);
            addRow(it, "Status", item.getStatus(), lF, vF);
            addRow(it, "Session Stripe", item.getStripeSessionId(), lF, vF);
            doc.add(it);
            doc.add(new com.lowagie.text.Paragraph(" "));
            doc.add(new com.lowagie.text.Paragraph("Montant: " + String.format("%.2f", item.getAmount()) + " TND", pF));
            double eurP = exchangeRateService.convertPrice(item.getAmount(), "EUR");
            if (eurP > 0)
                doc.add(new com.lowagie.text.Paragraph("~ " + String.format("%.2f", eurP) + " EUR", smF));
            doc.add(new com.lowagie.text.Paragraph(" "));
            com.lowagie.text.Paragraph ftPara = new com.lowagie.text.Paragraph(
                    "5ademni.tn - Genere le "
                            + java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                    smF);
            ftPara.setAlignment(1);
            doc.add(ftPara);
            doc.close();
            showAlert(Alert.AlertType.INFORMATION, "Succes", "PDF genere: " + out.getName());
            try {
                java.awt.Desktop.getDesktop().open(out);
            } catch (Exception ex) {
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur PDF", e.getMessage());
        }
    }

    private void addRow(com.lowagie.text.pdf.PdfPTable t, String l, String v, com.lowagie.text.Font lf,
            com.lowagie.text.Font vf) {
        com.lowagie.text.pdf.PdfPCell lc = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(l, lf));
        lc.setBackgroundColor(new java.awt.Color(249, 249, 249));
        lc.setPadding(10);
        com.lowagie.text.pdf.PdfPCell vc = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(v, vf));
        vc.setPadding(10);
        t.addCell(lc);
        t.addCell(vc);
    }

    private String getInitials(String name) {
        if (name == null || name.isBlank())
            return "?";
        String[] p = name.trim().split("\\s+");
        if (p.length >= 2)
            return ("" + p[0].charAt(0) + p[1].charAt(0)).toUpperCase();
        return ("" + p[0].charAt(0)).toUpperCase();
    }

    private String safe(String s) {
        return s != null ? s : "N/A";
    }

    @FXML
    private void goToContrats() throws IOException {
        App.setRoot("contrat");
    }

    private void showAlert(Alert.AlertType t, String title, String msg) {
        CustomAlert.show(t, title, msg);
    }
}