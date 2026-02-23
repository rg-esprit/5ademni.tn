package com.khademni.controller;

import com.khademni.App;
import com.khademni.service.StripeService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import java.io.IOException;

public class PaiementFormController {

    @FXML
    private TextField amountField;

    @FXML
    private TextField descriptionField;

    private final StripeService stripeService = new StripeService();

    @FXML
    private void handleGeneratePaymentLink() {
        String amountStr = amountField.getText().trim();
        String description = descriptionField.getText().trim();

        if (amountStr.isEmpty() || description.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champs Requis", "Veuillez remplir tous les champs.");
            return;
        }

        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                showAlert(Alert.AlertType.WARNING, "Montant Invalide", "Le montant doit être supérieur à zéro.");
                return;
            }

            String paymentUrl = stripeService.createCheckoutSession(amount, description, 0);

            if (App.getAppHostServices() != null) {
                App.getAppHostServices().showDocument(paymentUrl);
                showAlert(Alert.AlertType.INFORMATION, "Redirection",
                        "Le lien de paiement a été ouvert dans votre navigateur.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le navigateur. Lien: " + paymentUrl);
            }

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Format Invalide", "Veuillez saisir un nombre valide pour le montant.");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur Stripe", "Erreur lors de la génération: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() throws IOException {
        App.setRoot("paiement");
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
