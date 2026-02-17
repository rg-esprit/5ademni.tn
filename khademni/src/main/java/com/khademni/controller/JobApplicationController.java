package com.khademni.controller;

import com.khademni.App;
import com.khademni.model.JobApplicationModel;
import com.khademni.model.JobModel;
import com.khademni.utils.MyDataBase;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class JobApplicationController {

    @FXML private TableView<JobApplicationModel> applicationsTable;
    @FXML private TableColumn<JobApplicationModel, String> nameColumn;
    @FXML private TableColumn<JobApplicationModel, String> emailColumn;
    @FXML private TableColumn<JobApplicationModel, String> titleColumn;
    @FXML private TableColumn<JobApplicationModel, String> descriptionColumn;
    @FXML private TableColumn<JobApplicationModel, String> cvColumn;
    @FXML private TableColumn<JobApplicationModel, String> statusColumn;
    @FXML private TableColumn<JobApplicationModel, LocalDate> appliedDateColumn;
    @FXML private Label jobTitleLabel;
    @FXML private Label applicationsCountLabel;
    @FXML private Button addButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private ComboBox<String> statusFilterCombo;

    private JobModel selectedJob;
    private ObservableList<JobApplicationModel> applicationsList;

    public void setSelectedJob(JobModel job) {
        this.selectedJob = job;
        jobTitleLabel.setText("Job: " + job.getTitle());
        initialize();
    }

    @FXML
    public void initialize() {
        applicationsList = FXCollections.observableArrayList();
        applicationsTable.setItems(applicationsList);

        // Table columns
        nameColumn.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getApplicantName()));
        emailColumn.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getApplicantEmail()));
        titleColumn.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getTitle()));
        descriptionColumn.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getDescription()));
        cvColumn.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getCvUrl()));
        statusColumn.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getStatus()));
        appliedDateColumn.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue().getAppliedDate()));

        // Apply styling
        nameColumn.setStyle("-fx-alignment: CENTER-LEFT;");
        emailColumn.setStyle("-fx-alignment: CENTER-LEFT;");
        titleColumn.setStyle("-fx-alignment: CENTER-LEFT;");
        descriptionColumn.setStyle("-fx-alignment: CENTER-LEFT;");
        cvColumn.setStyle("-fx-alignment: CENTER-LEFT;");
        statusColumn.setStyle("-fx-alignment: CENTER;");
        appliedDateColumn.setStyle("-fx-alignment: CENTER;");

        // Status filter
        statusFilterCombo.setItems(FXCollections.observableArrayList("All", "PENDING", "ACCEPTED", "REJECTED"));
        statusFilterCombo.setValue("All");
        statusFilterCombo.setOnAction(e -> loadApplicationsFiltered());

        // Buttons
        addButton.setOnAction(e -> showAddApplicationDialog());
        editButton.setOnAction(e -> showEditApplicationDialog());
        deleteButton.setOnAction(e -> deleteSelectedApplication());

        if (selectedJob != null) loadApplicationsForJob();
    }

    // --------------- PARTIE DATABASE ----------------

    private void loadApplicationsForJob() {
        List<JobApplicationModel> apps = new ArrayList<>();
        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT ja.id, ja.job_id, ja.title, ja.description, ja.cv_path, ja.status, ja.application_date, " +
                             "u.first_name AS applicant_name, u.email AS applicant_email " +
                             "FROM job_applications ja LEFT JOIN users u ON ja.user_id = u.id WHERE ja.job_id = ? ORDER BY ja.application_date DESC"
             )) {
            stmt.setInt(1, selectedJob.getId());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                java.sql.Timestamp ts = rs.getTimestamp("application_date");
                LocalDate applied = ts != null ? ts.toLocalDateTime().toLocalDate() : LocalDate.now();
                apps.add(new JobApplicationModel(
                        rs.getInt("id"),
                        rs.getInt("job_id"),
                        rs.getString("applicant_name"),
                        rs.getString("applicant_email"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getString("cv_path"),
                        rs.getString("status"),
                        applied
                ));
            }
        } catch (SQLException e) {
            showAlert("Error", "Failed to load applications: " + e.getMessage());
        }
        applicationsList.setAll(apps);
        applicationsCountLabel.setText("Total applications: " + apps.size());
    }

    private void loadApplicationsFiltered() {
        String filter = statusFilterCombo.getValue();
        if ("All".equals(filter)) {
            loadApplicationsForJob();
        } else {
            List<JobApplicationModel> filtered = new ArrayList<>();
            for (JobApplicationModel app : applicationsList) {
                if (app.getStatus().equalsIgnoreCase(filter)) filtered.add(app);
            }
            applicationsList.setAll(filtered);
            applicationsCountLabel.setText("Showing: " + filtered.size() + " application(s)");
        }
    }

    // ----------------- ADD / EDIT / DELETE -----------------

    @FXML
    private void showAddApplicationDialog() {
        if (selectedJob == null) { showAlert("Warning", "No job selected"); return; }

        Stage dialog = new Stage();
        dialog.setTitle("Add Job Application");
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setResizable(false);

        ScrollPane form = createApplicationForm(null, dialog);
        Scene scene = new Scene(form, 600, 550);
        dialog.setScene(scene);
        dialog.show();
    }

    public void openAddApplicationDialog() {
        showAddApplicationDialog();
    }

    private void showEditApplicationDialog() {
        JobApplicationModel selected = applicationsTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("Warning", "Please select an application to edit"); return; }

        Stage dialog = new Stage();
        dialog.setTitle("Edit Job Application");
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setResizable(false);

        ScrollPane form = createApplicationForm(selected, dialog);
        Scene scene = new Scene(form, 500, 400);
        dialog.setScene(scene);
        dialog.show();
    }

    private ScrollPane createApplicationForm(JobApplicationModel app, Stage dialog) {
        VBox vbox = new VBox(15);
        vbox.setPadding(new Insets(20));
        vbox.setStyle("-fx-spacing: 15; -fx-padding: 20;");

        // Auto-filled user info (read-only)
        String userName = App.currentUser != null ? (App.currentUser.getFirstName() + " " + App.currentUser.getLastName()) : "Unknown";
        String userEmail = App.currentUser != null ? App.currentUser.getEmail() : "Unknown";

        Label userInfoLabel = new Label("👤 Applicant: " + userName + " | " + userEmail);
        userInfoLabel.setStyle("-fx-font-size: 12; -fx-font-weight: 600; -fx-text-fill: #6c0df2;");

        // Title field
        TextField titleField = new TextField();
        titleField.setPromptText("Application Title (e.g., Senior Developer Application)");
        titleField.setStyle("-fx-padding: 10 12 10 12; -fx-border-color: #e5e7eb; -fx-border-radius: 8; -fx-background-radius: 8; -fx-font-size: 12;");
        if (app != null) titleField.setText(app.getTitle());

        // Description field
        TextArea descriptionArea = new TextArea();
        descriptionArea.setPromptText("Why are you interested in this position? Tell us about your experience and motivation...");
        descriptionArea.setStyle("-fx-padding: 10 12 10 12; -fx-border-color: #e5e7eb; -fx-border-radius: 8; -fx-background-radius: 8; -fx-font-size: 12; -fx-control-inner-background: #fafafa;");
        descriptionArea.setWrapText(true);
        descriptionArea.setPrefRowCount(6);
        if (app != null) descriptionArea.setText(app.getDescription());

        // CV URL field
        TextField cvField = new TextField();
        cvField.setPromptText("CV URL (e.g., https://resume.example.com or file path)");
        cvField.setStyle("-fx-padding: 10 12 10 12; -fx-border-color: #e5e7eb; -fx-border-radius: 8; -fx-background-radius: 8; -fx-font-size: 12;");
        if (app != null) cvField.setText(app.getCvUrl());

        // Status combo
        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.setItems(FXCollections.observableArrayList("PENDING", "ACCEPTED", "REJECTED"));
        statusCombo.setValue(app != null ? app.getStatus() : "PENDING");
        statusCombo.setStyle("-fx-padding: 8 12 8 12; -fx-border-color: #e5e7eb; -fx-border-radius: 8;");

        // Validation label
        Label validationLabel = new Label();
        validationLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: 600; -fx-font-size: 11;");

        // Action buttons
        Button save = new Button(app != null ? "✏️ Update Application" : "✅ Submit Application");
        save.setStyle("-fx-background-color: #6c0df2; -fx-text-fill: white; -fx-font-weight: 600; " +
                      "-fx-padding: 12 24 12 24; -fx-border-radius: 8; -fx-cursor: hand; -fx-font-size: 12;");

        Button cancel = new Button("✕ Cancel");
        cancel.setStyle("-fx-background-color: #f0f0f0; -fx-text-fill: #333333; -fx-font-weight: 600; " +
                        "-fx-padding: 12 24 12 24; -fx-border-radius: 8; -fx-cursor: hand; -fx-font-size: 12;");

        HBox buttons = new HBox(12);
        buttons.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        buttons.getChildren().addAll(cancel, save);

        save.setOnAction(e -> {
            String title = titleField.getText().trim();
            String description = descriptionArea.getText().trim();
            String cv = cvField.getText().trim();
            String status = statusCombo.getValue();

            // Validation
            String validationError = validateApplicationInputs(title, description, cv);
            if (!validationError.isEmpty()) {
                validationLabel.setText("❌ " + validationError);
                return;
            }

            try (Connection conn = MyDataBase.getConnection()) {
                if (app == null) {
                    // INSERT
                    PreparedStatement stmt = conn.prepareStatement(
                            "INSERT INTO job_applications(job_id, user_id, title, description, cv_path, status, application_date) VALUES(?,?,?,?,?,?,?)"
                    );
                    stmt.setInt(1, selectedJob.getId());
                    stmt.setInt(2, App.currentUser != null ? App.currentUser.getId() : 1);
                    stmt.setString(3, title);
                    stmt.setString(4, description);
                    stmt.setString(5, cv);
                    stmt.setString(6, status.toUpperCase());
                    stmt.setDate(7, java.sql.Date.valueOf(LocalDate.now()));
                    stmt.executeUpdate();
                    showAlert("Success", "✅ Application submitted successfully!");
                } else {
                    // UPDATE
                    PreparedStatement stmt = conn.prepareStatement(
                            "UPDATE job_applications SET title=?, description=?, cv_path=?, status=? WHERE id=?"
                    );
                    stmt.setString(1, title);
                    stmt.setString(2, description);
                    stmt.setString(3, cv);
                    stmt.setString(4, status.toUpperCase());
                    stmt.setInt(5, app.getId());
                    stmt.executeUpdate();
                    showAlert("Success", "✅ Application updated successfully!");
                }
                loadApplicationsForJob();
                dialog.close();
            } catch (SQLException ex) {
                showAlert("Error", "Database error: " + ex.getMessage());
            }
        });

        cancel.setOnAction(e -> dialog.close());

        vbox.getChildren().addAll(
                new Label("📝 Application Form"),
                userInfoLabel,
                new Label("Application Title:"),
                titleField,
                new Label("Why are you interested?"),
                descriptionArea,
                new Label("CV URL:"),
                cvField,
                new Label("Status:"),
                statusCombo,
                validationLabel,
                buttons
        );

        ScrollPane scrollPane = new ScrollPane(vbox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-padding: 0; -fx-background-color: white;");
        return scrollPane;
    }

    private void deleteSelectedApplication() {
        JobApplicationModel selected = applicationsTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("Warning", "Please select an application to delete"); return; }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,"Are you sure you want to delete?",ButtonType.YES,ButtonType.NO);
        confirm.setTitle("Confirm Delete");
        if (confirm.showAndWait().get() == ButtonType.YES) {
            try (Connection conn = MyDataBase.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("DELETE FROM job_applications WHERE id=?")) {
                stmt.setInt(1, selected.getId());
                stmt.executeUpdate();
                showAlert("Success","Application deleted successfully!");
                loadApplicationsForJob();
            } catch (SQLException e) { showAlert("Error","Database error: "+e.getMessage()); }
        }
    }

    // ----------------- controle saisie -----------------
    private String validateApplicationInputs(String title, String description, String cvUrl) {
        if (title.isEmpty()) return "Application title is required";
        if (title.length() < 5) return "Title must be at least 5 characters";
        if (description.isEmpty()) return "Description is required";
        if (description.length() < 20) return "Description must be at least 20 characters";
        if (cvUrl.isEmpty()) return "CV URL is required";
        if (!cvUrl.matches("^(https?://.+|file://.+|.+\\.(pdf|doc|docx))$")) return "Invalid CV URL format";
        return "";
    }

    // ----------------- UTILITY -----------------
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title); alert.setContentText(content); alert.showAndWait();
    }
}
