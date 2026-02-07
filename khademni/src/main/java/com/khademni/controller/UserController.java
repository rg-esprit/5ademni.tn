package com.khademni.controller;

import com.khademni.App;
import com.khademni.model.UserModel;
import com.khademni.utils.MyDataBase;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.time.LocalDate;

public class UserController {

    // Login fields
    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField plainPasswordField;

    @FXML
    private Button togglePasswordButton;

    @FXML
    private Button signInButton;

    @FXML
    private Label errorLabel;

    // Signup fields
    @FXML
    private TextField signupFirstNameField;

    @FXML
    private TextField signupLastNameField;

    @FXML
    private DatePicker signupDobPicker;

    @FXML
    private TextField signupEmailField;

    @FXML
    private PasswordField signupPasswordField;

    @FXML
    private TextField signupPlainPasswordField;

    @FXML
    private PasswordField signupConfirmPasswordField;

    @FXML
    private TextField signupPlainConfirmPasswordField;

    @FXML
    private Button signupTogglePasswordButton;

    @FXML
    private Button signupToggleConfirmPasswordButton;

    @FXML
    private Button signUpButton;

    @FXML
    private Label signupErrorLabel;

    private boolean passwordVisible = false;
    private boolean confirmPasswordVisible = false;

    @FXML
    public void initialize() {
        // Bind password fields for login
        if (plainPasswordField != null && passwordField != null) {
            plainPasswordField.textProperty().bindBidirectional(passwordField.textProperty());
        }

        // Bind password fields for signup
        if (signupPlainPasswordField != null && signupPasswordField != null) {
            signupPlainPasswordField.textProperty().bindBidirectional(signupPasswordField.textProperty());
        }

        if (signupPlainConfirmPasswordField != null && signupConfirmPasswordField != null) {
            signupPlainConfirmPasswordField.textProperty().bindBidirectional(signupConfirmPasswordField.textProperty());
        }
    }

    // ============ LOGIN METHODS ============

    @FXML
    private void onSignIn(ActionEvent event) {
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();

        // Clear previous errors
        errorLabel.setText("");
        errorLabel.setVisible(false);

        // Validation
        if (email.isEmpty() || password.isEmpty()) {
            showError("Please fill in all fields.", errorLabel);
            return;
        }

        if (!isValidEmail(email)) {
            showError("Please enter a valid email address.", errorLabel);
            return;
        }

        // Authenticate user against database
        try {
            UserModel user = authenticateUser(email, password);
            if (user != null) {
                System.out.println("Login successful: " + user);
                showError("Login successful! Welcome " + user.getFirstName(), errorLabel);
                // TODO: Navigate to main application
            } else {
                showError("Invalid email or password.", errorLabel);
            }
        } catch (SQLException e) {
            showError("Database error: " + e.getMessage(), errorLabel);
            e.printStackTrace();
        }
    }

