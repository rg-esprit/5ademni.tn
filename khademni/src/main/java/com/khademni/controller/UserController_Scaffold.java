package com.khademni.controller;

import com.khademni.App;
import com.khademni.model.UserModel;
import com.khademni.service.ServiceFactory;
import com.khademni.service.UserService;
import com.khademni.exception.BusinessException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Refactored UserController (Scaffold).
 * Focuses strictly on UI binding and user interaction.
 * Business logic and data access are delegated to the Service layer.
 */
public class UserController_Scaffold {
    private static final Logger logger = LoggerFactory.getLogger(UserController_Scaffold.class);

    // Dependency injection (via Service Factory)
    private final UserService userService = ServiceFactory.getUserService();

    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label errorLabel;

    @FXML
    private void onSignIn(ActionEvent event) {
        String email = emailField.getText();
        String password = passwordField.getText();

        try {
            // Business Logic is here
            UserModel user = userService.login(email, password);

            // Navigate on success
            App.setCurrentUser(user);
            App.setRoot("profile");

        } catch (BusinessException e) {
            // Expected logical errors (wrong password, etc.)
            showError(e.getMessage());
        } catch (Exception e) {
            // Unexpected technical errors
            logger.error("Unexpected error during sign-in", e);
            showError("Une erreur inattendue est survenue.");
        }
    }

    @FXML
    private void onSignUp(ActionEvent event) {
        // Implementation of signup following the same pattern:
        // 1. Extract and validate basic UI input
        // 2. Wrap in UserModel
        // 3. Call userService.register(user)
        // 4. Handle BusinessException specifically
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
}
