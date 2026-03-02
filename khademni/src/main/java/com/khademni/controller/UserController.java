package com.khademni.controller;

import com.khademni.App;
import com.khademni.model.UserModel;
import com.khademni.model.UserRole;
import com.khademni.utils.MyDataBase;
import com.khademni.utils.VercelBlobUploader;
import com.khademni.utils.ValidationUtils;
import com.khademni.utils.SpellCheckDecorator;
import com.khademni.service.ServiceFactory;
import com.khademni.service.UserService;
import com.khademni.exception.BusinessException;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import com.khademni.service.StripeService;
import com.khademni.service.DepositVerificationResult;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.cell.PropertyValueFactory;
import com.khademni.service.FileStorageService;

public class UserController {
    private final UserService userService = ServiceFactory.getUserService();
    private final StripeService stripeService = new StripeService();

    // Profile fields
    @FXML
    private Label profileAvatarInitials;

    @FXML
    private ImageView profileAvatarImage;

    @FXML
    private StackPane avatarPane;

    @FXML
    private Label profileDisplayName;

    @FXML
    private Label profileDisplayEmail;

    @FXML
    private Label profileBalanceLabel;

    @FXML
    private Label profileStatusLabel;

    @FXML
    private Label profileFirstNameLabel;
    @FXML
    private Label profileLastNameLabel;
    @FXML
    private Label profileEmailLabel;

    @FXML
    private TextField profileFirstNameField;
    @FXML
    private TextField profileLastNameField;
    @FXML
    private TextField profileEmailField;

    @FXML
    private Button editFirstNameBtn;
    @FXML
    private Button editLastNameBtn;
    @FXML
    private Button editEmailBtn;

    @FXML
    private javafx.scene.layout.HBox firstNameActionBox;
    @FXML
    private javafx.scene.layout.HBox lastNameActionBox;
    @FXML
    private javafx.scene.layout.HBox emailActionBox;

    // Role toggle fields
    @FXML
    private Button roleClientBtn;

    @FXML
    private Button roleFreelancerBtn;

    @FXML
    private VBox cvSection;

    @FXML
    private Label cvFileLabel;

    @FXML
    private Button cvUploadBtn;
    @FXML
    private TableView<CVRecord> cvTableView;
    @FXML
    private TableColumn<CVRecord, String> cvNameColumn;
    @FXML
    private TableColumn<CVRecord, String> cvDateColumn;
    @FXML
    private TableColumn<CVRecord, Void> cvActionColumn;

    @FXML
    private Label profileBioLabel;
    @FXML
    private TextArea profileBioField;
    @FXML
    private Button editBioBtn;
    @FXML
    private VBox bioEditBox;

    @FXML
    private CheckBox languageToolAutoCorrectCheckbox;

    private UserRole selectedMode = UserRole.CLIENT;
    private String selectedCvPath = null;

    @FXML
    private Label profileImgStatusLabel;

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
    private TextField signupUserIdField;

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
    private ImageView signupAvatarPreview;

    @FXML
    private org.kordamp.ikonli.javafx.FontIcon signupAvatarPlaceholder;

    private File selectedSignupPhoto;

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
        // Ensure role and cv_path columns exist in the database
        ensureRoleAndCvColumns();

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

        // Ensure signup ID field is empty by default so user can decide
        if (signupUserIdField != null) {
            signupUserIdField.clear();
        }

        // Initialize CV Table
        if (cvTableView != null) {
            setupCVTable();
        }

        // Load profile data if on the profile page
        if (profileFirstNameField != null) {
            loadProfileData();

            if (languageToolAutoCorrectCheckbox != null) {
                languageToolAutoCorrectCheckbox.setSelected(SpellCheckDecorator.isEnabled());
            }

            // Attach live LanguageTool spell-check to profile text fields
            SpellCheckDecorator.attach(profileFirstNameField, profileStatusLabel, "fr");
            SpellCheckDecorator.attach(profileLastNameField, profileStatusLabel, "fr");
            SpellCheckDecorator.attach(profileBioField, profileStatusLabel, "fr");
        }

