package com.khademni.controller;

import com.khademni.model.ContratModel;   // 🔥 IMPORTANT
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDate;

public class ContratController {

    @FXML
    private TextField txtClientId;

    @FXML
    private TextField txtFreelancerId;

    @FXML
    private DatePicker dateContrat;

    @FXML
    private TextArea txtDescription;

    @FXML
    private TableView<ContratModel> tableContrat;

    @FXML
    private TableColumn<ContratModel, Integer> colId;

    @FXML
    private TableColumn<ContratModel, Integer> colClient;

    @FXML
    private TableColumn<ContratModel, Integer> colFreelancer;

    @FXML
    private TableColumn<ContratModel, LocalDate> colDate;

    @FXML
    private TableColumn<ContratModel, String> colDescription;

    private ObservableList<ContratModel> contratList = FXCollections.observableArrayList();
    private int idCounter = 1;

    @FXML
    public void initialize() {

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colClient.setCellValueFactory(new PropertyValueFactory<>("clientId"));
        colFreelancer.setCellValueFactory(new PropertyValueFactory<>("freelancerId"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateContrat"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));

        tableContrat.setItems(contratList);
    }

    @FXML
    private void ajouterContrat() {

        int clientId = Integer.parseInt(txtClientId.getText());
        int freelancerId = Integer.parseInt(txtFreelancerId.getText());
        LocalDate date = dateContrat.getValue();
        String description = txtDescription.getText();

        ContratModel contrat = new ContratModel(
                idCounter++,
                clientId,
                freelancerId,
                date,
                description
        );

        contratList.add(contrat);
        clearFields();
    }

    @FXML
    private void supprimerContrat() {
        ContratModel selected = tableContrat.getSelectionModel().getSelectedItem();
        if (selected != null) {
            contratList.remove(selected);
        }
    }

    private void clearFields() {
        txtClientId.clear();
        txtFreelancerId.clear();
        txtDescription.clear();
        dateContrat.setValue(null);
    }
}
