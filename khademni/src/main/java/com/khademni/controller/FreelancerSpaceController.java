package com.khademni.controller;

import com.khademni.App;
import com.khademni.model.OffreModel;
import com.khademni.utils.MyDataBase;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.util.Callback;

import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;

public class FreelancerSpaceController {

    @FXML
    private TableView<OffreModel> offreTable;
    @FXML
    private TableColumn<OffreModel, Integer> colId;
    @FXML
    private TableColumn<OffreModel, String> colTitre;
    @FXML
    private TableColumn<OffreModel, Double> colPrix;
    @FXML
    private TableColumn<OffreModel, String> colStatut;
    @FXML
    private TableColumn<OffreModel, LocalDateTime> colDate;
    @FXML
    private TableColumn<OffreModel, Void> colActions;
    @FXML
    private TextField searchField;

    private ObservableList<OffreModel> offreList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colPrix.setCellValueFactory(new PropertyValueFactory<>("prix"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateCreation"));

        addActionsButtonsToTable();
        loadOffres();
        setupSearch();
    }

    private void setupSearch() {
        FilteredList<OffreModel> filteredData = new FilteredList<>(offreList, p -> true);
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(offre -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase();
                return String.valueOf(offre.getId()).contains(lowerCaseFilter) ||
                        offre.getTitre().toLowerCase().contains(lowerCaseFilter);
            });
        });
        offreTable.setItems(filteredData);
    }

    private void loadOffres() {
        offreList.clear();
        String query = "SELECT * FROM offres";
        try (Connection conn = MyDataBase.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                offreList.add(new OffreModel(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getString("titre"),
                        rs.getString("description"),
                        rs.getDouble("prix"),
                        rs.getTimestamp("date_creation").toLocalDateTime(),
                        rs.getString("statut"),
                        "OFFRE"));
            }
            offreTable.setItems(offreList);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void addActionsButtonsToTable() {
        Callback<TableColumn<OffreModel, Void>, TableCell<OffreModel, Void>> cellFactory = param -> new TableCell<>() {
            private final Button editBtn = new Button("Modifier");
            private final Button deleteBtn = new Button("Supprimer");
            private final Button pdfBtn = new Button("PDF");
            private final HBox pane = new HBox(5, editBtn, deleteBtn, pdfBtn);

            {
                editBtn.setStyle("-fx-background-color: #6c0df2; -fx-text-fill: white; -fx-cursor: hand;");
                deleteBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-cursor: hand;");
                pdfBtn.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-cursor: hand;");

                editBtn.setOnAction(event -> {
                    OffreModel offre = getTableView().getItems().get(getIndex());
                    handleEdit(offre);
                });
                deleteBtn.setOnAction(event -> {
                    OffreModel offre = getTableView().getItems().get(getIndex());
                    handleDelete(offre);
                });
                pdfBtn.setOnAction(event -> {
                    OffreModel offre = getTableView().getItems().get(getIndex());
                    generatePDF(offre);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        };
        colActions.setCellFactory(cellFactory);
    }

    private void handleEdit(OffreModel offre) {
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
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer cet offre ?", ButtonType.YES, ButtonType.NO);
        if (alert.showAndWait().get() == ButtonType.YES) {
            String query = "DELETE FROM offres WHERE id = ?";
            try (Connection conn = MyDataBase.getConnection();
                    PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, offre.getId());
                pstmt.executeUpdate();
                MyDataBase.resetAutoIncrementIfEmpty("offres", 0);
                loadOffres();
            } catch (SQLException e) {
                e.printStackTrace();
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
            document.add(new Paragraph("Freelancer ID: " + offre.getUserId()));
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
}
