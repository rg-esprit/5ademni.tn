package com.khademni.controller;

import com.khademni.model.Client;
import com.khademni.model.Freelancer;
import com.khademni.model.ContratModel;
import com.khademni.service.ContratService;
import com.khademni.service.ServiceFactory;
import com.khademni.exception.ContractCreationException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.time.LocalDate;

public class AdminContratController {

    @FXML
    private ComboBox<Client> clientComboBox;
    @FXML
    private ComboBox<Freelancer> freelancerComboBox;
    @FXML
    private TextField titreField;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private TextField prixField;
    @FXML
    private DatePicker datePicker;

    private final ContratService contratService = ServiceFactory.getContratService();

    @FXML
    public void initialize() {
        loadComboBoxData();
    }

    private void loadComboBoxData() {
        try {
            ObservableList<Client> clients = FXCollections.observableArrayList(contratService.getAllClients());
            clientComboBox.setItems(clients);

            ObservableList<Freelancer> freelancers = FXCollections
                    .observableArrayList(contratService.getAllFreelancers());
            freelancerComboBox.setItems(freelancers);
        } catch (ContractCreationException e) {
            showAlert(Alert.AlertType.ERROR, "Error", e.getMessage());
        }
    }

    @FXML
    private void handleSaveContract() {
        try {
            Client selectedClient = clientComboBox.getValue();
            Freelancer selectedFreelancer = freelancerComboBox.getValue();

            if (selectedClient == null || selectedFreelancer == null) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Please select both a client and a freelancer.");
                return;
            }

            ContratModel contrat = new ContratModel();
            contrat.setIdClient(selectedClient.getId());
            contrat.setIdFreelancer(selectedFreelancer.getId());
            contrat.setTitre(titreField.getText().trim());
            contrat.setDescription(descriptionArea.getText().trim());

            try {
                contrat.setPrix(Double.parseDouble(prixField.getText().trim()));
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Invalid price format.");
                return;
            }

            contrat.setDateContrat(datePicker.getValue() != null ? datePicker.getValue() : LocalDate.now());
            contrat.setStatut("EN_ATTENTE");

            contratService.createContract(contrat);
            showAlert(Alert.AlertType.INFORMATION, "Success", "Contract created successfully!");
            clearForm();
        } catch (ContractCreationException e) {
            showAlert(Alert.AlertType.ERROR, "System Error", e.getMessage());
        }
    }

    private void clearForm() {
        clientComboBox.getSelectionModel().clearSelection();
        freelancerComboBox.getSelectionModel().clearSelection();
        titreField.clear();
        descriptionArea.clear();
        prixField.clear();
        datePicker.setValue(null);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
