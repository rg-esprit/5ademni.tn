package com.khademni.controller;

import com.khademni.App;
import com.khademni.model.JobApplicationModel;
import com.khademni.model.JobModel;
import com.khademni.utils.AIService;
import com.khademni.utils.MyDataBase;
import com.khademni.utils.SMSService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import javafx.geometry.Pos;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.util.Duration;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class JobApplicationController {

    // Common UI
    @FXML
    private Label jobTitleLabel;
    @FXML
    private Label applicationsCountLabel;

    // Admin/Owner UI
    @FXML
    private ScrollPane adminScrollPane;
    @FXML
    private VBox adminCardsContainer;

    @FXML
    private ComboBox<String> statusFilterCombo;
    @FXML
    private HBox adminButtonsBox; // Parent container of buttons (in bottom)

    // Freelancer UI
    @FXML
    private ScrollPane freelancerView;
    @FXML
    private Label jobDescriptionLabel;
    @FXML
    private Label totalApplicantsLabel;
    @FXML
    private VBox applicationFormContainer;

    private JobModel selectedJob;
    private List<JobApplicationModel> allApplications = new ArrayList<>();

    public void setSelectedJob(JobModel job) {
        this.selectedJob = job;
        jobTitleLabel.setText(job.getTitle() + " at " + job.getCompany());
        initializeView();
    }

    @FXML
    public void initialize() {
        // Default init
    }

    private void initializeView() {
        boolean isOwnerOrAdmin = App.currentUser != null &&
                (App.currentUser.isIsAdmin() || App.currentUser.getId() == 1
                        || App.currentUser.getId() == selectedJob.getUserId());

        if (isOwnerOrAdmin) {
            setupAdminView();
        } else {
            setupFreelancerView();
        }
    }

    // ==================== ADMIN / OWNER VIEW ====================

    private void setupAdminView() {
        adminScrollPane.setVisible(true);
        freelancerView.setVisible(false);

        setupFiltersAndButtons();
        loadApplicationsForJob();
    }

    private void setupFiltersAndButtons() {
        statusFilterCombo.setItems(FXCollections.observableArrayList("All", "PENDING", "ACCEPTED", "REJECTED"));
        statusFilterCombo.setValue("All");
        statusFilterCombo.setOnAction(e -> renderApplications(filterApplications(statusFilterCombo.getValue())));
    }

    private void loadApplicationsForJob() {
        allApplications.clear();
        try (Connection conn = MyDataBase.getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "SELECT ja.id, ja.job_id, ja.title, ja.description, ja.cv_path, ja.status, ja.application_date, "
                                +
                                "ja.phone_number, u.first_name, u.last_name, u.email AS applicant_email " +
                                "FROM job_applications ja LEFT JOIN users u ON ja.user_id = u.id WHERE ja.job_id = ? ORDER BY ja.application_date DESC, ja.id DESC")) {
            stmt.setInt(1, selectedJob.getId());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                allApplications.add(mapResultSetToApplication(rs));
            }
        } catch (SQLException e) {
            showAlert("Error", "Failed to load applications: " + e.getMessage());
        }

        renderApplications(allApplications);
        updateCounts(allApplications.size());
    }

    private void renderApplications(List<JobApplicationModel> apps) {
        adminCardsContainer.getChildren().clear();

        if (apps.isEmpty()) {
            Label placeholder = new Label("No applications found.");
            placeholder.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 14; -fx-padding: 20;");
            adminCardsContainer.getChildren().add(placeholder);
            return;
        }

        int delay = 0;
        for (JobApplicationModel app : apps) {
            HBox card = createApplicationCard(app);
            adminCardsContainer.getChildren().add(card);

            // Staggered Animation
            FadeTransition ft = new FadeTransition(Duration.millis(400), card);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.setDelay(Duration.millis(delay));
            ft.play();

            javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(Duration.millis(400),
                    card);
            tt.setFromY(20);
            tt.setToY(0);
            tt.setDelay(Duration.millis(delay));
            tt.play();

            delay += 100;
        }

        applicationsCountLabel.setText("Showing " + apps.size() + " applications");
    }

    private HBox createApplicationCard(JobApplicationModel app) {
        HBox card = new HBox(20);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("app-card");

        // 1. Avatar (Initials)
        String initials = getInitials(app.getApplicantName());
        javafx.scene.shape.Circle avatarCircle = new javafx.scene.shape.Circle(24);
        avatarCircle.getStyleClass().add("avatar-circle");

        Label initialsLabel = new Label(initials);
        initialsLabel.getStyleClass().add("avatar-text");

        javafx.scene.layout.StackPane avatarPane = new javafx.scene.layout.StackPane(avatarCircle, initialsLabel);

        // 2. Main Info
        VBox infoBox = new VBox(4);
        HBox.setHgrow(infoBox, javafx.scene.layout.Priority.ALWAYS);

        Label name = new Label(app.getApplicantName());
        name.getStyleClass().add("applicant-name");

        Label title = new Label(app.getTitle());
        title.getStyleClass().add("app-title");

        Label email = new Label("📧 " + app.getApplicantEmail() + " • " + formatRelativeDate(app.getAppliedDate()));
        email.getStyleClass().add("applicant-email");

        Label descSnippet = new Label(app.getDescription().length() > 60 ? app.getDescription().substring(0, 57) + "..."
                : app.getDescription());
        descSnippet.getStyleClass().add("app-desc-snippet");
        descSnippet.setWrapText(true);

        infoBox.getChildren().addAll(name, title, email, descSnippet);

        // 3. Status Badge
        Label statusBadge = new Label(app.getStatus());
        statusBadge.getStyleClass().addAll("status-badge", "status-" + app.getStatus().toLowerCase());

        // 4. Actions
        VBox actionsBox = new VBox(8);
        actionsBox.setAlignment(Pos.CENTER_RIGHT);

        Button cvBtn = new Button("📄 View CV");
        cvBtn.getStyleClass().addAll("action-btn", "btn-cv");
        cvBtn.setOnAction(e -> viewCv(app));

        HBox decisionBtns = new HBox(8);
        if ("PENDING".equalsIgnoreCase(app.getStatus())) {
            Button acceptBtn = new Button("Accept");
            acceptBtn.getStyleClass().addAll("action-btn", "btn-accept");
            acceptBtn.setOnAction(e -> updateStatus(app, "ACCEPTED"));

            Button rejectBtn = new Button("Reject");
            rejectBtn.getStyleClass().addAll("action-btn", "btn-reject");
            rejectBtn.setOnAction(e -> updateStatus(app, "REJECTED"));

            decisionBtns.getChildren().addAll(acceptBtn, rejectBtn);
        }

        actionsBox.getChildren().addAll(statusBadge, cvBtn, decisionBtns);

        card.getChildren().addAll(avatarPane, infoBox, actionsBox);
        return card;
    }

    private void updateStatus(JobApplicationModel app, String newStatus) {
        try (Connection conn = MyDataBase.getConnection();
                PreparedStatement stmt = conn.prepareStatement("UPDATE job_applications SET status=? WHERE id=?")) {
            stmt.setString(1, newStatus);
            stmt.setInt(2, app.getId());
            stmt.executeUpdate();

            // Refresh local list and re-render
            app.setStatus(newStatus);
            renderApplications(filterApplications(statusFilterCombo.getValue()));

            // Send SMS notification if application was ACCEPTED or REJECTED
            if ("ACCEPTED".equalsIgnoreCase(newStatus) || "REJECTED".equalsIgnoreCase(newStatus)) {
                String phone = app.getPhoneNumber();
                if (phone != null && !phone.trim().isEmpty()) {
                    String jobTitle = selectedJob != null ? selectedJob.getTitle() : "the position";
                    String smsBody;
                    if ("ACCEPTED".equalsIgnoreCase(newStatus)) {
                        smsBody = "Félicitations " + app.getApplicantName() + "! "
                                + "Votre candidature pour '" + jobTitle + "' a été acceptée. "
                                + "Bienvenue dans l'équipe! - Khademni.tn";
                    } else {
                        smsBody = "Bonjour " + app.getApplicantName() + ", "
                                + "Malheureusement, votre candidature pour '" + jobTitle + "' n'a pas été retenue. "
                                + "Nous vous souhaitons une bonne continuation. - Khademni.tn";
                    }
                    new Thread(() -> SMSService.sendSms(phone, smsBody)).start();
                }
            }

        } catch (SQLException e) {
            showAlert("Error", e.getMessage());
        }
    }

    private List<JobApplicationModel> filterApplications(String status) {
        if ("All".equals(status) || status == null)
            return allApplications;
        List<JobApplicationModel> filtered = new ArrayList<>();
        for (JobApplicationModel app : allApplications) {
            if (app.getStatus().equalsIgnoreCase(status))
                filtered.add(app);
        }
        return filtered;
    }

    private String getInitials(String name) {
        if (name == null || name.isEmpty())
            return "?";
        String[] parts = name.split(" ");
        if (parts.length >= 2)
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
        return name.substring(0, Math.min(2, name.length())).toUpperCase();
    }

    private void updateCounts(int size) {
        applicationsCountLabel.setText("Total applications: " + size);
        totalApplicantsLabel.setText(String.valueOf(size));
    }

    /**
     * Opens/views the CV (file or URL) for an application
     * Only accessible by job poster and the applicant
     */
    private void viewCv(JobApplicationModel app) {
        String cvPath = app.getCvUrl();

        if (cvPath == null || cvPath.isEmpty()) {
            showAlert("No CV", "No CV file or URL provided for this application.");
            return;
        }

        // Check if it's a URL or file path
        try {
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                if (cvPath.startsWith("http://") || cvPath.startsWith("https://")) {
                    // It's a URL - open in default browser
                    desktop.browse(new java.net.URI(cvPath));
                } else {
                    // It's a file path - open with default application
                    java.io.File file = new java.io.File(cvPath);
                    if (!file.exists()) {
                        showAlert("File Not Found", "The CV file could not be found at: " + cvPath);
                        return;
                    }
                    desktop.open(file);
                }
            } else {
                showAlert("Error", "Desktop operations are not supported on this system.");
            }
        } catch (Exception e) {
            showAlert("Error", "Could not open CV: " + e.getMessage());
        }
    }

    private JobApplicationModel mapResultSetToApplication(ResultSet rs) throws SQLException {
        java.sql.Timestamp ts = rs.getTimestamp("application_date");
        LocalDateTime applied = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();

        String firstName = rs.getString("first_name");
        String lastName = rs.getString("last_name");
        String fullName = (firstName != null ? firstName : "Unknown") + " " + (lastName != null ? lastName : "User");
        String email = rs.getString("applicant_email");
        if (email == null)
            email = "No Email Provided";

        String phone = null;
        try {
            phone = rs.getString("phone_number");
        } catch (SQLException ignored) {
        }

        return new JobApplicationModel(
                rs.getInt("id"),
                rs.getInt("job_id"),
                fullName.trim(),
                email,
                rs.getString("title"),
                rs.getString("description"),
                rs.getString("cv_path"),
                rs.getString("status"),
                applied,
                phone);
    }

    private String formatRelativeDate(LocalDateTime dateTime) {
        if (dateTime == null)
            return "unknown";

        LocalDateTime now = LocalDateTime.now();
        long years = ChronoUnit.YEARS.between(dateTime, now);
        if (years > 0)
            return years + (years == 1 ? " year ago" : " years ago");

        long months = ChronoUnit.MONTHS.between(dateTime, now);
        if (months > 0)
            return months + (months == 1 ? " month ago" : " months ago");

        long days = ChronoUnit.DAYS.between(dateTime, now);
        if (days > 0)
            return days + (days == 1 ? " day ago" : " days ago");

        long hours = ChronoUnit.HOURS.between(dateTime, now);
        if (hours > 0)
            return hours + (hours == 1 ? " hour ago" : " hours ago");

        long minutes = ChronoUnit.MINUTES.between(dateTime, now);
        if (minutes > 0)
            return minutes + (minutes == 1 ? " minute ago" : " minutes ago");

        return "Just now";
    }

    // This method is no longer needed - filtering is now handled by
    // renderApplications()

    // ==================== FREELANCER VIEW ====================

    private void setupFreelancerView() {
        adminScrollPane.setVisible(false);
        freelancerView.setVisible(true);
        if (adminButtonsBox != null)
            adminButtonsBox.setVisible(false); // Hide admin buttons

        // Hide top admin controls
        statusFilterCombo.setVisible(false);

        // Rebuild the content to show rich job details
        VBox container = (VBox) freelancerView.getContent();
        container.getChildren().clear();

        container.getChildren().add(buildJobDetailsView());
        container.getChildren().add(applicationFormContainer);

        // Check for existing application
        loadFreelancerApplicationState();
    }

    private javafx.scene.Node buildJobDetailsView() {
        VBox root = new VBox(15);
        root.setStyle("-fx-padding: 0 0 20 0; -fx-border-color: #f0f0f0; -fx-border-width: 0 0 1 0;");

        // Header: Job Title
        Label title = new Label(selectedJob.getTitle());
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #141118;");

        // Company & Location Row
        HBox metaRow = new HBox(15);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        String posterName = getJobPosterName(selectedJob.getUserId());
        Label company = new Label(
                "🏢 " + (selectedJob.getCompany() != null ? selectedJob.getCompany() : "Company") + " (" + posterName
                        + ")");
        company.setStyle("-fx-text-fill: #6b7280; -fx-font-weight: 600; -fx-font-size: 13px;");

        Label location = new Label("📍 " + selectedJob.getLocation());
        location.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 13px;");

        Label type = new Label("💼 " + selectedJob.getJobType());
        type.setStyle(
                "-fx-background-color: #f3e8ff; -fx-text-fill: #6c0df2; -fx-padding: 4 8; -fx-background-radius: 4; -fx-font-size: 11px; -fx-font-weight: bold;");

        Label category = new Label("🏷️ " + selectedJob.getCategory());
        category.setStyle(
                "-fx-background-color: #e0f2fe; -fx-text-fill: #0284c7; -fx-padding: 4 8; -fx-background-radius: 4; -fx-font-size: 11px; -fx-font-weight: bold;");

        metaRow.getChildren().addAll(company, location, type, category);

        // Salary
        Label salary = new Label("💰 " + selectedJob.getSalaryRange());
        salary.setStyle("-fx-text-fill: #059669; -fx-font-weight: bold; -fx-font-size: 14px;");

        // Description
        Label descHeader = new Label("About the role");
        descHeader
                .setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1f2937; -fx-padding: 10 0 5 0;");

        Label desc = new Label(selectedJob.getDescription());
        desc.setWrapText(true);
        desc.setStyle("-fx-font-size: 14px; -fx-text-fill: #4b5563; -fx-line-spacing: 5;");

        // Applicants count
        HBox statsRow = new HBox(8);
        statsRow.setAlignment(Pos.CENTER_LEFT);
        statsRow.setStyle("-fx-padding: 15 0 0 0;");
        Label applicantsLabel = new Label("👥 Applicants:");
        applicantsLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #6b7280;");

        // We need to keep a reference to update this label
        totalApplicantsLabel = new Label("0");
        totalApplicantsLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #6c0df2;");
        // Count immediately
        countTotalApplicants();

        statsRow.getChildren().addAll(applicantsLabel, totalApplicantsLabel);

        root.getChildren().addAll(title, metaRow, salary, descHeader, desc, statsRow);
        return root;
    }

    private void countTotalApplicants() {
        try (Connection conn = MyDataBase.getConnection();
                PreparedStatement stmt = conn
                        .prepareStatement("SELECT COUNT(*) FROM job_applications WHERE job_id=?")) {
            stmt.setInt(1, selectedJob.getId());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int count = rs.getInt(1);
                applicationsCountLabel.setText("Total applications: " + count);
                totalApplicantsLabel.setText(String.valueOf(count));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadFreelancerApplicationState() {
        applicationFormContainer.getChildren().clear();

        try (Connection conn = MyDataBase.getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "SELECT ja.id, ja.job_id, ja.title, ja.description, ja.cv_path, ja.status, ja.application_date, "
                                +
                                "ja.phone_number, u.first_name, u.last_name, u.email AS applicant_email " +
                                "FROM job_applications ja LEFT JOIN users u ON ja.user_id = u.id " +
                                "WHERE ja.job_id = ? AND ja.user_id = ?")) {

            stmt.setInt(1, selectedJob.getId());
            stmt.setInt(2, App.currentUser.getId());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                // Application exists
                JobApplicationModel app = mapResultSetToApplication(rs);
                showApplicationDetails(app);
            } else {
                // No application -> Show Apply Button
                showApplyButton();
            }

        } catch (SQLException e) {
            showAlert("Error", "Failed to check application status: " + e.getMessage());
        }
    }

    private void showApplicationDetails(JobApplicationModel app) {
        VBox detailsBox = new VBox(15);
        detailsBox.setStyle(
                "-fx-padding: 20; -fx-background-color: #f9f9f9; -fx-border-radius: 8; -fx-background-radius: 8;");

        Label statusLabel = new Label("Status: " + app.getStatus());
        statusLabel
                .setStyle("-fx-font-weight: bold; -fx-padding: 5 10; -fx-text-fill: white; -fx-background-radius: 4; " +
                        (app.getStatus().equalsIgnoreCase("ACCEPTED") ? "-fx-background-color: #10b981;"
                                : app.getStatus().equalsIgnoreCase("REJECTED") ? "-fx-background-color: #ef4444;"
                                        : "-fx-background-color: #f59e0b;"));

        Label dateLabel = new Label("📅 Applied: " + formatRelativeDate(app.getAppliedDate()));
        dateLabel.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 13px;");

        Label title = new Label(app.getTitle());
        title.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");

        Label desc = new Label(app.getDescription());
        desc.setWrapText(true);

        Label cv = new Label("CV: " + app.getCvUrl());
        cv.setStyle("-fx-text-fill: #6c0df2; -fx-underline: true; -fx-cursor: hand;");

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);

        if ("PENDING".equalsIgnoreCase(app.getStatus())) {
            Button editBtn = new Button("✏️ Edit");
            editBtn.setStyle("-fx-background-color: #8b5cf6; -fx-text-fill: white; -fx-cursor: hand;");
            editBtn.setOnAction(e -> showApplicationForm(app));

            Button deleteBtn = new Button("🗑️ Delete");
            deleteBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-cursor: hand;");
            deleteBtn.setOnAction(e -> deleteMyApplication(app));

            actions.getChildren().addAll(editBtn, deleteBtn);
        } else {
            Label info = new Label("Application " + app.getStatus().toLowerCase() + ". Modification disabled.");
            info.setStyle("-fx-text-fill: #6b7280; -fx-font-style: italic;");
            actions.getChildren().add(info);
        }

        detailsBox.getChildren().addAll(statusLabel, title, desc, dateLabel, cv, new Separator(), actions);
        applicationFormContainer.getChildren().add(detailsBox);
    }

    private void showApplyButton() {
        applicationFormContainer.getChildren().clear();

        Button applyBtn = new Button("✨ Apply Now");
        applyBtn.setStyle(
                "-fx-background-color: #6c0df2; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 12 30; -fx-background-radius: 30; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(108, 13, 242, 0.3), 10, 0, 0, 4);");

        applyBtn.setOnMouseEntered(e -> {
            applyBtn.setStyle(
                    "-fx-background-color: #5b0bc9; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 12 30; -fx-background-radius: 30; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(108, 13, 242, 0.4), 15, 0, 0, 6);");
            ScaleTransition st = new ScaleTransition(Duration.millis(200), applyBtn);
            st.setToX(1.1);
            st.setToY(1.1);
            st.play();
        });

        applyBtn.setOnMouseExited(e -> {
            applyBtn.setStyle(
                    "-fx-background-color: #6c0df2; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 12 30; -fx-background-radius: 30; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(108, 13, 242, 0.3), 10, 0, 0, 4);");
            ScaleTransition st = new ScaleTransition(Duration.millis(200), applyBtn);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });

        applyBtn.setOnAction(e -> {
            try {
                showApplicationForm(null);
            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert("Error", "Unable to open application form: " + ex.getMessage());
            }
        });

        HBox container = new HBox(applyBtn);
        container.setAlignment(Pos.CENTER);
        container.setStyle("-fx-padding: 20 0;");

        applicationFormContainer.getChildren().add(container);

        // Fade in
        FadeTransition ft = new FadeTransition(Duration.millis(500), container);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    private void showApplicationForm(JobApplicationModel existingApp) {
        try {
            applicationFormContainer.getChildren().clear();

            VBox form = new VBox(12);
            form.setStyle("-fx-padding: 10;");

            Label headerLabel = new Label(existingApp == null ? "✨ Apply for this Job" : "✏️ Edit Application");
            headerLabel.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #141118;");

            TextField titleField = new TextField(existingApp != null ? existingApp.getTitle() : "");
            titleField.setPromptText("Application Title");
            titleField.setStyle("-fx-padding: 10; -fx-border-color: #e5e7eb; -fx-border-radius: 6;");

            TextArea descArea = new TextArea(existingApp != null ? existingApp.getDescription() : "");
            descArea.setPromptText("Write your motivation here...");
            descArea.setPrefRowCount(4);
            descArea.setWrapText(true);
            descArea.setStyle(
                    "-fx-control-inner-background: #fafafa; -fx-border-color: #e5e7eb; -fx-border-radius: 6;");

            HBox descLabelRow = new HBox(10);
            descLabelRow.setAlignment(Pos.CENTER_LEFT);
            Label motivationLabel = new Label("Why are you a good fit?");
            motivationLabel.setStyle("-fx-font-weight: bold;");

            // Create context for AI
            java.util.Map<String, String> context = new java.util.HashMap<>();
            context.put("contextType", "Job Application Motivation");
            if (selectedJob != null) {
                context.put("Job Title", selectedJob.getTitle());
                context.put("Company", selectedJob.getCompany());
                context.put("Category", selectedJob.getCategory());
            }

            Button aiBtn = createAIButton(descArea, context);
            descLabelRow.getChildren().addAll(motivationLabel, aiBtn);

            // ---- Phone Number Field (+216 Tunisian) ----
            Label phoneLabel = new Label("📱 Phone Number (for SMS notifications)");
            phoneLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

            Label prefixLabel = new Label("+216");
            prefixLabel.setStyle(
                    "-fx-font-weight: bold; -fx-text-fill: white; -fx-background-color: #6c0df2; "
                            + "-fx-padding: 9 10; -fx-background-radius: 6 0 0 6;");

            // Extract digits only from existing phone (strip +216)
            String existingDigits = "";
            if (existingApp != null && existingApp.getPhoneNumber() != null) {
                existingDigits = existingApp.getPhoneNumber().replace("+216", "").trim();
            }
            TextField phoneDigitsField = new TextField(existingDigits);
            phoneDigitsField.setPromptText("XXXXXXXX (8 digits)");
            phoneDigitsField.setStyle(
                    "-fx-padding: 9; -fx-border-color: #e5e7eb; -fx-border-radius: 0 6 6 0; -fx-background-radius: 0 6 6 0;");
            // Only allow digits, max 8
            phoneDigitsField.setTextFormatter(new TextFormatter<>(change -> {
                String newText = change.getControlNewText();
                if (newText.matches("\\d{0,8}"))
                    return change;
                return null;
            }));

            HBox phoneRow = new HBox(0, prefixLabel, phoneDigitsField);
            phoneRow.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(phoneDigitsField, javafx.scene.layout.Priority.ALWAYS);

            // CV Section - Dual Option: File Upload OR URL
            Label cvSectionLabel = new Label("CV / Resume (Choose one option)");
            cvSectionLabel.setStyle("-fx-font-weight: 700; -fx-font-size: 13px; -fx-text-fill: #374151;");

            // Option 1: File Upload
            Label cvFileLabel = new Label("No file selected");
            cvFileLabel.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 12px; -fx-padding: 5 0;");
            cvFileLabel.setWrapText(true);

            Button uploadCvBtn = new Button("📄 Upload CV File");
            uploadCvBtn.setStyle(
                    "-fx-background-color: #e0f2fe; -fx-text-fill: #0284c7; -fx-font-weight: 600; -fx-padding: 10 20; -fx-background-radius: 6; -fx-cursor: hand;");

            final String[] selectedCvPath = { "" };

            // Option 2: URL Input
            TextField cvUrlField = new TextField();
            cvUrlField.setPromptText("Or paste CV URL (Google Drive, LinkedIn, etc.)");
            cvUrlField.setStyle("-fx-padding: 10; -fx-border-color: #e5e7eb; -fx-border-radius: 6;");

            // Initialize from existing app
            if (existingApp != null && existingApp.getCvUrl() != null && !existingApp.getCvUrl().isEmpty()) {
                if (existingApp.getCvUrl().startsWith("http://") || existingApp.getCvUrl().startsWith("https://")) {
                    cvUrlField.setText(existingApp.getCvUrl());
                } else {
                    selectedCvPath[0] = existingApp.getCvUrl();
                    cvFileLabel.setText(new java.io.File(existingApp.getCvUrl()).getName());
                    cvFileLabel.setStyle(
                            "-fx-text-fill: #10b981; -fx-font-size: 12px; -fx-padding: 5 0; -fx-font-weight: 600;");
                }
            }

            uploadCvBtn.setOnAction(e -> {
                javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
                fileChooser.setTitle("Select CV File");
                fileChooser.getExtensionFilters().addAll(
                        new javafx.stage.FileChooser.ExtensionFilter("PDF Files", "*.pdf"),
                        new javafx.stage.FileChooser.ExtensionFilter("Word Documents", "*.doc", "*.docx"),
                        new javafx.stage.FileChooser.ExtensionFilter("Text Files", "*.txt"));

                java.io.File file = fileChooser.showOpenDialog(uploadCvBtn.getScene().getWindow());
                if (file != null) {
                    // Security validation
                    String validationError = validateCvFile(file);
                    if (validationError != null) {
                        showAlert("Security Error", validationError);
                        return;
                    }

                    selectedCvPath[0] = file.getAbsolutePath();
                    cvFileLabel.setText("✓ " + file.getName() + " (" + formatFileSize(file.length()) + ")");
                    cvFileLabel.setStyle(
                            "-fx-text-fill: #10b981; -fx-font-size: 12px; -fx-padding: 5 0; -fx-font-weight: 600;");
                    cvUrlField.clear(); // Clear URL if file is selected
                }
            });

            // Clear file selection when URL is entered
            cvUrlField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && !newVal.trim().isEmpty()) {
                    selectedCvPath[0] = "";
                    cvFileLabel.setText("No file selected");
                    cvFileLabel.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 12px; -fx-padding: 5 0;");
                }
            });

            VBox cvFileBox = new VBox(5, uploadCvBtn, cvFileLabel);
            cvFileBox.setStyle("-fx-padding: 5 0;");

            Label orLabel = new Label("OR");
            orLabel.setStyle("-fx-text-fill: #9ca3af; -fx-font-weight: 600; -fx-padding: 5 0;");

            Label errorLabel = new Label();
            errorLabel.setStyle("-fx-text-fill: #ef4444;");

            Button submitBtn = new Button(existingApp == null ? "Submit Application" : "Save Changes");
            submitBtn.setStyle(
                    "-fx-background-color: #6c0df2; -fx-text-fill: white; -fx-font-weight: 600; -fx-padding: 10 20; -fx-background-radius: 6; -fx-cursor: hand;");

            Button cancelBtn = new Button("Cancel");
            cancelBtn.setStyle(
                    "-fx-background-color: #f3f4f6; -fx-text-fill: #374151; -fx-padding: 10 20; -fx-background-radius: 6; -fx-cursor: hand;");
            cancelBtn.setOnAction(e -> loadFreelancerApplicationState());

            HBox btns = new HBox(10);
            btns.getChildren().addAll(submitBtn);
            if (existingApp != null)
                btns.getChildren().add(cancelBtn);

            submitBtn.setOnAction(e -> {
                String t = titleField.getText().trim();
                String d = descArea.getText().trim();
                String cvFile = selectedCvPath[0];
                String cvUrl = cvUrlField.getText().trim();
                String phoneDigits = phoneDigitsField.getText().trim();

                if (t.length() < 3 || d.length() < 10) {
                    errorLabel.setText("Title (min 3 chars) and Motivation (min 10 chars) are required.");
                    return;
                }

                // Validate phone number
                if (phoneDigits.isEmpty()) {
                    errorLabel.setText("Please enter your phone number (8 digits).");
                    return;
                }
                if (phoneDigits.length() != 8) {
                    errorLabel.setText("Phone number must be exactly 8 digits (e.g. 98765432).");
                    return;
                }
                String fullPhone = "+216" + phoneDigits;

                // Validate that at least one CV option is provided
                if ((cvFile == null || cvFile.isEmpty()) && (cvUrl == null || cvUrl.isEmpty())) {
                    errorLabel.setText("Please upload a CV file OR provide a CV URL.");
                    return;
                }

                // Validate URL if provided
                if (cvUrl != null && !cvUrl.isEmpty()) {
                    String urlValidationError = validateCvUrl(cvUrl);
                    if (urlValidationError != null) {
                        errorLabel.setText(urlValidationError);
                        return;
                    }
                }

                // Use file path if available, otherwise use URL
                String finalCvPath = (cvFile != null && !cvFile.isEmpty()) ? cvFile : cvUrl;

                if (existingApp == null) {
                    saveApplication(t, d, finalCvPath, fullPhone);
                } else {
                    updateApplication(existingApp.getId(), t, d, finalCvPath, fullPhone);
                }
            });

            form.getChildren().addAll(headerLabel,
                    new Label("Title"), titleField,
                    descLabelRow, descArea,
                    phoneLabel, phoneRow,
                    cvSectionLabel, cvFileBox, orLabel, cvUrlField,
                    errorLabel, btns);
            applicationFormContainer.getChildren().add(form);

            // Fade in
            FadeTransition ft = new FadeTransition(Duration.millis(400), form);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();
        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert("Error", "Failed to build application form: " + ex.getMessage());
        }
    }

    private void saveApplication(String title, String desc, String cv, String phone) {
        if (App.currentUser == null) {
            showAlert("Error", "You must be logged in to submit an application");
            return;
        }
        if (selectedJob == null) {
            showAlert("Error", "No job selected");
            return;
        }
        try (Connection conn = MyDataBase.getConnection()) {
            String sql = "INSERT INTO job_applications(job_id, user_id, title, description, cv_path, status, application_date, phone_number) VALUES(?,?,?,?,?,'PENDING',?,?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, selectedJob.getId());
                stmt.setInt(2, App.currentUser.getId());
                stmt.setString(3, title);
                stmt.setString(4, desc);
                stmt.setString(5, cv);
                stmt.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
                stmt.setString(7, phone);

                stmt.executeUpdate();
            }
            showAlert("Success", "Application submitted!");
            loadFreelancerApplicationState(); // Refresh
            countTotalApplicants();

        } catch (Throwable ex) {
            System.err.println("ERROR in saveApplication: " + ex.getMessage());
            ex.printStackTrace();
            showAlert("Error", "Failed to submit: " + ex.getMessage());
        }
    }

    private void updateApplication(int appId, String title, String desc, String cv, String phone) {
        if (App.currentUser == null) {
            showAlert("Error", "You must be logged in to update an application");
            return;
        }
        try (Connection conn = MyDataBase.getConnection()) {
            String sql = "UPDATE job_applications SET title=?, description=?, cv_path=?, phone_number=? WHERE id=?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, title);
                stmt.setString(2, desc);
                stmt.setString(3, cv);
                stmt.setString(4, phone);
                stmt.setInt(5, appId);

                stmt.executeUpdate();
            }
            showAlert("Success", "Application updated!");
            loadFreelancerApplicationState(); // Refresh

        } catch (Throwable ex) {
            System.err.println("ERROR in updateApplication: " + ex.getMessage());
            ex.printStackTrace();
            showAlert("Error", "Failed to update: " + ex.getMessage());
        }
    }

    private void deleteMyApplication(JobApplicationModel app) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Withdraw application?", ButtonType.YES, ButtonType.NO);
        confirm.initOwner(App.getPrimaryStage());
        if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            try (Connection conn = MyDataBase.getConnection();
                    PreparedStatement stmt = conn.prepareStatement("DELETE FROM job_applications WHERE id=?")) {
                stmt.setInt(1, app.getId());
                stmt.executeUpdate();
                showAlert("Success", "Application withdrawn.");
                loadFreelancerApplicationState();
                countTotalApplicants();
            } catch (SQLException e) {
                showAlert("Error", e.getMessage());
            }
        }
    }

    // ==================== COMMON UTILS ====================

    // For Admin: Delete ANY application
    private void deleteSelectedApplication() {
        // Not used in card view, but kept if needed for future logic
    }

    // For Admin: Edit status
    private void showEditApplicationDialog() {
        // Not used in card view
    }

    // ==================== SECURITY VALIDATION ====================

    /**
     * Validates uploaded CV files for security
     * Checks: file size, extension, and basic content validation
     */
    private String validateCvFile(java.io.File file) {
        // Check file size (max 10MB)
        long maxSize = 10 * 1024 * 1024; // 10MB
        if (file.length() > maxSize) {
            return "File too large. Maximum size is 10MB.";
        }

        // Check file extension (whitelist approach)
        String fileName = file.getName().toLowerCase();
        String[] allowedExtensions = { ".pdf", ".doc", ".docx", ".txt" };
        boolean validExtension = false;
        for (String ext : allowedExtensions) {
            if (fileName.endsWith(ext)) {
                validExtension = true;
                break;
            }
        }

        if (!validExtension) {
            return "Invalid file type. Only PDF, DOC, DOCX, and TXT files are allowed.";
        }

        // Block executable and script files
        String[] dangerousExtensions = { ".exe", ".bat", ".sh", ".cmd", ".com", ".scr", ".vbs", ".js", ".jar" };
        for (String ext : dangerousExtensions) {
            if (fileName.endsWith(ext)) {
                return "Dangerous file type detected. This file type is not allowed.";
            }
        }

        // Basic content check - read first few bytes to verify it's not an executable
        try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
            byte[] header = new byte[4];
            int bytesRead = fis.read(header);
            if (bytesRead >= 2) {
                // Check for common executable signatures
                if (header[0] == 'M' && header[1] == 'Z') { // Windows PE executable
                    return "File appears to be an executable. Not allowed.";
                }
                if (bytesRead >= 4 && header[0] == 0x7F && header[1] == 'E' && header[2] == 'L' && header[3] == 'F') { // ELF
                                                                                                                       // executable
                    return "File appears to be an executable. Not allowed.";
                }
            }
        } catch (java.io.IOException e) {
            return "Error reading file. Please try again.";
        }

        return null; // File is valid
    }

    /**
     * Validates CV URLs for security
     * Checks: URL format, protocol, and blocks suspicious domains
     */
    private String validateCvUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return "URL cannot be empty.";
        }

        url = url.trim();

        // Check if it's a valid URL format
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return "URL must start with http:// or https://";
        }

        // Block suspicious or dangerous URL patterns
        String lowerUrl = url.toLowerCase();
        String[] blockedPatterns = {
                "javascript:", "data:", "file:", "ftp:",
                ".exe", ".bat", ".sh", ".cmd", ".scr", ".vbs"
        };

        for (String pattern : blockedPatterns) {
            if (lowerUrl.contains(pattern)) {
                return "Suspicious URL pattern detected. This URL is not allowed.";
            }
        }

        // Block adult/inappropriate content keywords
        String[] adultKeywords = {
                "porn", "xxx", "adult", "sex", "nude", "nsfw",
                "escort", "casino", "gambling", "viagra", "cialis",
                "18+", "xnxx", "xvideos", "pornhub", "redtube",
                "onlyfans", "chaturbate", "livejasmin"
        };

        for (String keyword : adultKeywords) {
            if (lowerUrl.contains(keyword)) {
                return "⚠️ Inappropriate content detected. This URL is not allowed for professional CV submissions.";
            }
        }

        // Block known malicious/spam domains and URL shorteners
        String[] blockedDomains = {
                "bit.ly", "tinyurl.com", "goo.gl",
                "mediafire.com", "4shared.com",
                "torrent", "pirate", "crack", "keygen"
        };

        for (String domain : blockedDomains) {
            if (lowerUrl.contains(domain)) {
                return "This domain is not allowed. Please use professional platforms like Google Drive, Dropbox, LinkedIn, or GitHub.";
            }
        }

        // Check URL length (prevent extremely long URLs)
        if (url.length() > 2048) {
            return "URL is too long. Maximum length is 2048 characters.";
        }

        return null; // URL is valid
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024)
            return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    private String getJobPosterName(int userId) {
        String name = "Unknown";
        try (Connection conn = MyDataBase.getConnection();
                PreparedStatement stmt = conn
                        .prepareStatement("SELECT first_name, last_name FROM users WHERE id = ?")) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                name = rs.getString("first_name") + " " + rs.getString("last_name");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return name;
    }

    private Button createAIButton(TextArea target, java.util.Map<String, String> context) {
        Button aiBtn = new Button("✨ AI Enhance");
        aiBtn.setStyle(
                "-fx-background-color: #f5f3ff; -fx-text-fill: #7c3aed; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 12; -fx-cursor: hand;");
        aiBtn.setOnAction(e -> {
            String original = target.getText();
            if (original.isEmpty()) {
                showAlert("Info", "Please type something first so I can improve it!");
                return;
            }
            aiBtn.setDisable(true);
            aiBtn.setText("⏳ Enhancing...");

            AIService.rewriteProfessionally(original, context).thenAccept(improved -> {
                javafx.application.Platform.runLater(() -> {
                    target.setText(improved);
                    aiBtn.setDisable(false);
                    aiBtn.setText("✨ AI Enhance");
                });
            });
        });
        return aiBtn;
    }

    private void showAlert(String title, String msg) {
        javafx.application.Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(msg);
            alert.show();
        });
    }
}
