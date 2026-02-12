package com.khademni.controller;

import com.khademni.model.Contrat;   // 🔥 IMPORTANT
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
    private TableView<Contrat> tableContrat;

    @FXML
    private TableColumn<Contrat, Integer> colId;

    @FXML
    private TableColumn<Contrat, Integer> colClient;

    @FXML
    private TableColumn<Contrat, Integer> colFreelancer;

    @FXML
    private TableColumn<Contrat, LocalDate> colDate;

    @FXML
    private TableColumn<Contrat, String> colDescription;

    private ObservableList<Contrat> contratList = FXCollections.observableArrayList();
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

        Contrat contrat = new Contrat(
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
        Contrat selected = tableContrat.getSelectionModel().getSelectedItem();
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