        // Attach live spell-check for signup fields
        if (signupFirstNameField != null) {
            SpellCheckDecorator.attach(signupFirstNameField, "fr");
        }
        if (signupLastNameField != null) {
            SpellCheckDecorator.attach(signupLastNameField, "fr");
        }
        if (signupBioField != null) {
            SpellCheckDecorator.attach(signupBioField, "fr");
        }
    }

    private void setupCVTable() {
        cvNameColumn.setCellValueFactory(new PropertyValueFactory<>("fileName"));
        cvDateColumn.setCellValueFactory(new PropertyValueFactory<>("uploadDate"));

        cvActionColumn.setCellFactory(param -> new TableCell<>() {
            private final Button downloadBtn = new Button("Download");

            {
                downloadBtn.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-cursor: hand;");
                downloadBtn.setOnAction(event -> {
                    CVRecord record = getTableView().getItems().get(getIndex());
                    downloadCV(record.getFilePath());
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(downloadBtn);
                }
            }
        });
    }

    private void downloadCV(String filePath) {
        if (filePath == null || filePath.isEmpty())
            return;
        try {
            File file = new File(filePath);
            if (file.exists()) {
                App.getAppHostServices().showDocument(file.toURI().toString());
            } else {
                showProfileStatus("Le fichier n'existe plus sur le serveur.", true);
            }
        } catch (Exception e) {
            showProfileStatus("Erreur lors de l'ouverture du fichier.", true);
            e.printStackTrace();
        }
    }

    public static class CVRecord {
        private final String fileName;
        private final String uploadDate;
        private final String filePath;

        public CVRecord(String fileName, String uploadDate, String filePath) {
            this.fileName = fileName;
            this.uploadDate = uploadDate;
            this.filePath = filePath;
        }

        public String getFileName() {
            return fileName;
        }

        public String getUploadDate() {
            return uploadDate;
        }

        public String getFilePath() {
            return filePath;
        }
    }

    // ============ LOGIN METHODS ============

    @FXML
    private void onSignIn(ActionEvent event) {
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();

        errorLabel.setText("");
        errorLabel.setVisible(false);

        try {
            UserModel user = userService.login(email, password);
            App.setCurrentUser(user);
            App.setRoot("profile");
        } catch (BusinessException e) {
            showError(e.getMessage(), errorLabel);
        } catch (Exception e) {
            showError("Une erreur est survenue lors de la connexion.", errorLabel);
            e.printStackTrace();
        }
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
        App.setCurrentUser(null); // Clear any residual state
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
        String email = signupEmailField.getText().trim();
        String password = signupPasswordField.getText().trim();

        signupErrorLabel.setText("");
        signupErrorLabel.setVisible(false);

        try {
            String userIdText = signupUserIdField.getText().trim();
            int uniqueId;
            if (userIdText.isEmpty()) {
                uniqueId = com.khademni.service.UserIdService.generateUniqueId();
            } else {
                try {
                    uniqueId = Integer.parseInt(userIdText);
                } catch (NumberFormatException e) {
                    throw new BusinessException("L'ID doit être composé uniquement de chiffres.");
                }
                if (!com.khademni.service.UserIdService.isValid(uniqueId)) {
                    throw new BusinessException("L'ID doit comporter exactement 4 chiffres (1000–9999).");
                }
                if (!com.khademni.service.UserIdService.isIdAvailable(uniqueId)) {
                    throw new BusinessException("Cet ID est déjà utilisé. Veuillez en choisir un autre.");
                }
            }

            UserModel user = new UserModel(
                    uniqueId,
                    ValidationUtils.autoCorrect(signupFirstNameField.getText()),
                    ValidationUtils.autoCorrect(signupLastNameField.getText()),
                    signupDobPicker.getValue(),
                    email,
                    password);
            user.setBio(signupBioField != null ? ValidationUtils.autoCorrect(signupBioField.getText()) : "");

            if (selectedSignupPhoto != null) {
                user.setProfileImage(FileStorageService.storeProfileImage(selectedSignupPhoto));
            }

            userService.register(user);
            showError("Compte créé avec succès ! Redirection...", signupErrorLabel);

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

        } catch (BusinessException e) {
            showError(e.getMessage(), signupErrorLabel);
        } catch (Exception e) {
            showError("Erreur: " + e.getClass().getSimpleName() + " - " + e.getMessage(), signupErrorLabel);
            e.printStackTrace();
        }
    }

    @FXML
    private void onSignupChoosePhoto(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose Profile Photo");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));

        Stage stage = (Stage) signupErrorLabel.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            selectedSignupPhoto = file;
            Image img = new Image(file.toURI().toString());
            signupAvatarPreview.setImage(img);
            signupAvatarPreview.setVisible(true);
            signupAvatarPlaceholder.setVisible(false);
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
            ((org.kordamp.ikonli.javafx.FontIcon) signupTogglePasswordButton.getGraphic())
                    .setIconLiteral("fas-eye-slash");
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
            ((org.kordamp.ikonli.javafx.FontIcon) signupToggleConfirmPasswordButton.getGraphic())
                    .setIconLiteral("fas-eye-slash");
        } else {
            signupConfirmPasswordField.setVisible(true);
            signupConfirmPasswordField.setManaged(true);
            signupConfirmPasswordField.setText(signupPlainConfirmPasswordField.getText());
            signupPlainConfirmPasswordField.setVisible(false);
            signupPlainConfirmPasswordField.setManaged(false);
            ((org.kordamp.ikonli.javafx.FontIcon) signupToggleConfirmPasswordButton.getGraphic())
                    .setIconLiteral("fas-eye");
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
        if (user != null) {
            String fullName = user.getFirstName() + " " + user.getLastName();
            profileDisplayName.setText(fullName);
            profileDisplayEmail.setText(user.getEmail());
            profileBalanceLabel.setText(String.format("%.2f €", user.getBalance()));

            // Populate form fields (for edit mode)
            profileFirstNameField.setText(user.getFirstName());
            profileLastNameField.setText(user.getLastName());
            profileEmailField.setText(user.getEmail());

            // Populate labels (for view mode)
            profileFirstNameLabel.setText(user.getFirstName());
            profileLastNameLabel.setText(user.getLastName());
            profileEmailLabel.setText(user.getEmail());

            // Avatar circle logic
            String initials = "";
            if (user.getFirstName() != null && !user.getFirstName().isEmpty())
                initials += user.getFirstName().charAt(0);
            if (user.getLastName() != null && !user.getLastName().isEmpty())
                initials += user.getLastName().charAt(0);
            profileAvatarInitials.setText(initials.toUpperCase());

            if (user.getProfileImage() != null && !user.getProfileImage().isEmpty()) {
                loadAvatarImage(user.getProfileImage());
            }

            // Role and CV logic
            selectedMode = user.getCurrentMode() != null ? user.getCurrentMode() : UserRole.CLIENT;
            selectedCvPath = user.getCvPath();

            // Bio
            profileBioLabel
                    .setText(user.getBio() == null || user.getBio().isEmpty() ? "No bio provided." : user.getBio());
            profileBioField.setText(user.getBio());

            updateRoleToggleUI();
        }
    }

    private void refreshCVTable() {
        if (cvTableView == null)
            return;

        ObservableList<CVRecord> cvData = FXCollections.observableArrayList();
        if (selectedCvPath != null && !selectedCvPath.isEmpty()) {
            File cvFile = new File(selectedCvPath);
            UserModel user = App.getCurrentUser();
            String dateStr = user.getCvUploadedAt() != null
                    ? user.getCvUploadedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                    : "N/A";
            cvData.add(new CVRecord(cvFile.getName(), dateStr, selectedCvPath));

            if (cvUploadBtn != null) {
                cvUploadBtn.setText("Mettre à jour le CV");
            }
        } else {
            if (cvUploadBtn != null) {
                cvUploadBtn.setText("Uploader mon CV");
            }
        }
        cvTableView.setItems(cvData);
    }

    // --- Inline Editing Actions ---

    @FXML
    private void onEditFirstName() {
        toggleEditField(profileFirstNameLabel, profileFirstNameField, editFirstNameBtn, firstNameActionBox, true);
    }

    @FXML
    private void onCancelEditFirstName() {
        profileFirstNameField.setText(profileFirstNameLabel.getText());
        toggleEditField(profileFirstNameLabel, profileFirstNameField, editFirstNameBtn, firstNameActionBox, false);
    }

    @FXML
    private void onSaveFirstName() {
        String newVal = ValidationUtils.autoCorrect(profileFirstNameField.getText());
        profileFirstNameField.setText(newVal);
        String err = ValidationUtils.validateField(newVal, "Le prénom", 2);
        if (err != null) {
            showProfileStatus(err, true);
            return;
        }

        if (!showPasswordConfirmation())
            return;

        try {
            UserModel user = App.getCurrentUser();
            user.setFirstName(newVal);
            userService.updateProfile(user);

            profileFirstNameLabel.setText(user.getFirstName());
            toggleEditField(profileFirstNameLabel, profileFirstNameField, editFirstNameBtn, firstNameActionBox, false);
            profileDisplayName.setText(user.getFirstName() + " " + user.getLastName());
            showProfileStatus("Prénom mis à jour !", false);
        } catch (BusinessException e) {
            showProfileStatus(e.getMessage(), true);
        }
    }

    @FXML
    private void onEditLastName() {
        toggleEditField(profileLastNameLabel, profileLastNameField, editLastNameBtn, lastNameActionBox, true);
    }

    @FXML
    private void onCancelEditLastName() {
        profileLastNameField.setText(profileLastNameLabel.getText());
        toggleEditField(profileLastNameLabel, profileLastNameField, editLastNameBtn, lastNameActionBox, false);
    }

    @FXML
    private void onSaveLastName() {
        String newVal = ValidationUtils.autoCorrect(profileLastNameField.getText());
        profileLastNameField.setText(newVal);
        String err = ValidationUtils.validateField(newVal, "Le nom", 2);
        if (err != null) {
            showProfileStatus(err, true);
            return;
        }

        if (!showPasswordConfirmation())
            return;

        try {
            UserModel user = App.getCurrentUser();
            user.setLastName(newVal);
            userService.updateProfile(user);

            profileLastNameLabel.setText(user.getLastName());
            toggleEditField(profileLastNameLabel, profileLastNameField, editLastNameBtn, lastNameActionBox, false);
            profileDisplayName.setText(user.getFirstName() + " " + user.getLastName());
            showProfileStatus("Nom mis à jour !", false);
        } catch (BusinessException e) {
            showProfileStatus(e.getMessage(), true);
        }
    }

    @FXML
    private void onEditEmail() {
        toggleEditField(profileEmailLabel, profileEmailField, editEmailBtn, emailActionBox, true);
    }

    @FXML
    private void onCancelEditEmail() {
        profileEmailField.setText(profileEmailLabel.getText());
        toggleEditField(profileEmailLabel, profileEmailField, editEmailBtn, emailActionBox, false);
    }

    @FXML
    private void onSaveEmail() {
        String newVal = profileEmailField.getText().trim();
        if (!ValidationUtils.isValidEmail(newVal)) {
            showProfileStatus("Format d'email invalide.", true);
            return;
        }

        if (!showPasswordConfirmation())
            return;

        try {
            UserModel user = App.getCurrentUser();
            user.setEmail(ValidationUtils.sanitize(newVal));
            userService.updateProfile(user);

            profileEmailLabel.setText(user.getEmail());
            profileDisplayEmail.setText(user.getEmail());
            toggleEditField(profileEmailLabel, profileEmailField, editEmailBtn, emailActionBox, false);
            showProfileStatus("Email mis à jour !", false);
        } catch (BusinessException e) {
            showProfileStatus(e.getMessage(), true);
        }
    }

    private void toggleEditField(Label lbl, TextField txt, Button editBtn, javafx.scene.layout.HBox actionBox,
            boolean edit) {
        lbl.setVisible(!edit);
        lbl.setManaged(!edit);
        txt.setVisible(edit);
        txt.setManaged(edit);
        editBtn.setVisible(!edit);
        editBtn.setManaged(!edit);
        actionBox.setVisible(edit);
        actionBox.setManaged(edit);
        if (edit)
            txt.requestFocus();
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

    private void showProfileStatus(String message, boolean isError) {
        if (profileStatusLabel == null)
            return;
        profileStatusLabel.setText(message);
        profileStatusLabel.setVisible(true);
        profileStatusLabel.setManaged(true);
        if (isError) {
            profileStatusLabel.setStyle(
                    "-fx-text-fill: #dc2626; -fx-background-color: #fef2f2; -fx-border-color: #fecaca; -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 12 20 12 20; -fx-font-weight: 600;");
        } else {
            profileStatusLabel.setStyle(
                    "-fx-text-fill: #059669; -fx-background-color: #ecfdf5; -fx-border-color: #a7f3d0; -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 12 20 12 20; -fx-font-weight: 600;");
        }
    }

    @FXML
    private void onToggleLanguageTool(ActionEvent event) {
        if (languageToolAutoCorrectCheckbox != null) {
            boolean enabled = languageToolAutoCorrectCheckbox.isSelected();
            SpellCheckDecorator.setEnabled(enabled);
            showProfileStatus("Correction automatique " + (enabled ? "activée" : "désactivée"), false);
        }
    }

    // ============ AVATAR / PROFILE IMAGE METHODS ============

    @FXML
    private void onAvatarClick(MouseEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une photo de profil");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));

        Stage stage = (Stage) avatarPane.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            try {
                String imgPath = FileStorageService.storeProfileImage(selectedFile);
                UserModel user = App.getCurrentUser();
                user.setProfileImage(imgPath);
                userService.updateProfile(user);

                loadAvatarImage(imgPath);
                showProfileStatus("Photo de profil mise à jour !", false);
            } catch (Exception e) {
                showProfileStatus("Erreur: " + e.getMessage(), true);
                e.printStackTrace();
            }
        }
    }

    private void loadAvatarImage(String imgPath) {
        if (profileAvatarImage == null)
            return;

        if (imgPath != null && !imgPath.isEmpty()) {
            File imgFile = new File(imgPath);
            if (imgFile.exists()) {
                Image image = new Image(imgFile.toURI().toString(), 100, 100, false, true);
                profileAvatarImage.setImage(image);
                profileAvatarImage.setVisible(true);
                Circle clip = new Circle(50, 50, 50);
                profileAvatarImage.setClip(clip);
                profileAvatarInitials.setVisible(false);
                return;
            }
        }
        profileAvatarImage.setVisible(false);
        profileAvatarInitials.setVisible(true);
    }

    // ============ ROLE TOGGLE METHODS ============

    @FXML
    private void onSelectClient(ActionEvent event) {
        try {
            selectedMode = UserRole.CLIENT;
            UserModel user = App.getCurrentUser();
            user.setCurrentMode(UserRole.CLIENT);
            userService.updateProfile(user);
            updateRoleToggleUI();
        } catch (BusinessException e) {
            showProfileStatus(e.getMessage(), true);
        }
    }

    @FXML
    private void onSelectFreelancer(ActionEvent event) {
        try {
            selectedMode = UserRole.FREELANCER;
            UserModel user = App.getCurrentUser();
            user.setCurrentMode(UserRole.FREELANCER);
            userService.updateProfile(user);
            updateRoleToggleUI();
        } catch (BusinessException e) {
            showProfileStatus(e.getMessage(), true);
        }
    }

    private void updateRoleToggleUI() {
        if (roleClientBtn == null || roleFreelancerBtn == null)
            return;

        boolean isFreelancer = UserRole.FREELANCER.equals(selectedMode);
        roleClientBtn.getStyleClass().setAll(isFreelancer ? "role-toggle-btn" : "role-toggle-btn-active");
        roleFreelancerBtn.getStyleClass().setAll(isFreelancer ? "role-toggle-btn-active" : "role-toggle-btn");
        cvSection.setVisible(isFreelancer);
        cvSection.setManaged(isFreelancer);
        if (isFreelancer)
            refreshCVTable();
    }

    @FXML
    private void onUploadCV(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner votre CV (PDF, DOC, DOCX)");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Documents", "*.pdf", "*.doc", "*.docx"));

        Stage stage = (Stage) cvTableView.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            // Validate size
            if (file.length() > 5 * 1024 * 1024) {
                showProfileStatus("Le fichier est trop volumineux (max 5MB).", true);
                return;
            }

            try {
                UserModel user = App.getCurrentUser();

                // Store CV securely with user ID
                String destPath = FileStorageService.storeCV(file, user.getId());

                user.setCvPath(destPath);
                user.setCvUploadedAt(LocalDateTime.now());
                userService.updateProfile(user);

                // Explicitly update global session to ensure all controllers see the change
                App.setCurrentUser(user);

                selectedCvPath = destPath;
                refreshCVTable();
                showProfileStatus("CV uploadé avec succès !", false);
            } catch (Exception e) {
                showProfileStatus("Erreur: " + e.getMessage(), true);
                e.printStackTrace();
            }
        }
    }

    private void ensureRoleAndCvColumns() {
        try (Connection conn = MyDataBase.getConnection();
                Statement stmt = conn.createStatement()) {

            // Check existing columns
            ResultSet rs = conn.getMetaData().getColumns(null, null, "users", null);
            boolean hasCurrentMode = false;
            boolean hasBio = false;
            boolean hasCvPath = false;
            boolean hasCvUploadedAt = false;
            boolean hasRole = false;

            while (rs.next()) {
                String col = rs.getString("COLUMN_NAME");
                if ("current_mode".equalsIgnoreCase(col))
                    hasCurrentMode = true;
                if ("bio".equalsIgnoreCase(col))
                    hasBio = true;
                if ("cv_path".equalsIgnoreCase(col))
                    hasCvPath = true;
                if ("cv_uploaded_at".equalsIgnoreCase(col))
                    hasCvUploadedAt = true;
                if ("role".equalsIgnoreCase(col))
                    hasRole = true;
            }

            if (!hasCurrentMode) {
                stmt.execute(
                        "ALTER TABLE users ADD COLUMN current_mode ENUM('CLIENT', 'FREELANCER') NOT NULL DEFAULT 'CLIENT'");
                if (hasRole) {
                    stmt.execute("UPDATE users SET current_mode = 'CLIENT' WHERE role LIKE '%CLIENT%'");
                    stmt.execute("UPDATE users SET current_mode = 'FREELANCER' WHERE role LIKE '%FREELANCER%'");
                }
            }
            if (!hasBio) {
                stmt.execute("ALTER TABLE users ADD COLUMN bio TEXT");
            }
            if (!hasCvPath) {
                stmt.execute("ALTER TABLE users ADD COLUMN cv_path VARCHAR(255)");
            }
            if (!hasCvUploadedAt) {
                stmt.execute("ALTER TABLE users ADD COLUMN cv_uploaded_at TIMESTAMP NULL DEFAULT NULL");
            }
            // Increase password length if needed (BCrypt needs ~60 chars)
            stmt.execute("ALTER TABLE users MODIFY COLUMN password VARCHAR(255) NOT NULL");

            // Ensure transactions table exists (for Stripe deposits)
            try (ResultSet rsTables = conn.getMetaData().getTables(null, null, "transactions", null)) {
                if (!rsTables.next()) {
                    stmt.execute("CREATE TABLE transactions (" +
                            "id INT AUTO_INCREMENT PRIMARY KEY," +
                            "user_id INT NOT NULL," +
                            "stripe_payment_id VARCHAR(255) NOT NULL," +
                            "amount DOUBLE NOT NULL," +
                            "status VARCHAR(50) NOT NULL," +
                            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                            "INDEX idx_stripe_payment_id (stripe_payment_id)," +
                            "INDEX idx_user_id (user_id)" +
                            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
                }
            }

        } catch (SQLException e) {
            System.err.println("Database migration check failed: " + e.getMessage());
            // We don't block the app, but signup/profile might fail later
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

    // --- Bio Actions ---

    @FXML
    private void onEditBio() {
        profileBioLabel.setVisible(false);
        profileBioLabel.setManaged(false);
        editBioBtn.setVisible(false);
        editBioBtn.setManaged(false);
        bioEditBox.setVisible(true);
        bioEditBox.setManaged(true);
        profileBioField.requestFocus();
    }

    @FXML
    private void onCancelEditBio() {
        profileBioField.setText(profileBioLabel.getText().equals("No bio provided.") ? "" : profileBioLabel.getText());
        closeBioEdit();
    }

    @FXML
    private void onSaveBio() {
        String newVal = ValidationUtils.autoCorrect(profileBioField.getText());
        profileBioField.setText(newVal);
        // Bio can be empty (user clears it) — only auto-correct, no min-length required
        try {
            UserModel user = App.getCurrentUser();
            user.setBio(newVal);
            userService.updateProfile(user);

            profileBioLabel.setText(newVal.isEmpty() ? "No bio provided." : newVal);
            closeBioEdit();
            showProfileStatus("Bio mise à jour !", false);
        } catch (BusinessException e) {
            showProfileStatus(e.getMessage(), true);
        }
    }

    private void closeBioEdit() {
        profileBioLabel.setVisible(true);
        profileBioLabel.setManaged(true);
        editBioBtn.setVisible(true);
        editBioBtn.setManaged(true);
        bioEditBox.setVisible(false);
        bioEditBox.setManaged(false);
    }

    // --- Password Confirmation Loader ---

    private boolean showPasswordConfirmation() {
        return com.khademni.utils.PasswordConfirmationUtil.showConfirmation();
    }

    // ============ DEPOSIT (STRIPE) ============

    @FXML
    private void onDeposit(ActionEvent event) {
        UserModel user = App.getCurrentUser();
        if (user == null) {
            showProfileStatus("Vous devez être connecté pour déposer des fonds.", true);
            return;
        }

        TextInputDialog dialog = new TextInputDialog("10");
        dialog.setTitle("Dépôt sur le portefeuille");
        dialog.setHeaderText("Montant à déposer (€)");
        dialog.setContentText("Montant en euros (€) — le paiement Stripe sera en euros :");
        dialog.getEditor().setPromptText("Ex: 10");

        String amountStr = dialog.showAndWait().orElse(null);
        if (amountStr == null || amountStr.isBlank()) {
            return;
        }

        double amountEur;
        try {
            amountEur = Double.parseDouble(amountStr.trim().replace(",", "."));
        } catch (NumberFormatException e) {
            showProfileStatus("Montant invalide. Saisissez un nombre (ex: 10 ou 25.5).", true);
            return;
        }

        if (amountEur <= 0) {
            showProfileStatus("Le montant doit être strictement positif.", true);
            return;
        }

        Stage progressStage = new Stage();
        ProgressIndicator pi = new ProgressIndicator();
        Scene ps = new Scene(pi, 120, 120);
        progressStage.initModality(Modality.APPLICATION_MODAL);
        progressStage.setScene(ps);
        progressStage.setTitle("Préparation du paiement...");
        progressStage.show();

        final double amountFinal = amountEur;
        Task<String> createSessionTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                return stripeService.createDepositCheckoutSession(amountFinal, user.getId());
            }
        };

        createSessionTask.setOnSucceeded(evt -> {
            progressStage.close();
            String paymentUrl = createSessionTask.getValue();
            if (paymentUrl == null || paymentUrl.isBlank()) {
                showProfileStatus("Impossible de générer le lien de paiement.", true);
                return;
            }

            Stage webStage = new Stage();
            webStage.initModality(Modality.APPLICATION_MODAL);
            webStage.setTitle("Paiement Stripe - Dépôt");

            WebView webView = new WebView();
            WebEngine engine = webView.getEngine();
            engine.load(paymentUrl);

            engine.locationProperty().addListener((obs, oldLoc, newLoc) -> {
                if (newLoc == null)
                    return;
                String lower = newLoc.toLowerCase();
                if (lower.contains("deposit-success") && lower.contains("session_id")) {
                    String sessionId = extractQueryParam(newLoc, "session_id");
                    if (sessionId == null || sessionId.isBlank()) {
                        Platform.runLater(() -> {
                            showProfileStatus("Session de paiement introuvable.", true);
                            webStage.close();
                        });
                        return;
                    }

                    Stage verifyStage = new Stage();
                    ProgressIndicator verifyPi = new ProgressIndicator();
                    Scene verifyScene = new Scene(verifyPi, 120, 120);
                    verifyStage.initOwner(webStage);
                    verifyStage.initModality(Modality.APPLICATION_MODAL);
                    verifyStage.setScene(verifyScene);
                    verifyStage.setTitle("Vérification du paiement...");
                    verifyStage.show();

                    Task<DepositVerificationResult> verifyTask = new Task<>() {
                        @Override
                        protected DepositVerificationResult call() throws Exception {
                            return stripeService.retrieveSessionForDeposit(sessionId);
                        }
                    };

                    verifyTask.setOnSucceeded(v -> {
                        verifyStage.close();
                        DepositVerificationResult result = verifyTask.getValue();
                        if (result != null && result.isSucceeded()) {
                            try {
                                userService.deposit(result.getUserId(), result.getAmountDt(),
                                        result.getStripePaymentId());
                                java.util.Optional<UserModel> updated = userService.getUserById(user.getId());
                                updated.ifPresent(App::setCurrentUser);
                                if (profileBalanceLabel != null && updated.isPresent()) {
                                    profileBalanceLabel.setText(String.format("%.2f €", updated.get().getBalance()));
                                }
                                showProfileStatus("Dépôt réussi ! " + String.format("%.2f", result.getAmountDt())
                                        + " € ont été ajoutés à votre solde.", false);
                            } catch (BusinessException e) {
                                showProfileStatus(e.getMessage(), true);
                            }
                        } else {
                            String msg = (result != null && result.getErrorMessage() != null) ? result.getErrorMessage()
                                    : "Paiement non confirmé.";
                            showProfileStatus("Échec du dépôt: " + msg, true);
                        }
                        webStage.close();
                    });

                    verifyTask.setOnFailed(v -> {
                        verifyStage.close();
                        Throwable ex = verifyTask.getException();
                        showProfileStatus("Erreur lors de la vérification: " + (ex != null ? ex.getMessage() : ""),
                                true);
                        webStage.close();
                    });

                    new Thread(verifyTask, "stripe-deposit-verify-thread").start();
                } else if (lower.contains("deposit-cancel")) {
                    Platform.runLater(() -> {
                        showProfileStatus("Paiement annulé.", true);
                        webStage.close();
                    });
                }
            });

            Scene scene = new Scene(webView, 900, 700);
            webStage.setScene(scene);
            webStage.show();
        });

        createSessionTask.setOnFailed(evt -> {
            progressStage.close();
            Throwable ex = createSessionTask.getException();
            String msg = ex != null ? ex.getMessage() : "Erreur inconnue";
            if (msg.contains("card") || msg.contains("declined")) {
                showProfileStatus("Carte refusée. Vérifiez vos informations ou utilisez une autre carte.", true);
            } else {
                showProfileStatus("Erreur Stripe: " + msg, true);
            }
        });

        new Thread(createSessionTask, "stripe-deposit-session-thread").start();
    }

    private String extractQueryParam(String url, String name) {
        try {
            URI uri = new URI(url);
            String query = uri.getQuery();
            if (query == null)
                return null;
            for (String p : query.split("&")) {
                int idx = p.indexOf('=');
                if (idx > 0) {
                    String key = URLDecoder.decode(p.substring(0, idx), StandardCharsets.UTF_8);
                    if (name.equals(key)) {
                        return URLDecoder.decode(p.substring(idx + 1), StandardCharsets.UTF_8);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

}