    private UserModel authenticateUser(String email, String password) throws SQLException {
        String hashedPassword = hashPassword(password);
        String query = "SELECT * FROM users WHERE email = ? AND password = ?";

        Connection conn = MyDataBase.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, email);
            stmt.setString(2, hashedPassword);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                UserModel user = new UserModel();
                user.setId(rs.getInt("id"));
                user.setFirstName(rs.getString("first_name"));
                user.setLastName(rs.getString("last_name"));
                user.setDateOfBirth(rs.getDate("date_of_birth").toLocalDate());
                user.setBalance(rs.getDouble("balance"));
                user.setEmail(rs.getString("email"));
                user.setPassword(rs.getString("password"));
                return user;
            }
        }

        return null;
    }

    @FXML
    private void togglePasswordVisibility(ActionEvent event) {
        passwordVisible = !passwordVisible;

        if (passwordVisible) {
            plainPasswordField.setVisible(true);
            plainPasswordField.setManaged(true);
            passwordField.setVisible(false);
            passwordField.setManaged(false);
            togglePasswordButton.setText("&#xe8f5;"); // visibility_off
        } else {
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            plainPasswordField.setVisible(false);
            plainPasswordField.setManaged(false);
            togglePasswordButton.setText("&#xe8f4;"); // visibility
        }
    }

    @FXML
    private void onCreateAccount(ActionEvent event) {
        try {
            App.setRoot("signup");
        } catch (IOException e) {
            System.out.println("Error navigating to signup: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void onForgotPassword(ActionEvent event) {
        System.out.println("Navigate to forgot password");
        // TODO: Navigate to forgot password page
    }

    @FXML
    private void onGoogleSignIn(ActionEvent event) {
        System.out.println("Google Sign In clicked");
        // TODO: Implement Google OAuth
    }

    @FXML
    private void onAppleSignIn(ActionEvent event) {
        System.out.println("Apple Sign In clicked");
        // TODO: Implement Apple Sign In
    }

    // ============ SIGNUP METHODS ============

    @FXML
    private void onSignUp(ActionEvent event) {
        String firstName = signupFirstNameField.getText().trim();
        String lastName = signupLastNameField.getText().trim();
        LocalDate dob = signupDobPicker.getValue();
        String email = signupEmailField.getText().trim();
        String password = signupPasswordField.getText().trim();
        String confirmPassword = signupConfirmPasswordField.getText().trim();

        // Clear previous errors
        signupErrorLabel.setText("");
        signupErrorLabel.setVisible(false);

        // Validation
        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showError("Please fill in all required fields.", signupErrorLabel);
            return;
        }

        if (dob == null) {
            showError("Please select your date of birth.", signupErrorLabel);
            return;
        }

        if (!isValidEmail(email)) {
            showError("Please enter a valid email address.", signupErrorLabel);
            return;
        }

        if (password.length() < 6) {
            showError("Password must be at least 6 characters long.", signupErrorLabel);
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match.", signupErrorLabel);
            return;
        }

        // Check if user already exists
        try {
            if (userExists(email)) {
                showError("An account with this email already exists.", signupErrorLabel);
                return;
            }

            // Create new user
            UserModel user = new UserModel(firstName, lastName, dob, email, password);
            if (createUser(user)) {
                System.out.println("Signup successful: " + user);
                showError("Account created successfully! Redirecting to login...", signupErrorLabel);
                
                // Navigate to login after 2 seconds
                new Thread(() -> {
                    try {
                        Thread.sleep(2000);
                        javafx.application.Platform.runLater(() -> {
                            try {
                                App.setRoot("login");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
            } else {
                showError("Failed to create account. Please try again.", signupErrorLabel);
            }
        } catch (SQLException e) {
            showError("Database error: " + e.getMessage(), signupErrorLabel);
            e.printStackTrace();
        }
    }

    private boolean userExists(String email) throws SQLException {
        String query = "SELECT COUNT(*) FROM users WHERE email = ?";

        Connection conn = MyDataBase.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }

        return false;
    }

    private boolean createUser(UserModel user) throws SQLException {
        String query = "INSERT INTO users (first_name, last_name, date_of_birth, balance, email, password) VALUES (?, ?, ?, ?, ?, ?)";

        Connection conn = MyDataBase.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, user.getFirstName());
            stmt.setString(2, user.getLastName());
            stmt.setDate(3, Date.valueOf(user.getDateOfBirth()));
            stmt.setDouble(4, 0.0); // Initial balance
            stmt.setString(5, user.getEmail());
            stmt.setString(6, hashPassword(user.getPassword()));

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    @FXML
    private void toggleSignupPasswordVisibility(ActionEvent event) {
        passwordVisible = !passwordVisible;

        if (passwordVisible) {
            signupPlainPasswordField.setVisible(true);
            signupPlainPasswordField.setManaged(true);
            signupPasswordField.setVisible(false);
            signupPasswordField.setManaged(false);
            signupTogglePasswordButton.setText("&#xe8f5;"); // visibility_off
        } else {
            signupPasswordField.setVisible(true);
            signupPasswordField.setManaged(true);
            signupPlainPasswordField.setVisible(false);
            signupPlainPasswordField.setManaged(false);
            signupTogglePasswordButton.setText("&#xe8f4;"); // visibility
        }
    }

    @FXML
    private void toggleSignupConfirmPasswordVisibility(ActionEvent event) {
        confirmPasswordVisible = !confirmPasswordVisible;

        if (confirmPasswordVisible) {
            signupPlainConfirmPasswordField.setVisible(true);
            signupPlainConfirmPasswordField.setManaged(true);
            signupConfirmPasswordField.setVisible(false);
            signupConfirmPasswordField.setManaged(false);
            signupToggleConfirmPasswordButton.setText("&#xe8f5;"); // visibility_off
        } else {
            signupConfirmPasswordField.setVisible(true);
            signupConfirmPasswordField.setManaged(true);
            signupPlainConfirmPasswordField.setVisible(false);
            signupPlainConfirmPasswordField.setManaged(false);
            signupToggleConfirmPasswordButton.setText("&#xe8f4;"); // visibility
        }
    }

    @FXML
    private void onAlreadyHaveAccount(ActionEvent event) {
        try {
            App.setRoot("login");
        } catch (IOException e) {
            System.out.println("Error navigating to login: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void onSignupGoogleSignIn(ActionEvent event) {
        System.out.println("Google Sign Up clicked");
        // TODO: Implement Google OAuth
    }

    @FXML
    private void onSignupAppleSignIn(ActionEvent event) {
        System.out.println("Apple Sign Up clicked");
        // TODO: Implement Apple Sign In
    }

    // ============ UTILITY METHODS ============

    private void showError(String message, Label label) {
        if (message == null || message.isEmpty()) {
            label.setVisible(false);
            label.setManaged(false);
        } else {
            label.setText(message);
            label.setVisible(true);
            label.setManaged(true);
        }
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }
}
