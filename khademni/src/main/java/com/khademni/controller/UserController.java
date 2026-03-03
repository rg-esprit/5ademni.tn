package com.khademni.controller;

import com.khademni.App;
import com.khademni.model.UserModel;
import com.khademni.utils.MyDataBase;
import com.khademni.utils.VercelBlobUploader;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.time.LocalDate;

public class UserController {

    // Profile fields
    @FXML
    private Label profileAvatarInitials;

    @FXML
    private Label profileDisplayName;

    @FXML
    private Label profileDisplayEmail;

    @FXML
    private Label profileBalanceLabel;

    @FXML
    private Label profileStatusLabel;

    @FXML
    private TextField profileFirstNameField;

    @FXML
    private TextField profileLastNameField;

    @FXML
    private TextField profileEmailField;

    @FXML
    private DatePicker profileDobPicker;

    @FXML
    private TextArea profileBioField;

    @FXML
    private TextField profileImgField;

    @FXML
    private Label profileImgStatusLabel;

    @FXML
    private ImageView profileAvatarImage;

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
    private TextField signupProfileImgField;

    @FXML
    private Label signupImgStatusLabel;

    @FXML
    private TextArea signupBioField;

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

    /** Holds the DB-generated id of the user just created during signup, to allow face enrollment. */
    private int tempNewUserId = -1;

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

