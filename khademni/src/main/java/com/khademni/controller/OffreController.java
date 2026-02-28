package com.khademni.controller;

import com.khademni.App;
import com.khademni.model.OffreModel;
import com.khademni.utils.MyDataBase;
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
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

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

    private final OffreService offreService = ServiceFactory.getOffreService();

    private ObservableList<OffreModel> offreList = FXCollections.observableArrayList();

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
        FilteredList<OffreModel> filteredData = new FilteredList<>(offreList, p -> true);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(offre -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }

                String lowerCaseFilter = newValue.toLowerCase();

                if (String.valueOf(offre.getId()).contains(lowerCaseFilter)) {
                    return true;
                } else if (offre.getTitre().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (offre.getType().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (offre.getUserName() != null && offre.getUserName().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }
                return false;
            });
        });

        offreTable.setItems(filteredData);
    }

    private void loadOffres() {
        offreList.clear();
        String query = "SELECT t.id, t.user_id, u.unique_id, CONCAT(u.first_name, ' ', u.last_name) AS user_name, t.titre, t.description, t.prix, t.date_creation, t.date_limite, t.statut, 'OFFRE' as type "
                +
                "FROM offres t LEFT JOIN users u ON t.user_id = u.id " +
                "UNION ALL " +
                "SELECT t.id, t.user_id, u.unique_id, CONCAT(u.first_name, ' ', u.last_name) AS user_name, t.titre, t.description, t.prix, t.date_creation, t.date_limite, t.statut, 'DEMANDE' as type "
                +
                "FROM demandes t LEFT JOIN users u ON t.user_id = u.id " +
                "ORDER BY date_creation DESC";
        try (Connection conn = MyDataBase.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                offreList.add(new OffreModel(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getInt("unique_id"),
                        rs.getString("titre"),
                        rs.getString("description"),
                        rs.getDouble("prix"),
                        rs.getTimestamp("date_creation").toLocalDateTime(),
                        rs.getString("statut"),
                        rs.getString("type"),
                        rs.getTimestamp("date_limite") != null ? rs.getTimestamp("date_limite").toLocalDateTime()
                                : null,
                        rs.getString("user_name")));
            }
            offreTable.setItems(offreList);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les offres : " + e.getMessage());
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
                MyDataBase.resetAutoIncrementIfEmpty(offre.getType().equalsIgnoreCase("OFFRE") ? "offres" : "demandes",
                        0);
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
