package com.khademni.controller;

import com.khademni.App;
import com.khademni.model.ContratModel;
import com.khademni.model.UserModel;
import com.khademni.utils.ValidationUtils;
import com.khademni.utils.SpellCheckDecorator;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import com.khademni.service.ContratService;
import com.khademni.service.ServiceFactory;
import com.khademni.service.UserIdService;
import com.khademni.service.UserService;
import com.khademni.model.Client;
import com.khademni.model.Freelancer;
import com.khademni.exception.BusinessException;
import com.khademni.exception.ContractCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;

public class ContratFormController {
    private static final Logger logger = LoggerFactory.getLogger(ContratFormController.class);
    private final ContratService contratService = ServiceFactory.getContratService();
    private final UserService userService = ServiceFactory.getUserService();

    @FXML
    private Label formTitle;

    @FXML
    private TextField titreField;

    @FXML
    private ComboBox<Freelancer> idFreelancerComboBox;
    @FXML
    private ComboBox<Client> idClientComboBox;

    @FXML
    private TextField prixField;

    @FXML
    private TextArea descriptionField;

    @FXML
    private DatePicker dateContratPicker;

    @FXML
    private Label errorLabel;

    @FXML
    public void initialize() {
        UserModel currentUser = App.getCurrentUser();
        if (currentUser != null && !UserIdService.hasValidUniqueId(currentUser)) {
            promptAndSetUniqueIdOnce(currentUser);
        }
        try {
            idClientComboBox
                    .setItems(javafx.collections.FXCollections.observableArrayList(contratService.getAllClients()));
            idFreelancerComboBox
                    .setItems(javafx.collections.FXCollections.observableArrayList(contratService.getAllFreelancers()));
        } catch (ContractCreationException e) {
            logger.error("Error loading dropdown data", e);
        }

        // Attach live LanguageTool spell-check
        SpellCheckDecorator.attach(titreField, "fr");
        SpellCheckDecorator.attach(descriptionField, "fr");
    }

    private void promptAndSetUniqueIdOnce(UserModel currentUser) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("ID unique obligatoire");
        dialog.setHeaderText("Choisissez votre ID unique (4 chiffres)");
        dialog.setContentText("Cet ID ne pourra plus être modifié après enregistrement :");
        dialog.getEditor().setPromptText("Ex: 5680");
        String result = dialog.showAndWait().orElse(null);
        if (result == null || result.isBlank())
            return;
        int uniqueId;
        try {
            uniqueId = Integer.parseInt(result.trim());
        } catch (NumberFormatException e) {
            showError("L'ID doit être composé uniquement de 4 chiffres (1000–9999).");
            return;
        }
        try {
            userService.setUniqueIdOnceIfMissing(currentUser.getId(), uniqueId);
            Optional<UserModel> updated = userService.getUserById(currentUser.getId());
            updated.ifPresent(App::setCurrentUser);
        } catch (BusinessException e) {
            showError(e.getMessage());
        }
    }

    private ContratModel contratToEdit;

    public void setContratToEdit(ContratModel contrat) {
        this.contratToEdit = contrat;
        if (contrat != null) {
            if (formTitle != null)
                formTitle.setText("Modifier le Contrat");
            titreField.setText(contrat.getTitre());

            idClientComboBox.getItems().stream()
                    .filter(c -> c.getId() == contrat.getIdClient())
                    .findFirst()
                    .ifPresent(idClientComboBox::setValue);

            idFreelancerComboBox.getItems().stream()
                    .filter(f -> f.getId() == contrat.getIdFreelancer())
                    .findFirst()
                    .ifPresent(idFreelancerComboBox::setValue);

            prixField.setText(String.valueOf(contrat.getPrix()));
            descriptionField.setText(contrat.getDescription());
            dateContratPicker.setValue(contrat.getDateContrat());
        }
    }

    @FXML
    private void saveContrat(ActionEvent event) {
        // Auto-correct text fields before validation
        String titre = ValidationUtils.autoCorrect(titreField.getText());
        String description = ValidationUtils.autoCorrect(descriptionField.getText());
        // Push corrected values back so the user sees them
        titreField.setText(titre);
        descriptionField.setText(description);

        Client selectedClient = idClientComboBox.getValue();
        Freelancer selectedFreelancer = idFreelancerComboBox.getValue();
        String prixStr = prixField.getText().trim();
        LocalDate date = dateContratPicker.getValue();

        // Clear previous errors
        showError("");

        if (selectedClient == null || selectedFreelancer == null) {
            showError("Veuillez sélectionner un client et un freelancer.");
            return;
        }

        String titreError = ValidationUtils.validateField(titre, "Le titre", 5);
        if (titreError != null) {
            showError(titreError);
            return;
        }

        double prix;
        try {
            prix = Double.parseDouble(prixStr);
            if (prix <= 0)
                throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showError("Le prix doit être un nombre positif.");
            return;
        }

        String descError = ValidationUtils.validateField(description, "La description", 10);
        if (descError != null) {
            showError(descError);
            return;
        }

        if (date == null) {
            showError("La date du contrat est requise.");
            return;
        }

        try {
            ContratModel contrat = (contratToEdit != null) ? contratToEdit : new ContratModel();
            contrat.setIdClient(selectedClient.getId());
            contrat.setIdFreelancer(selectedFreelancer.getId());
            contrat.setTitre(titre);
            contrat.setDescription(description);
            contrat.setPrix(prix);
            contrat.setDateContrat(date);
            contrat.setStatut("EN_ATTENTE");

            contratService.createContract(contrat);

            showAlert(Alert.AlertType.INFORMATION, "Succès", "Contrat enregistré avec succès !");
            App.setRoot("contrat");
        } catch (ContractCreationException | IOException e) {
            logger.error("Failed to save contract", e);
            showError("Erreur: " + e.getMessage());
        }
    }

    @FXML
    private void cancel(ActionEvent event) throws IOException {
        App.setRoot("contrat");
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
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
