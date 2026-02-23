package com.khademni.controller;

import com.khademni.App;
import com.khademni.model.OffreModel;
import com.khademni.utils.MyDataBase;
import com.khademni.utils.PasswordUtil;
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
import java.time.format.DateTimeFormatter;

public class ClientSpaceController {

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
    private TableColumn<OffreModel, Void> colActions;
    @FXML
    private TextField searchField;

    private ObservableList<OffreModel> offreList = FXCollections.observableArrayList();

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

        addActionsButtonsToTable();
        setupSearch();
        loadOffres();
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
        // Try with unique_id JOIN first, fall back to simple query
        String queryJoin = "SELECT d.*, u.unique_id FROM demandes d LEFT JOIN users u ON d.user_id = u.id";
        String querySimple = "SELECT * FROM demandes";
        boolean useJoin = true;

        try (Connection conn = MyDataBase.getConnection();
                Statement stmt = conn.createStatement()) {

            ResultSet rs;
            try {
                rs = stmt.executeQuery(queryJoin);
            } catch (SQLException joinErr) {
                // unique_id column missing — fall back
                useJoin = false;
                rs = stmt.executeQuery(querySimple);
            }

            while (rs.next()) {
                OffreModel offre = new OffreModel();
                offre.setId(rs.getInt("id"));
                offre.setUserId(rs.getInt("user_id"));
                if (useJoin) {
                    try {
                        offre.setUserUniqueId(rs.getInt("unique_id"));
                    } catch (SQLException ignored) {
                    }
                } else {
                    offre.setUserUniqueId(rs.getInt("user_id"));
                }
                offre.setTitre(rs.getString("titre"));
                offre.setDescription(rs.getString("description"));
                offre.setPrix(rs.getDouble("prix"));
                offre.setDateCreation(rs.getTimestamp("date_creation").toLocalDateTime());
                offre.setStatut(rs.getString("statut"));
                offre.setType("DEMANDE");
                try {
                    Timestamp dl = rs.getTimestamp("date_limite");
                    if (dl != null)
                        offre.setDateLimite(dl.toLocalDateTime());
                } catch (SQLException ignored) {
                    /* column may not exist */ }
                offreList.add(offre);
            }
            rs.close();
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
        if (!confirmPassword())
            return;
        OffreFormController.editMode = true;
        OffreFormController.selectedOffre = offre;
        OffreFormController.creationType = "DEMANDE";
        try {
            App.setRoot("offre_form");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleDelete(OffreModel offre) {
        if (!confirmPassword())
            return;
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer cette demande ?", ButtonType.YES,
                ButtonType.NO);
        if (alert.showAndWait().get() == ButtonType.YES) {
            String query = "DELETE FROM demandes WHERE id = ?";
            try (Connection conn = MyDataBase.getConnection();
                    PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, offre.getId());
                pstmt.executeUpdate();
                MyDataBase.resetAutoIncrementIfEmpty("demandes", 1);
                loadOffres();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void generatePDF(OffreModel offre) {
        String dest = System.getProperty("user.home") + "/Downloads/Demande_" + offre.getId() + ".pdf";
        try (FileOutputStream fos = new FileOutputStream(dest)) {
            Document document = new Document();
            PdfWriter.getInstance(document, fos);
            document.open();
            document.add(new Paragraph("5ademni.tn - Demande de Travail"));
            document.add(new Paragraph("--------------------------------------------------"));
            document.add(new Paragraph("ID Demande: " + offre.getId()));
            document.add(new Paragraph("Client ID: " + offre.getUserUniqueId()));
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
    private void goToAddDemand() throws IOException {
        OffreFormController.editMode = false;
        OffreFormController.creationType = "DEMANDE";
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
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Confirmation");
        dialog.setHeaderText("Entrez votre mot de passe pour continuer");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        javafx.scene.control.PasswordField pwField = new javafx.scene.control.PasswordField();
        pwField.setPromptText("Mot de passe");
        dialog.getDialogPane().setContent(pwField);
        dialog.setResultConverter(btn -> btn == ButtonType.OK ? pwField.getText() : null);
        java.util.Optional<String> result = dialog.showAndWait();
        if (result.isEmpty() || result.get().isEmpty())
            return false;
        String enteredPassword = result.get();
        String storedHash = App.getCurrentUser().getPassword();
        if (!PasswordUtil.checkPassword(enteredPassword, storedHash)) {
            new Alert(Alert.AlertType.ERROR, "Mot de passe incorrect.").showAndWait();
            return false;
        }
        return true;
    }
}
