package com.khademni.controller;

import com.khademni.App;
import com.khademni.model.ContratModel;
import com.khademni.utils.MyDataBase;
import com.lowagie.text.Document;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;

public class ContratController {

    @FXML
    private TableView<ContratModel> contratTable;
    @FXML
    private TableColumn<ContratModel, Integer> colId;
    @FXML
    private TableColumn<ContratModel, String> colTitre;
    @FXML
    private TableColumn<ContratModel, Integer> colFreelancer;
    @FXML
    private TableColumn<ContratModel, Integer> colClient;
    @FXML
    private TableColumn<ContratModel, Double> colPrix;
    @FXML
    private TableColumn<ContratModel, String> colStatut;
    @FXML
    private TableColumn<ContratModel, LocalDate> colDate;
    @FXML
    private TableColumn<ContratModel, String> colDesc;
    @FXML
    private TableColumn<ContratModel, Void> colActions;

    @FXML
    private TextField searchField;
    @FXML
    private TabPane mainTabPane;
    @FXML
    private Tab listTab;
    @FXML
    private Tab addTab;
    @FXML
    private Tab infoTab;

    // Form fields
    @FXML
    private TextField titreField;
    @FXML
    private TextField idFreelancerField;
    @FXML
    private TextField idClientField;
    @FXML
    private TextField prixField;
    @FXML
    private TextArea descriptionField;
    @FXML
    private DatePicker dateContratPicker;

    // Info fields
    @FXML
    private Label infoIdContrat;
    @FXML
    private Label infoIdClient;
    @FXML
    private Label infoIdFreelancer;
    @FXML
    private Label infoFreelancerName;
    @FXML
    private Label infoTitre;
    @FXML
    private Label infoPrix;
    @FXML
    private Label infoDate;
    @FXML
    private Label infoStatut;
    @FXML
    private Label infoDescription;
    @FXML
    private Label selectionPrompt;
    @FXML
    private Label errorLabel;

    private ContratModel contratToEdit;

