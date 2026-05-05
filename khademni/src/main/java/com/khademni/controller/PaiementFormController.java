package com.khademni.controller;

import com.khademni.App;
import com.khademni.service.StripeService;
import com.khademni.utils.CustomAlert;
import javafx.fxml.FXML;
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
            CustomAlert.showWarning("Champs Requis", "Veuillez remplir tous les champs.");
            return;
        }

        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                CustomAlert.showWarning("Montant Invalide", "Le montant doit etre superieur a zero.");
                return;
            }

            String paymentUrl = stripeService.createCheckoutSession(amount, description, 0);

            if (App.getAppHostServices() != null) {
                App.getAppHostServices().showDocument(paymentUrl);
                CustomAlert.showSuccess("Redirection",
                        "Le lien de paiement a ete ouvert dans votre navigateur.");
            } else {
                CustomAlert.showError("Erreur", "Impossible d'ouvrir le navigateur. Lien: " + paymentUrl);
            }

        } catch (NumberFormatException e) {
            CustomAlert.showError("Format Invalide", "Veuillez saisir un nombre valide pour le montant.");
        } catch (Exception e) {
            e.printStackTrace();
            CustomAlert.showError("Erreur Stripe", "Erreur lors de la generation: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() throws IOException {
        App.setRoot("paiement");
    }
}
