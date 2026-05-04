package com.khademni.controller;

import com.khademni.App;
import com.khademni.model.ContratModel;
import com.khademni.model.UserModel;
import com.khademni.service.ExchangeRateService;
import com.khademni.utils.MyDataBase;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.animation.FadeTransition;
import javafx.util.Duration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class PaiementController {

    @FXML private VBox paiementListContainer;
    @FXML private TextField searchField;
    @FXML private Label paymentCountLabel;
    @FXML private Label totalPaidLabel;
    @FXML private Label countLabel;

    private ObservableList<ContratModel> contratList = FXCollections.observableArrayList();
    private ExchangeRateService exchangeRateService = ExchangeRateService.getInstance();

    @FXML
    public void initialize() {
        setupSearch();
        loadContrats();
    }

    private void setupSearch() {
        searchField.textProperty().addListener((obs, o, n) -> {
            String f = n == null ? "" : n.toLowerCase().trim();
            List<ContratModel> filtered = contratList.stream()
                    .filter(c -> (c.getTitre() != null && c.getTitre().toLowerCase().contains(f))
                            || (c.getDescription() != null && c.getDescription().toLowerCase().contains(f))
                            || (c.getClientName() != null && c.getClientName().toLowerCase().contains(f)))
                    .collect(Collectors.toList());
            renderList(filtered);
        });
    }

    private void renderList(List<ContratModel> contrats) {
        paiementListContainer.getChildren().clear();
        countLabel.setText(contrats.size() + " paiements trouves");
        int delay = 0;
        for (ContratModel item : contrats) {
            VBox card = createListCard(item);
            card.setOpacity(0);
            FadeTransition ft = new FadeTransition(Duration.millis(400), card);
            ft.setFromValue(0); ft.setToValue(1);
            ft.setDelay(Duration.millis(delay));
            ft.play();
            paiementListContainer.getChildren().add(card);
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

        // Avatar
        String initials = getInitials(item.getClientName());
        Label avatar = new Label(initials);
        avatar.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: 900; " +
                "-fx-font-size: 14; -fx-padding: 12 14; -fx-background-radius: 100; -fx-min-width: 44; -fx-min-height: 44; -fx-alignment: center;");

        // Title area
        VBox titleArea = new VBox(4);
        HBox.setHgrow(titleArea, Priority.ALWAYS);

        Label badge = new Label("Paye");
        badge.getStyleClass().add("badge-success");

        Label title = new Label(item.getTitre() != null ? item.getTitre() : "Sans titre");
        title.setStyle("-fx-font-size: 16; -fx-font-weight: 800; -fx-text-fill: #1f2937;");
        title.setWrapText(true);

        Label clientInfo = new Label("Client: " + safe(item.getClientName()) + " - " + safe(item.getFreelancerName()));
        clientInfo.setStyle("-fx-font-size: 12; -fx-text-fill: #6b7280; -fx-font-weight: 600;");

        titleArea.getChildren().addAll(badge, title, clientInfo);

        // Price
        VBox priceArea = new VBox(4);
        priceArea.setAlignment(Pos.CENTER_RIGHT);
        Label price = new Label(String.format("%,.0f TND", item.getPrix()));
        price.setStyle("-fx-font-size: 18; -fx-font-weight: 900; -fx-text-fill: #10b981;");

        Label expandLabel = new Label("v Details");
        expandLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #9ca3af; -fx-font-weight: 600;");
        priceArea.getChildren().addAll(price, expandLabel);

        header.getChildren().addAll(avatar, titleArea, priceArea);

        // === ACTION PANEL (hidden) ===
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

        VBox dateBox = new VBox(6);
        Label dateLabel = new Label("DATE");
        dateLabel.getStyleClass().add("info-label");
        Label dateValue = new Label(item.getDateContrat() != null ? item.getDateContrat().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "N/A");
        dateValue.setStyle("-fx-font-size: 13; -fx-text-fill: #374151;");
        dateBox.getChildren().addAll(dateLabel, dateValue);

        infoRow.getChildren().addAll(descBox, phoneBox, dateBox);

        // Action row - only PDF button
        HBox actionRow = new HBox(10);
        actionRow.setAlignment(Pos.CENTER_LEFT);
        actionRow.setPadding(new Insets(8, 0, 0, 0));

        // Currency info
        HBox converter = new HBox(8);
        converter.setAlignment(Pos.CENTER_LEFT);
        converter.setStyle("-fx-background-color: white; -fx-padding: 6 14; -fx-background-radius: 10; -fx-border-color: #e5e7eb; -fx-border-radius: 10;");
        double eurVal = exchangeRateService.convertPrice(item.getPrix(), "EUR");
        Label convLabel = new Label(String.format("%,.0f TND = %.2f EUR", item.getPrix(), eurVal > 0 ? eurVal : item.getPrix() * 0.30));
        convLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #6b7280;");
        converter.getChildren().add(convLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button pdfBtn = new Button("\uD83D\uDCC4 Telecharger PDF");
        pdfBtn.setStyle("-fx-background-color: #7c3aed; -fx-text-fill: white; -fx-font-weight: 700; " +
                "-fx-font-size: 13; -fx-padding: 10 24; -fx-background-radius: 10; -fx-cursor: hand;");
        pdfBtn.setOnAction(e -> generatePDF(item));

        actionRow.getChildren().addAll(converter, spacer, pdfBtn);
        actionPanel.getChildren().addAll(infoRow, actionRow);
        card.getChildren().addAll(header, actionPanel);

        // Toggle
        header.setOnMouseClicked(e -> {
            boolean show = !actionPanel.isVisible();
            for (var node : paiementListContainer.getChildren()) {
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
                expandLabel.setText("^ Reduire");
            } else {
                expandLabel.setText("v Details");
            }
        });

        return card;
    }

    private void loadContrats() {
        contratList.clear();
        UserModel cu = App.getCurrentUser();
        if (cu == null) return;
        String q = "SELECT c.*, u1.first_name as cfname, u1.last_name as clname, u2.first_name as ffname, u2.last_name as flname FROM contrats c LEFT JOIN users u1 ON c.client_id = u1.id LEFT JOIN users u2 ON c.freelancer_id = u2.id WHERE (c.client_id = ? OR c.freelancer_id = ?) AND c.statut = 'PAYE'";
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
            renderList(contratList);
            updateStats();
        } catch (Exception e) { showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage()); }
    }

    private void updateStats() {
        paymentCountLabel.setText(String.valueOf(contratList.size()));
        double total = contratList.stream().mapToDouble(ContratModel::getPrix).sum();
        totalPaidLabel.setText(String.format("%,.0f TND", total));
    }

    private void generatePDF(ContratModel c) {
        String home = System.getProperty("user.home");
        File dl = new File(home, "Downloads");
        if (!dl.exists()) dl = new File(home, "Documents");
        try {
            String fn = "Paiement_Contrat_" + c.getIdContrat() + ".pdf";
            File out = new File(dl, fn);
            com.lowagie.text.Document doc = new com.lowagie.text.Document(com.lowagie.text.PageSize.A4, 50, 50, 50, 50);
            com.lowagie.text.pdf.PdfWriter.getInstance(doc, new FileOutputStream(out));
            doc.open();
            java.awt.Color green = new java.awt.Color(16, 185, 129);
            java.awt.Color dark = new java.awt.Color(26, 26, 26);
            java.awt.Color gray = new java.awt.Color(107, 114, 128);
            com.lowagie.text.Font tF = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 22, com.lowagie.text.Font.BOLD, java.awt.Color.WHITE);
            com.lowagie.text.Font sF = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 11, com.lowagie.text.Font.NORMAL, java.awt.Color.WHITE);
            com.lowagie.text.Font hF = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 16, com.lowagie.text.Font.BOLD, dark);
            com.lowagie.text.Font lF = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 10, com.lowagie.text.Font.BOLD, gray);
            com.lowagie.text.Font vF = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 12, com.lowagie.text.Font.NORMAL, dark);
            com.lowagie.text.Font pF = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 20, com.lowagie.text.Font.BOLD, green);
            com.lowagie.text.Font smF = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9, com.lowagie.text.Font.NORMAL, gray);

            com.lowagie.text.pdf.PdfPTable ht = new com.lowagie.text.pdf.PdfPTable(1);
            ht.setWidthPercentage(100);
            com.lowagie.text.pdf.PdfPCell hc = new com.lowagie.text.pdf.PdfPCell();
            hc.setBackgroundColor(new java.awt.Color(17, 24, 39)); hc.setPadding(20); hc.setBorder(0);
            hc.addElement(new com.lowagie.text.Paragraph("RECU DE PAIEMENT", tF));
            hc.addElement(new com.lowagie.text.Paragraph("Reference: PAY-" + String.format("%04d", c.getIdContrat()), sF));
            ht.addCell(hc); doc.add(ht); doc.add(new com.lowagie.text.Paragraph(" "));

            com.lowagie.text.pdf.PdfPTable it = new com.lowagie.text.pdf.PdfPTable(2);
            it.setWidthPercentage(100); it.setWidths(new float[]{30, 70});
            addRow(it, "Contrat", safe(c.getTitre()), lF, vF);
            addRow(it, "Date", c.getDateContrat() != null ? c.getDateContrat().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "N/A", lF, vF);
            addRow(it, "Client", safe(c.getClientName()), lF, vF);
            addRow(it, "Freelancer", safe(c.getFreelancerName()), lF, vF);
            addRow(it, "Statut", "PAYE", lF, vF);
            doc.add(it); doc.add(new com.lowagie.text.Paragraph(" "));
            doc.add(new com.lowagie.text.Paragraph("Montant: " + String.format("%.2f", c.getPrix()) + " TND", pF));
            double eurP = exchangeRateService.convertPrice(c.getPrix(), "EUR");
            if (eurP > 0) doc.add(new com.lowagie.text.Paragraph("~ " + String.format("%.2f", eurP) + " EUR", smF));
            doc.add(new com.lowagie.text.Paragraph(" "));
            com.lowagie.text.Paragraph ft = new com.lowagie.text.Paragraph("5ademni.tn - Genere le " + java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), smF);
            ft.setAlignment(1); doc.add(ft);
            doc.close();
            showAlert(Alert.AlertType.INFORMATION, "Succes", "PDF genere: " + out.getName());
            try { java.awt.Desktop.getDesktop().open(out); } catch (Exception ex) {}
        } catch (Exception e) { showAlert(Alert.AlertType.ERROR, "Erreur PDF", e.getMessage()); }
    }

    private void addRow(com.lowagie.text.pdf.PdfPTable t, String l, String v, com.lowagie.text.Font lf, com.lowagie.text.Font vf) {
        com.lowagie.text.pdf.PdfPCell lc = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(l, lf));
        lc.setBackgroundColor(new java.awt.Color(249, 249, 249)); lc.setPadding(10);
        com.lowagie.text.pdf.PdfPCell vc = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(v, vf));
        vc.setPadding(10); t.addCell(lc); t.addCell(vc);
    }

    private String getInitials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] p = name.trim().split("\\s+");
        if (p.length >= 2) return ("" + p[0].charAt(0) + p[1].charAt(0)).toUpperCase();
        return ("" + p[0].charAt(0)).toUpperCase();
    }

    private String safe(String s) { return s != null ? s : "N/A"; }

    @FXML private void goToContrats() throws IOException { App.setRoot("contrat"); }

    private void showAlert(Alert.AlertType t, String title, String msg) {
        Alert a = new Alert(t); a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.show();
    }
}