    private ObservableList<ContratModel> contratList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("idContrat"));
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colFreelancer.setCellValueFactory(new PropertyValueFactory<>("idFreelancer"));
        colClient.setCellValueFactory(new PropertyValueFactory<>("idClient"));
        colPrix.setCellValueFactory(new PropertyValueFactory<>("prix"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateContrat"));
        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));

        addActionsButtonsToTable();
        setupSearch(); // Call this first to set items to the filtered list
        loadContrats(); // Then load data into the underlying list

        // Selection listener for Informations tab
        contratTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                showContratDetails(newSelection);
            }
        });
    }

    private void showContratDetails(ContratModel contrat) {
        infoIdContrat.setText(String.valueOf(contrat.getIdContrat()));
        infoIdClient.setText(String.valueOf(contrat.getIdClient()));
        infoIdFreelancer.setText(String.valueOf(contrat.getIdFreelancer()));
        infoFreelancerName.setText(contrat.getFreelancerName());
        infoTitre.setText(contrat.getTitre());
        infoPrix.setText(String.format("%.2f DT", contrat.getPrix()));
        infoDate.setText(contrat.getDateContrat().toString());
        infoStatut.setText(contrat.getStatut());
        infoDescription.setText(contrat.getDescription());

        if (selectionPrompt != null)
            selectionPrompt.setVisible(false);
    }

    private void setupSearch() {
        FilteredList<ContratModel> filteredData = new FilteredList<>(contratList, p -> true);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(contrat -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase();
                return String.valueOf(contrat.getIdContrat()).contains(lowerCaseFilter) ||
                        contrat.getTitre().toLowerCase().contains(lowerCaseFilter) ||
                        contrat.getDescription().toLowerCase().contains(lowerCaseFilter);
            });
        });

        contratTable.setItems(filteredData);
    }

    private void loadContrats() {
        System.out.println("DEBUG: loadContrats() started.");
        contratList.clear();
        String query = "SELECT c.*, u.first_name, u.last_name FROM contrats c LEFT JOIN users u ON c.freelancer_id = u.id";

        System.out.println("DEBUG: Executing Query: " + query);

        try (Connection conn = MyDataBase.getConnection()) {
            if (conn == null || conn.isClosed()) {
                System.out.println("DEBUG ERROR: Connection is null or closed!");
                return;
            }
            System.out.println("DEBUG: Database connection is open.");

            try (Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(query)) {

                while (rs.next()) {
                    int id = rs.getInt("id");
                    String titre = rs.getString("titre");
                    String firstName = rs.getString("first_name");
                    String lastName = rs.getString("last_name");
                    String freelancerName = (firstName != null && lastName != null) ? (firstName + " " + lastName)
                            : "Unknown Freelancer";

                    System.out.println("DEBUG: Row found -> ID: " + id + ", Titre: " + titre);

                    java.sql.Date sqlDate = rs.getDate("date_contrat");
                    LocalDate date = (sqlDate != null) ? sqlDate.toLocalDate() : LocalDate.now();

                    ContratModel contrat = new ContratModel(
                            id,
                            rs.getInt("client_id"),
                            rs.getInt("freelancer_id"),
                            freelancerName,
                            titre,
                            rs.getString("description"),
                            rs.getDouble("prix"),
                            date,
                            rs.getString("statut"));
                    contratList.add(contrat);
                }
                System.out.println("DEBUG: Total Contrats added to list: " + contratList.size());
            }

            // Re-bind or refresh if needed
            if (contratTable.getItems() == null || contratTable.getItems().isEmpty()) {
                System.out.println("DEBUG: Table items empty, triggering setupSearch again.");
                setupSearch();
            }

        } catch (Exception e) {
            System.out.println("DEBUG ERROR in loadContrats: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les contrats : " + e.getMessage());
        }
    }

    private void addActionsButtonsToTable() {
        Callback<TableColumn<ContratModel, Void>, TableCell<ContratModel, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<ContratModel, Void> call(final TableColumn<ContratModel, Void> param) {
                final TableCell<ContratModel, Void> cell = new TableCell<>() {
                    private final Button editBtn = new Button("Modifier");
                    private final Button deleteBtn = new Button("Supprimer");
                    private final HBox pane = new HBox(editBtn, deleteBtn);

                    {
                        pane.setSpacing(10);
                        editBtn.setStyle(
                                "-fx-background-color: #6c5ce7; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5;");
                        deleteBtn.setStyle(
                                "-fx-background-color: white; -fx-text-fill: #6c5ce7; -fx-border-color: #6c5ce7; -fx-border-radius: 5; -fx-background-radius: 5;");

                        editBtn.setOnAction(event -> {
                            ContratModel contrat = getTableView().getItems().get(getIndex());
                            handleEdit(contrat);
                        });

                        deleteBtn.setOnAction(event -> {
                            ContratModel contrat = getTableView().getItems().get(getIndex());
                            handleDelete(contrat);
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(pane);
                        }
                    }
                };
                return cell;
            }
        };
        colActions.setCellFactory(cellFactory);
    }

    private void handleEdit(ContratModel contrat) {
        this.contratToEdit = contrat;
        titreField.setText(contrat.getTitre());
        idFreelancerField.setText(String.valueOf(contrat.getIdFreelancer()));
        idClientField.setText(String.valueOf(contrat.getIdClient()));
        prixField.setText(String.valueOf(contrat.getPrix()));
        descriptionField.setText(contrat.getDescription());
        dateContratPicker.setValue(contrat.getDateContrat());

        // Switch to Add/Edit tab
        mainTabPane.getSelectionModel().select(1);
    }

    @FXML
    private void saveContrat() {
        String titre = titreField.getText().trim();
        String freelancerIdStr = idFreelancerField.getText().trim();
        String clientIdStr = idClientField.getText().trim();
        String prixStr = prixField.getText().trim();
        String description = descriptionField.getText().trim();
        LocalDate date = dateContratPicker.getValue();

        // Clear previous errors
        showError("");

        // Control de saisie (Validation)
        if (clientIdStr.isEmpty()) {
            showError("L'ID Client est requis.");
            return;
        }

        if (clientIdStr.length() < 3 || clientIdStr.length() > 7) {
            showError("L'ID Client doit contenir entre 3 et 7 caractères.");
            return;
        }

        int cId;
        try {
            cId = Integer.parseInt(clientIdStr);
        } catch (NumberFormatException e) {
            showError("L'ID Client doit être un nombre valide.");
            return;
        }

        if (freelancerIdStr.isEmpty()) {
            showError("L'ID Freelancer est requis.");
            return;
        }

        if (freelancerIdStr.length() < 3 || freelancerIdStr.length() > 7) {
            showError("L'ID Freelancer doit contenir entre 3 et 7 caractères.");
            return;
        }

        int fId;
        try {
            fId = Integer.parseInt(freelancerIdStr);
        } catch (NumberFormatException e) {
            showError("L'ID Freelancer doit être un nombre valide.");
            return;
        }

        if (date == null) {
            showError("La date du contrat est requise.");
            return;
        }

        if (titre.isEmpty() || titre.length() < 5) {
            showError("Le titre doit contenir au moins 5 caractères.");
            return;
        }

        if (description.isEmpty() || description.length() < 10) {
            showError("La description doit contenir au moins 10 caractères.");
            return;
        }

        double prix;
        try {
            prix = Double.parseDouble(prixStr);
            if (prix <= 0) {
                showError("Le prix doit être un nombre positif.");
                return;
            }
        } catch (NumberFormatException e) {
            showError("Le prix doit être un nombre valide.");
            return;
        }

        try {
            Connection conn = MyDataBase.getConnection();
            String query;

            if (contratToEdit == null) {
                query = "INSERT INTO contrats (client_id, freelancer_id, titre, description, prix, date_contrat, statut) VALUES (?, ?, ?, ?, ?, ?, ?)";
            } else {
                query = "UPDATE contrats SET client_id=?, freelancer_id=?, titre=?, description=?, prix=?, date_contrat=? WHERE id=?";
            }

            try (PreparedStatement stmt = (contratToEdit == null)
                    ? conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)
                    : conn.prepareStatement(query)) {

                stmt.setInt(1, cId);
                stmt.setInt(2, fId);
                stmt.setString(3, titre);
                stmt.setString(4, description);
                stmt.setDouble(5, prix);
                stmt.setDate(6, java.sql.Date.valueOf(date));

                if (contratToEdit == null) {
                    stmt.setString(7, "EN_ATTENTE");
                } else {
                    stmt.setInt(7, contratToEdit.getIdContrat());
                }

                stmt.executeUpdate();
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Contrat enregistré avec succès !");
                resetForm();
                loadContrats();
                mainTabPane.getSelectionModel().select(0); // Back to list
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur lors de l'enregistrement : " + e.getMessage());
        }
    }

    @FXML
    private void resetForm() {
        contratToEdit = null;
        titreField.clear();
        idFreelancerField.clear();
        idClientField.clear();
        prixField.clear();
        descriptionField.clear();
        dateContratPicker.setValue(null);
        showError("");
    }

    private void handleDelete(ContratModel contrat) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer Contrat");
        alert.setHeaderText(null);
        alert.setContentText("Voulez-vous vraiment supprimer ce contrat ?");

        if (alert.showAndWait().get() == ButtonType.OK) {
            String query = "DELETE FROM contrats WHERE id = ?";
            try (Connection conn = MyDataBase.getConnection();
                    PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setInt(1, contrat.getIdContrat());
                stmt.executeUpdate();
                MyDataBase.resetAutoIncrementIfEmpty("contrats", 0);
                loadContrats(); // Refresh list
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la suppression.");
            }
        }
    }

    @FXML
    private void generatePDF() {
        Document document = new Document();
        try {
            String userHome = System.getProperty("user.home");
            String path = userHome + "/Downloads/contrats_list_" + System.currentTimeMillis() + ".pdf";
            PdfWriter.getInstance(document, new FileOutputStream(path));
            document.open();

            document.add(new Paragraph("Liste des Contrats", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20)));
            document.add(new Paragraph("Generated on: " + LocalDate.now()));
            document.add(new Paragraph(" ")); // Spacer

            PdfPTable table = new PdfPTable(7);
            table.addCell("ID");
            table.addCell("Titre");
            table.addCell("Client");
            table.addCell("Prix");
            table.addCell("Date");
            table.addCell("Statut");

            for (ContratModel contrat : contratList) {
                table.addCell(String.valueOf(contrat.getIdContrat()));
                table.addCell(contrat.getTitre());
                table.addCell(String.valueOf(contrat.getIdClient()));
                table.addCell(String.valueOf(contrat.getPrix()));
                table.addCell(contrat.getDateContrat().toString());
                table.addCell(contrat.getStatut());
            }

            document.add(table);
            document.close();

            showAlert(Alert.AlertType.INFORMATION, "PDF Created", "PDF exported successfully to " + path);

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Could not generate PDF: " + e.getMessage());
        }
    }

    @FXML
    private void goToPaiement() throws IOException {
        App.setRoot("paiement");
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showError(String message) {
        if (errorLabel == null)
            return;
        if (message == null || message.isEmpty()) {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        } else {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }
}