        // Load profile data if on the profile page
        if (profileFirstNameField != null) {
            loadProfileData();
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
                App.setCurrentUser(user);
                App.setRoot("profile");
            } else {
                showError("Invalid email or password.", errorLabel);
            }
        } catch (SQLException | IOException e) {
            showError("Error: " + e.getMessage(), errorLabel);
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
                user.setIsAdmin(rs.getBoolean("is_admin"));
                user.setProfileImg(rs.getString("profile_img"));
                user.setBio(rs.getString("bio"));
                try { user.setFaceEmbedding(rs.getString("face_embedding")); } catch (SQLException ignored) {}
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
            plainPasswordField.setText(passwordField.getText());
            passwordField.setVisible(false);
            passwordField.setManaged(false);
            ((org.kordamp.ikonli.javafx.FontIcon) togglePasswordButton.getGraphic()).setIconLiteral("fas-eye-slash");
        } else {
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            passwordField.setText(plainPasswordField.getText());
            plainPasswordField.setVisible(false);
            plainPasswordField.setManaged(false);
            ((org.kordamp.ikonli.javafx.FontIcon) togglePasswordButton.getGraphic()).setIconLiteral("fas-eye");
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
        String profileImg = signupProfileImgField != null && signupProfileImgField.getText() != null
            ? signupProfileImgField.getText().trim()
            : "";
        String bio = signupBioField != null && signupBioField.getText() != null
            ? signupBioField.getText().trim()
            : "";

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
            user.setProfileImg(profileImg);
            user.setBio(bio);
            if (createUser(user)) {
                System.out.println("Signup successful: " + user);
                tempNewUserId = user.getId();
                showError("Account created! You can now enroll your face or proceed to login.", signupErrorLabel);
                
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
        String query = "INSERT INTO users (first_name, last_name, date_of_birth, balance, email, password, is_admin, profile_img, bio) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        Connection conn = MyDataBase.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, user.getFirstName());
            stmt.setString(2, user.getLastName());
            stmt.setDate(3, Date.valueOf(user.getDateOfBirth()));
            stmt.setDouble(4, 0.0); // Initial balance
            stmt.setString(5, user.getEmail());
            stmt.setString(6, hashPassword(user.getPassword()));
            stmt.setBoolean(7, user.isIsAdmin());
            stmt.setString(8, user.getProfileImg() != null ? user.getProfileImg() : "");
            stmt.setString(9, user.getBio() != null ? user.getBio() : "");

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    user.setId(generatedKeys.getInt(1)); // store DB-assigned id on model
                }
                return true;
            }
            return false;
        }
    }

    @FXML
    private void toggleSignupPasswordVisibility(ActionEvent event) {
        passwordVisible = !passwordVisible;

        if (passwordVisible) {
            signupPlainPasswordField.setVisible(true);
            signupPlainPasswordField.setManaged(true);
            signupPlainPasswordField.setText(signupPasswordField.getText());
            signupPasswordField.setVisible(false);
            signupPasswordField.setManaged(false);
            ((org.kordamp.ikonli.javafx.FontIcon) signupTogglePasswordButton.getGraphic()).setIconLiteral("fas-eye-slash");
        } else {
            signupPasswordField.setVisible(true);
            signupPasswordField.setManaged(true);
            signupPasswordField.setText(signupPlainPasswordField.getText());
            signupPlainPasswordField.setVisible(false);
            signupPlainPasswordField.setManaged(false);
            ((org.kordamp.ikonli.javafx.FontIcon) signupTogglePasswordButton.getGraphic()).setIconLiteral("fas-eye");
        }
    }

    @FXML
    private void toggleSignupConfirmPasswordVisibility(ActionEvent event) {
        confirmPasswordVisible = !confirmPasswordVisible;

        if (confirmPasswordVisible) {
            signupPlainConfirmPasswordField.setVisible(true);
            signupPlainConfirmPasswordField.setManaged(true);
            signupPlainConfirmPasswordField.setText(signupConfirmPasswordField.getText());
            signupConfirmPasswordField.setVisible(false);
            signupConfirmPasswordField.setManaged(false);
            ((org.kordamp.ikonli.javafx.FontIcon) signupToggleConfirmPasswordButton.getGraphic()).setIconLiteral("fas-eye-slash");
        } else {
            signupConfirmPasswordField.setVisible(true);
            signupConfirmPasswordField.setManaged(true);
            signupConfirmPasswordField.setText(signupPlainConfirmPasswordField.getText());
            signupPlainConfirmPasswordField.setVisible(false);
            signupPlainConfirmPasswordField.setManaged(false);
            ((org.kordamp.ikonli.javafx.FontIcon) signupToggleConfirmPasswordButton.getGraphic()).setIconLiteral("fas-eye");
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

    // ============ IMAGE UPLOAD METHODS ============

    @FXML
    private void onSignupChooseImage(ActionEvent event) {
        File file = openImageFileChooser();
        if (file == null) return;
        uploadImageAsync(file, signupProfileImgField, signupImgStatusLabel, null);
    }

    @FXML
    private void onProfileChooseImage(ActionEvent event) {
        File file = openImageFileChooser();
        if (file == null) return;
        uploadImageAsync(file, profileImgField, profileImgStatusLabel, profileAvatarImage);
    }

    private File openImageFileChooser() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Profile Image");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        return chooser.showOpenDialog(App.getPrimaryStage());
    }

    private void uploadImageAsync(File file, TextField urlField, Label statusLabel, ImageView avatarImage) {
        if (statusLabel != null) {
            statusLabel.setText("Uploading " + file.getName() + "...");
            statusLabel.setStyle("-fx-text-fill: #6c63ff; -fx-font-size: 13;");
        }

        Thread uploadThread = new Thread(() -> {
            try {
                String url = VercelBlobUploader.upload(file);
                javafx.application.Platform.runLater(() -> {
                    urlField.setText(url);
                    if (statusLabel != null) {
                        statusLabel.setText("Uploaded \u2713");
                        statusLabel.setStyle("-fx-text-fill: #059669; -fx-font-size: 13; -fx-font-weight: 600;");
                    }
                    if (avatarImage != null) {
                        showAvatarImage(avatarImage, url);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    if (statusLabel != null) {
                        statusLabel.setText("Upload failed: " + e.getMessage());
                        statusLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 13;");
                    }
                });
            }
        }, "vercel-blob-upload");
        uploadThread.setDaemon(true);
        uploadThread.start();
    }

    private void showAvatarImage(ImageView imageView, String url) {
        if (url == null || url.isBlank()) {
            imageView.setVisible(false);
            imageView.setManaged(false);
            return;
        }
        // Private blob URLs need authenticated download via the Vercel API
        Thread loader = new Thread(() -> {
            try {
                java.io.InputStream is = VercelBlobUploader.downloadAsStream(url);
                javafx.application.Platform.runLater(() -> {
                    Image img = new Image(is, 100, 100, true, true);
                    imageView.setImage(img);
                    Circle clip = new Circle(50, 50, 50);
                    imageView.setClip(clip);
                    imageView.setVisible(true);
                    imageView.setManaged(true);
                });
            } catch (Exception e) {
                System.err.println("Failed to load avatar from blob: " + e.getMessage());
                e.printStackTrace();
            }
        }, "blob-avatar-loader");
        loader.setDaemon(true);
        loader.start();
    }

    // ============ FACE RECOGNITION METHODS ============

    /**
     * Opens the face-capture dialog in enrollment mode.
     * Can be triggered from signup (uses tempNewUserId) or profile (uses currentUser).
     */
    @FXML
    private void onEnrollFace(ActionEvent event) {
        int userId = -1;
        if (App.getCurrentUser() != null) {
            userId = App.getCurrentUser().getId();
        } else if (tempNewUserId > 0) {
            userId = tempNewUserId;
        }
        if (userId <= 0) {
            Label label = (signupErrorLabel != null) ? signupErrorLabel : errorLabel;
            showError("Please create your account first before enrolling your face.", label);
            return;
        }
        openFaceCaptureDialog(true, userId);
    }

    /**
     * Opens the face-capture dialog in 1:N identification mode (Face ID Login).
     * Available from the login screen.
     */
    @FXML
    private void onFaceLogin(ActionEvent event) {
        openFaceCaptureDialog(false, -1);
    }

    private void openFaceCaptureDialog(boolean enroll, int userId) {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("face_capture.fxml"));
            Parent root = loader.load();
            FaceController fc = loader.getController();

            Stage dialog = new Stage();
            dialog.initOwner(App.getPrimaryStage());
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle(enroll ? "Enroll Face ID" : "Face ID Login");
            dialog.setResizable(false);

            Scene dialogScene = new Scene(root);
            String cssUrl = App.class.getResource("face_capture.css") != null
                    ? App.class.getResource("face_capture.css").toExternalForm() : null;
            if (cssUrl != null) dialogScene.getStylesheets().add(cssUrl);
            dialog.setScene(dialogScene);

            // Configure mode BEFORE showing
            if (enroll) {
                fc.configureEnroll(userId);
            } else {
                fc.configureLogin();
            }

            // Ensure dialog releases resources on window close
            dialog.setOnCloseRequest(e -> dialog.close());

            dialog.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ============ PROFILE METHODS ============

    private void loadProfileData() {
        UserModel user = App.getCurrentUser();
        if (user == null) return;

        profileFirstNameField.setText(user.getFirstName());
        profileLastNameField.setText(user.getLastName());
        profileEmailField.setText(user.getEmail());
        profileDobPicker.setValue(user.getDateOfBirth());
        profileBioField.setText(user.getBio() != null ? user.getBio() : "");
        profileImgField.setText(user.getProfileImg() != null ? user.getProfileImg() : "");

        // Show profile image if URL exists
        if (profileImgStatusLabel != null) {
            String imgUrl = user.getProfileImg();
            if (imgUrl != null && !imgUrl.isBlank()) {
                profileImgStatusLabel.setText("Image loaded");
                profileImgStatusLabel.setStyle("-fx-text-fill: #059669; -fx-font-size: 13;");
            } else {
                profileImgStatusLabel.setText("No image selected");
                profileImgStatusLabel.setStyle("-fx-text-fill: #a0a0a0; -fx-font-size: 13;");
            }
        }
        if (profileAvatarImage != null) {
            String imgUrl = user.getProfileImg();
            if (imgUrl != null && !imgUrl.isBlank()) {
                showAvatarImage(profileAvatarImage, imgUrl);
                // Hide initials when image is shown
                profileAvatarInitials.setVisible(false);
                profileAvatarInitials.setManaged(false);
            } else {
                profileAvatarImage.setVisible(false);
                profileAvatarImage.setManaged(false);
                profileAvatarInitials.setVisible(true);
                profileAvatarInitials.setManaged(true);
            }
        }

        // Display section
        String fullName = user.getFirstName() + " " + user.getLastName();
        profileDisplayName.setText(fullName);
        profileDisplayEmail.setText(user.getEmail());
        profileBalanceLabel.setText(String.format("$%.2f", user.getBalance()));

        // Avatar initials
        String initials = "";
        if (user.getFirstName() != null && !user.getFirstName().isEmpty()) {
            initials += user.getFirstName().charAt(0);
        }
        if (user.getLastName() != null && !user.getLastName().isEmpty()) {
            initials += user.getLastName().charAt(0);
        }
        profileAvatarInitials.setText(initials.toUpperCase());
    }

    @FXML
    private void onProfileSave(ActionEvent event) {
        UserModel user = App.getCurrentUser();
        if (user == null) return;

        String firstName = profileFirstNameField.getText().trim();
        String lastName = profileLastNameField.getText().trim();
        String email = profileEmailField.getText().trim();
        LocalDate dob = profileDobPicker.getValue();
        String bio = profileBioField.getText() != null ? profileBioField.getText().trim() : "";
        String profileImg = profileImgField.getText() != null ? profileImgField.getText().trim() : "";

        // Validation
        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty()) {
            showProfileStatus("Please fill in all required fields.", true);
            return;
        }

        if (!isValidEmail(email)) {
            showProfileStatus("Please enter a valid email address.", true);
            return;
        }

        if (dob == null) {
            showProfileStatus("Please select your date of birth.", true);
            return;
        }

        // Update in database
        try {
            String query = "UPDATE users SET first_name = ?, last_name = ?, email = ?, date_of_birth = ?, bio = ?, profile_img = ? WHERE id = ?";
            Connection conn = MyDataBase.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, firstName);
                stmt.setString(2, lastName);
                stmt.setString(3, email);
                stmt.setDate(4, Date.valueOf(dob));
                stmt.setString(5, bio);
                stmt.setString(6, profileImg);
                stmt.setInt(7, user.getId());

                int rows = stmt.executeUpdate();
                if (rows > 0) {
                    // Update the local user object
                    user.setFirstName(firstName);
                    user.setLastName(lastName);
                    user.setEmail(email);
                    user.setDateOfBirth(dob);
                    user.setBio(bio);
                    user.setProfileImg(profileImg);
                    App.setCurrentUser(user);

                    // Refresh display
                    loadProfileData();
                    showProfileStatus("Profile updated successfully!", false);
                } else {
                    showProfileStatus("Failed to update profile.", true);
                }
            }
        } catch (SQLException e) {
            showProfileStatus("Database error: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }

    @FXML
    private void onProfileCancel(ActionEvent event) {
        loadProfileData(); // Reset to current values
    }

    @FXML
    private void onSignOut(ActionEvent event) {
        App.setCurrentUser(null);
        try {
            App.setRoot("login");
        } catch (IOException e) {
            System.out.println("Error navigating to login: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void onDeleteAccount(ActionEvent event) {
        UserModel currentUser = App.getCurrentUser();
        if (currentUser == null) {
            showProfileStatus("No user logged in.", true);
            return;
        }

        // Show confirmation dialog
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(App.getPrimaryStage());
        alert.setTitle("Delete Account");
        alert.setHeaderText("Are you sure you want to delete your account?");
        alert.setContentText("This action cannot be undone. All your data will be permanently deleted.");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                Connection conn = MyDataBase.getConnection();
                
                // First delete all reviews where user is client or freelancer
                String deleteReviewsQuery = "DELETE FROM reviews WHERE client_id = ? OR freelancer_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(deleteReviewsQuery)) {
                    stmt.setInt(1, currentUser.getId());
                    stmt.setInt(2, currentUser.getId());
                    stmt.executeUpdate();
                }
                
                // Then delete the user account
                String deleteUserQuery = "DELETE FROM users WHERE id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(deleteUserQuery)) {
                    stmt.setInt(1, currentUser.getId());
                    int rowsAffected = stmt.executeUpdate();
                    if (rowsAffected > 0) {
                        App.setCurrentUser(null);
                        App.setRoot("login");
                    } else {
                        showProfileStatus("Failed to delete account.", true);
                    }
                }
            } catch (SQLException | IOException e) {
                showProfileStatus("Error deleting account: " + e.getMessage(), true);
                e.printStackTrace();
            }
        }
    }

    private void showProfileStatus(String message, boolean isError) {
        if (profileStatusLabel == null) return;
        profileStatusLabel.setText(message);
        profileStatusLabel.setVisible(true);
        profileStatusLabel.setManaged(true);
        if (isError) {
            profileStatusLabel.setStyle("-fx-text-fill: #dc2626; -fx-background-color: #fef2f2; -fx-border-color: #fecaca; -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 12 20 12 20; -fx-font-size: 14; -fx-font-weight: 600; -fx-alignment: center;");
        } else {
            profileStatusLabel.setStyle("-fx-text-fill: #059669; -fx-background-color: #ecfdf5; -fx-border-color: #a7f3d0; -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 12 20 12 20; -fx-font-size: 14; -fx-font-weight: 600; -fx-alignment: center;");
        }
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





@FXML
private void onShowFavoris() {
    try {
        App.setRoot("Article/favoris");
    } catch (IOException e) {
        e.printStackTrace();
        showAlert(Alert.AlertType.ERROR, "Erreur", 
                  "Impossible d'ouvrir la page des favoris.");
    }
}



private void showAlert(Alert.AlertType type, String title, String message) {
    Alert alert = new Alert(type);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(message);
    alert.showAndWait();
}



}
