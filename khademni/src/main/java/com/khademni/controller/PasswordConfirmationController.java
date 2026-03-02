package com.khademni.controller;

import com.khademni.App;
import com.khademni.utils.PasswordUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;

public class PasswordConfirmationController {

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    private boolean confirmed = false;

    @FXML
    private void onConfirm() {
        String password = passwordField.getText();
        if (App.getCurrentUser() == null || App.getCurrentUser().getPassword() == null) {
            System.out.println("[PasswordConfirm] ERROR: currentUser is null or has no password!");
            errorLabel.setText("Erreur: utilisateur non connecté.");
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
            return;
        }
        String hashedPassword = App.getCurrentUser().getPassword();
        System.out.println("[PasswordConfirm] Checking password for user: " + App.getCurrentUser().getEmail());
        System.out.println("[PasswordConfirm] Stored hash starts with: "
                + hashedPassword.substring(0, Math.min(10, hashedPassword.length())));

        if (PasswordUtil.checkPassword(password, hashedPassword)) {
            System.out.println("[PasswordConfirm] Password verified successfully!");
            confirmed = true;
            closeStage();
        } else {
            System.out.println("[PasswordConfirm] Password verification FAILED.");
            errorLabel.setText("Mot de passe incorrect.");
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }

    @FXML
    private void onCancel() {
        confirmed = false;
        closeStage();
    }

    private void closeStage() {
        Stage stage = (Stage) passwordField.getScene().getWindow();
        stage.close();
    }

    public boolean isConfirmed() {
        return confirmed;
    }
}
