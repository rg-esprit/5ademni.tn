package com.khademni.controller;

import com.khademni.App;
import com.khademni.model.ContratModel;
import com.khademni.utils.MyDataBase;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.util.Callback;

import java.io.IOException;
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
    private TableColumn<ContratModel, Integer> colFreelancer;
    @FXML
    private TableColumn<ContratModel, Integer> colClient;
    @FXML
    private TableColumn<ContratModel, String> colStatut;
    @FXML
    private TableColumn<ContratModel, Void> colActions;

    private ObservableList<ContratModel> contratList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("idContrat"));
        colFreelancer.setCellValueFactory(new PropertyValueFactory<>("idFreelancer"));
        colClient.setCellValueFactory(new PropertyValueFactory<>("idClient"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

        addActionsButtonsToTable();
        loadContrats();
    }

    private void loadContrats() {
        contratList.clear();
        String query = "SELECT * FROM contrats";
        try (Connection conn = MyDataBase.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                contratList.add(new ContratModel(
                        rs.getInt("id"),
                        rs.getInt("client_id"),
                        rs.getInt("freelancer_id"),
                        "", // freelancerName not needed here but required by constructor
                        rs.getString("titre"),
                        rs.getString("description"),
                        rs.getDouble("prix"),
                        rs.getDate("date_contrat").toLocalDate(),
                        rs.getString("statut")));
            }
            contratStatusTable.setItems(contratList);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les contrats : " + e.getMessage());
        }
    }

    private void addActionsButtonsToTable() {
        Callback<TableColumn<ContratModel, Void>, TableCell<ContratModel, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<ContratModel, Void> call(final TableColumn<ContratModel, Void> param) {
                final TableCell<ContratModel, Void> cell = new TableCell<>() {
                    private final Button payeBtn = new Button("Payé");
                    private final Button attenteBtn = new Button("En Attente");
                    private final Button annulerBtn = new Button("Annuler");
                    private final HBox pane = new HBox(payeBtn, attenteBtn, annulerBtn);

                    {
                        pane.setSpacing(5);
                        payeBtn.setStyle(
                                "-fx-background-color: #6c5ce7; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 5;");
                        attenteBtn.setStyle(
                                "-fx-background-color: #a29bfe; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 5;");
                        annulerBtn.setStyle(
                                "-fx-background-color: white; -fx-text-fill: #6c5ce7; -fx-border-color: #6c5ce7; -fx-border-radius: 5; -fx-background-radius: 5; -fx-font-size: 11px;");

                        payeBtn.setOnAction(event -> updateStatus(getTableView().getItems().get(getIndex()), "PAYE"));
                        attenteBtn.setOnAction(
                                event -> updateStatus(getTableView().getItems().get(getIndex()), "EN_ATTENTE"));
                        annulerBtn.setOnAction(
                                event -> updateStatus(getTableView().getItems().get(getIndex()), "ANNULE"));
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

    private void updateStatus(ContratModel contrat, String newStatus) {
        String query = "UPDATE contrats SET statut = ? WHERE id = ?";
        try (Connection conn = MyDataBase.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, newStatus);
            stmt.setInt(2, contrat.getIdContrat());
            stmt.executeUpdate();

            showAlert(Alert.AlertType.INFORMATION, "Succès", "Statut mis à jour !");
            loadContrats(); // Refresh table
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de mettre à jour le statut.");
        }
    }

    @FXML
    private void goToContrats() throws IOException {
        App.setRoot("contrat");
    }

    @FXML
    private void handlePay() {
        try {
            String flouciLoginUrl = "https://app.flouci.com/login";

            if (App.getAppHostServices() != null) {
                App.getAppHostServices().showDocument(flouciLoginUrl);

                showAlert(Alert.AlertType.INFORMATION, "Connexion Flouci",
                        "Redirection vers la page de connexion Flouci...");
            } else {
                System.err.println("HostServices is not available. URL: " + flouciLoginUrl);
                showAlert(Alert.AlertType.ERROR, "Erreur de Navigation",
                        "Impossible d'ouvrir le navigateur. Le lien est : " + flouciLoginUrl);
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Une erreur est survenue lors de la redirection : " + e.getMessage());
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
