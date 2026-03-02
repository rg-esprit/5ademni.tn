package com.khademni.controller;

import com.khademni.App;
import com.khademni.model.OffreModel;

import java.time.LocalDateTime;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import javafx.scene.control.cell.PropertyValueFactory;
import com.khademni.service.OffreService;
import com.khademni.service.ServiceFactory;
import com.khademni.exception.BusinessException;
import com.khademni.utils.PasswordConfirmationUtil;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import java.io.FileOutputStream;

import java.io.IOException;

public class OffreController {

    @FXML
    private TableView<OffreModel> offreTable;
    @FXML
    private TableColumn<OffreModel, Integer> colId;
    @FXML
    private TableColumn<OffreModel, String> colPubliePar;
    @FXML
    private TableColumn<OffreModel, String> colTitre;
    @FXML
    private TableColumn<OffreModel, Double> colPrix;
    @FXML
    private TableColumn<OffreModel, String> colStatut;
    @FXML
    private TableColumn<OffreModel, String> colType;
    @FXML
    private TableColumn<OffreModel, LocalDateTime> colDate;
    // colActions removed
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
    private Button btnPrevPage;
    @FXML
    private Button btnNextPage;
    @FXML
    private Label lblPageInfo;

    private final OffreService offreService = ServiceFactory.getOffreService();

    private ObservableList<OffreModel> offreList = FXCollections.observableArrayList();

    // Pagination Variables
    private int currentPage = 1;
    private int rowsPerPage = 10;
    private int totalRecords = 0;
    private String currentSearchQuery = "";

    @FXML
    public void initialize() {
        // Show sequential row number (1, 2, 3...) instead of database ID
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
        colPubliePar.setCellValueFactory(new PropertyValueFactory<>("userName"));
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
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateCreation"));

        setupFloatingActionBar();
        loadOffres();
        setupSearch();
    }

    private void setupSearch() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            currentSearchQuery = newValue == null ? "" : newValue.trim();
            currentPage = 1; // Reset to first page on new search
            loadOffres();
        });
    }

    private void loadOffres() {
        offreList.clear();
        try {
            int offset = (currentPage - 1) * rowsPerPage;

            // 1. Get total count for the current search query
            totalRecords = offreService.countSearchResults(currentSearchQuery);

            // 2. Fetch the specific page
            offreList.addAll(offreService.searchPaginated(currentSearchQuery, offset, rowsPerPage));
            offreTable.setItems(offreList);

            updatePaginationUI();

        } catch (BusinessException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les offres : " + e.getMessage());
        }
    }

    private void updatePaginationUI() {
        int totalPages = (int) Math.ceil((double) totalRecords / rowsPerPage);
        if (totalPages == 0)
            totalPages = 1;

        lblPageInfo.setText("Page " + currentPage + " sur " + totalPages);
        btnPrevPage.setDisable(currentPage == 1);
        btnNextPage.setDisable(currentPage >= totalPages);
    }

    @FXML
    private void onPrevPage() {
        if (currentPage > 1) {
            currentPage--;
            loadOffres();
        }
    }

    @FXML
    private void onNextPage() {
        int totalPages = (int) Math.ceil((double) totalRecords / rowsPerPage);
        if (currentPage < totalPages) {
            currentPage++;
            loadOffres();
        }
    }

    @FXML
    private void goToAddOffer() throws IOException {
        // Form to create an OFFER (Freelancer)
        OffreFormController.creationType = "OFFRE";
        App.setRoot("offre_form");
    }

    @FXML
    private void goToAddDemand() throws IOException {
        // Form to create a DEMANDE (Client)
        OffreFormController.creationType = "DEMANDE";
        App.setRoot("offre_form");
    }

    private void setupFloatingActionBar() {
        offreTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                boolean isOwner = App.getCurrentUser() != null
                        && newSelection.getUserId() == App.getCurrentUser().getId();
                fabEditBtn.setVisible(isOwner);
                fabEditBtn.setManaged(isOwner);
                fabDeleteBtn.setVisible(isOwner);
                fabDeleteBtn.setManaged(isOwner);
                // Everyone can export to PDF
                fabPdfBtn.setVisible(true);
                fabPdfBtn.setManaged(true);

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
        if (!PasswordConfirmationUtil.showConfirmation()) {
            return;
        }
        OffreFormController.editMode = true;
        OffreFormController.selectedOffre = offre;
        OffreFormController.creationType = offre.getType();
        try {
            App.setRoot("offre_form");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleDelete(OffreModel offre) {
        if (!PasswordConfirmationUtil.showConfirmation()) {
            return;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer cet élément ?", ButtonType.YES, ButtonType.NO);
        if (alert.showAndWait().get() == ButtonType.YES) {
            try {
                offreService.deleteOffre(offre.getId(), offre.getType());
                loadOffres();
            } catch (BusinessException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
            }
        }
    }

    private void generatePDF(OffreModel offre) {
        String dest = System.getProperty("user.home") + "/Downloads/" + offre.getType() + "_" + offre.getId() + ".pdf";
        try (FileOutputStream fos = new FileOutputStream(dest)) {
            Document document = new Document();
            PdfWriter.getInstance(document, fos);
            document.open();
            document.add(new Paragraph("5ademni.tn - " + offre.getType()));
            document.add(new Paragraph("--------------------------------------------------"));
            document.add(new Paragraph("ID: " + offre.getId()));
            document.add(
                    new Paragraph("Publié par: " + (offre.getUserName() != null ? offre.getUserName() : "Inconnu")));
            document.add(new Paragraph("Titre: " + offre.getTitre()));
            document.add(new Paragraph("Prix: " + (offre.getPrix() == 0 ? "----" : offre.getPrix() + " DT")));
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

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
