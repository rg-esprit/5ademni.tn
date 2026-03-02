package com.khademni.controller;

import com.khademni.App;
import com.khademni.model.JobModel;
import com.khademni.utils.MyDataBase;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.animation.*;
import javafx.util.Duration;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JobsManagementController {

    @FXML
    private TextField searchJobsField;

    @FXML
    private VBox jobsTableContainer;

    private List<JobModel> allJobs = new ArrayList<>();
    private List<JobModel> filteredJobs = new ArrayList<>();

    @FXML
    public void initialize() {
        loadAllJobs();
        displayJobsTable();
    }

    // ==================== LOAD/READ ====================
    private void loadAllJobs() {
        allJobs.clear();
        try {
            Connection conn = MyDataBase.getConnection();
            if (conn == null) {
                showAlert(Alert.AlertType.ERROR, "Database Error", "Unable to connect to database");
                loadSampleJobs();
                return;
            }

            String query = "SELECT j.*, u.first_name, u.last_name, u.email FROM jobs j LEFT JOIN users u ON j.user_id = u.id ORDER BY j.posted_date DESC, j.id DESC";
            Statement statement = conn.createStatement();
            ResultSet resultSet = statement.executeQuery(query);

            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String title = resultSet.getString("title");
                String company = resultSet.getString("company");
                String location = resultSet.getString("location");
                String description = resultSet.getString("description");
                String category = resultSet.getString("category");
                String salaryRange = resultSet.getString("salary_range");
                String jobType = resultSet.getString("job_type");
                LocalDateTime postedDate = resultSet.getTimestamp("posted_date").toLocalDateTime();
                String requirementsJson = resultSet.getString("requirements");
                int userId = resultSet.getInt("user_id");

                String[] requirements = requirementsJson != null ? requirementsJson.split(",\\s*") : new String[0];

                // Get user name and email from joined users table
                String firstName = resultSet.getString("first_name");
                String lastName = resultSet.getString("last_name");
                String userName = "Unknown User";
                if (firstName != null && lastName != null) {
                    userName = firstName + " " + lastName;
                }
                String userEmail = resultSet.getString("email");
                if (userEmail == null) {
                    userEmail = "";
                }

                JobModel job = new JobModel(id, title, company, location, description,
                        category, salaryRange, jobType, postedDate,
                        requirements, userId, userName, userEmail);
                allJobs.add(job);
            }

            resultSet.close();
            statement.close();
            conn.close();

        } catch (SQLException e) {
            System.err.println("Error loading jobs: " + e.getMessage());
            loadSampleJobs();
        }
    }

    private void loadSampleJobs() {
        allJobs.add(new JobModel(1, "Senior Java Developer", "Tech Innovators Inc", "Tunis",
                "Experienced Java developer needed...", "Software Development", "2000 - 3500 TND",
                "Full-time", LocalDateTime.now().minusDays(2),
                new String[] { "Java 17+", "Spring Boot", "MySQL" }, 1, "Ahmed Ben Ali", "ahmed.benali@techmail.com"));

        allJobs.add(new JobModel(2, "UI/UX Designer", "Creative Studio", "Remote",
                "Join our design team...", "Design", "1500 - 2500 TND",
                "Full-time", LocalDateTime.now().minusDays(5),
                new String[] { "Figma", "Adobe XD", "Prototyping" }, 2, "Fatima Karray", "fatima.karray@design.com"));

        allJobs.add(new JobModel(3, "Marketing Manager", "Digital Solutions", "Sfax",
                "Lead marketing initiatives...", "Marketing", "1200 - 2000 TND",
                "Full-time", LocalDateTime.now().minusDays(1),
                new String[] { "Digital Marketing", "Social Media", "Analytics" }, 3, "Salem Mezzi",
                "salem.mezzi@brandmail.com"));
    }

    // ==================== DISPLAY ====================
    private void displayJobsTable() {
        jobsTableContainer.getChildren().clear();

        if (allJobs.isEmpty()) {
            VBox emptyBox = new VBox(16);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setStyle("-fx-padding: 60 40 60 40; -fx-border-radius: 16; -fx-background-color: white;");
            Label emptyTitle = new Label("No jobs found");
            emptyTitle.setStyle("-fx-font-size: 20; -fx-font-weight: 700; -fx-text-fill: #141118;");
            Label emptySubtitle = new Label("Click 'Add New Job' to create your first job posting");
            emptySubtitle.setStyle("-fx-font-size: 14; -fx-font-weight: 400; -fx-text-fill: #999999;");
            emptyBox.getChildren().addAll(emptyTitle, emptySubtitle);
            jobsTableContainer.getChildren().add(emptyBox);
            return;
        }

        int idx = 0;
        for (JobModel job : allJobs) {
            VBox row = createJobRow(job);
            // animation wl translation
            row.setOpacity(0);
            row.setTranslateY(12);
            jobsTableContainer.getChildren().add(row);

            FadeTransition ft = new FadeTransition(Duration.millis(380), row);
            ft.setFromValue(0);
            ft.setToValue(1);
            TranslateTransition tt = new TranslateTransition(Duration.millis(380), row);
            tt.setFromY(12);
            tt.setToY(0);
            ParallelTransition pt = new ParallelTransition(ft, tt);
            pt.setDelay(Duration.millis(idx * 70));
            pt.play();
            idx++;
        }
    }

    private VBox createJobRow(JobModel job) {
        VBox row = new VBox(12);
        row.setStyle("-fx-background-color: white; -fx-border-radius: 12; -fx-padding: 18 20; " +
                "-fx-border-color: rgba(139,92,246,0.06); -fx-border-width: 1; " +
                "-fx-effect: dropshadow(gaussian, rgba(124,58,237,0.06), 22, 0, 0, 8);");

        // Header Row
        HBox headerBox = new HBox(16);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label titleLabel = new Label(job.getTitle());
        titleLabel.setStyle("-fx-font-size: 18; -fx-font-weight: 800; -fx-text-fill: #141118;");
        Label companyLabel = new Label(job.getCompany());
        companyLabel.setStyle("-fx-font-size: 14; -fx-font-weight: 600; -fx-text-fill: #6c0df2;");
        titleBox.getChildren().addAll(titleLabel, companyLabel);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        headerBox.getChildren().add(titleBox);
        row.getChildren().add(headerBox);

        // Info Row: Location, Type, Salary
        HBox infoBox = new HBox(16);
        infoBox.setAlignment(Pos.CENTER_LEFT);

        Label locationLabel = new Label("📍 " + job.getLocation());
        locationLabel.setStyle("-fx-font-size: 13; -fx-font-weight: 500; -fx-text-fill: #333333;");

        Label typeLabel = new Label("💼 " + job.getJobType());
        typeLabel.setStyle("-fx-font-size: 13; -fx-font-weight: 500; -fx-text-fill: #333333;");

        Label salaryLabel = new Label("💰 " + job.getSalaryRange());
        salaryLabel.setStyle("-fx-font-size: 13; -fx-font-weight: 500; -fx-text-fill: #333333;");

        Label categoryLabel = new Label("📌 " + job.getCategory());
        categoryLabel.setStyle("-fx-font-size: 13; -fx-font-weight: 500; -fx-text-fill: #333333;");

        infoBox.getChildren().addAll(locationLabel, typeLabel, salaryLabel, categoryLabel);
        row.getChildren().add(infoBox);

        // Description
        Label descLabel = new Label(job.getDescription());
        descLabel.setStyle("-fx-font-size: 13; -fx-font-weight: 400; -fx-text-fill: #666666; -fx-wrap-text: true;");
        descLabel.setWrapText(true);
        row.getChildren().add(descLabel);

        // Action Buttons
        HBox actionsBox = new HBox(12);
        actionsBox.setAlignment(Pos.CENTER_RIGHT);

        Button viewBtn = new Button("👁 View");
        viewBtn.setStyle("-fx-background-color: #f0f0f0; -fx-text-fill: #333333; -fx-font-weight: 600; " +
                "-fx-padding: 8 16 8 16; -fx-border-radius: 8; -fx-cursor: hand;");
        viewBtn.setOnAction(e -> showJobDetails(job));

        Button editBtn = new Button("✏️ Edit");
        editBtn.setStyle("-fx-background-color: #6c0df2; -fx-text-fill: white; -fx-font-weight: 600; " +
                "-fx-padding: 8 16 8 16; -fx-border-radius: 8; -fx-cursor: hand;");
        editBtn.setOnAction(e -> showEditJobDialog(job));

        Button deleteBtn = new Button("🗑️ Delete");
        deleteBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: 600; " +
                "-fx-padding: 8 16 8 16; -fx-border-radius: 8; -fx-cursor: hand;");
        deleteBtn.setOnAction(e -> deleteJob(job));

        Button appsBtn = new Button("📨 Applications");
        appsBtn.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: 600; " +
                "-fx-padding: 8 16 8 16; -fx-border-radius: 8; -fx-cursor: hand;");
        appsBtn.setOnAction(e -> showApplicationsForJob(job));

        actionsBox.getChildren().addAll(viewBtn, appsBtn, editBtn, deleteBtn);
        row.getChildren().add(actionsBox);

        // hover lift + glow
        DropShadow hover = new DropShadow(30, Color.web("#7c3aed", 0.10));
        row.setOnMouseEntered(ev -> {
            TranslateTransition lift = new TranslateTransition(Duration.millis(160), row);
            lift.setToY(-6);
            lift.play();
            row.setEffect(hover);
        });
        row.setOnMouseExited(ev -> {
            TranslateTransition down = new TranslateTransition(Duration.millis(160), row);
            down.setToY(0);
            down.play();
            row.setEffect(null);
        });

        return row;
    }

    // ==================== CREATE ====================
    @FXML
    private void showAddJobDialog() {
        Dialog<JobModel> dialog = new Dialog<>();
        dialog.initOwner(App.getPrimaryStage());
        dialog.setTitle("Add New Job");
        dialog.setHeaderText("Create a new job posting");

        DialogPane dialogPane = dialog.getDialogPane();
        ButtonType postButtonType = new ButtonType("Post", ButtonBar.ButtonData.OK_DONE);
        dialogPane.getButtonTypes().addAll(postButtonType, ButtonType.CANCEL);

        VBox content = createJobForm(null);
        dialogPane.setContent(content);

        javafx.scene.Node okButton = dialog.getDialogPane().lookupButton(postButtonType);
        okButton.addEventFilter(ActionEvent.ACTION, evt -> {
            JobModel candidate = extractJobFormData(content, null);
            if (candidate.getTitle() == null || candidate.getTitle().trim().length() < 3
                    || candidate.getCompany() == null || candidate.getCompany().trim().length() < 3
                    || candidate.getLocation() == null || candidate.getLocation().trim().length() < 3
                    || candidate.getDescription() == null || candidate.getDescription().trim().length() < 20
                    || candidate.getSalaryRange() == null || candidate.getSalaryRange().trim().isEmpty()) {
                evt.consume();
                showAlert(Alert.AlertType.ERROR, "Validation",
                        "Please provide valid Title, Company, Location (min 3 chars), Description (min 20 chars), and Salary (must be a valid integer).");
            }
        });

        dialog.setResultConverter(buttonType -> {
            if (buttonType == postButtonType) {
                return extractJobFormData(content, null);
            }
            return null;
        });

        Optional<JobModel> result = dialog.showAndWait();
        if (result.isPresent()) {
            insertJobToDB(result.get());
        }
    }

    private void insertJobToDB(JobModel job) {
        try {
            Connection conn = MyDataBase.getConnection();
            if (conn == null) {
                showAlert(Alert.AlertType.ERROR, "Database Error", "Unable to connect to database");
                return;
            }

            String query = "INSERT INTO jobs (title, company, location, description, category, salary_range, job_type, posted_date, requirements, user_id) "
                    +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, job.getTitle());
            stmt.setString(2, job.getCompany());
            stmt.setString(3, job.getLocation());
            stmt.setString(4, job.getDescription());
            stmt.setString(5, job.getCategory());
            stmt.setString(6, job.getSalaryRange());
            stmt.setString(7, job.getJobType());
            stmt.setTimestamp(8, Timestamp.valueOf(job.getPostedDate()));
            stmt.setString(9, String.join(", ", job.getRequirements()));
            stmt.setInt(10, App.currentUser != null ? App.currentUser.getId() : 1);

            stmt.executeUpdate();
            stmt.close();
            conn.close();

            showAlert(Alert.AlertType.INFORMATION, "Success", "Job created successfully!");
            loadAllJobs();
            displayJobsTable();

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Error creating job: " + e.getMessage());
        }
    }

    // ==================== UPDATE ====================
    private void showEditJobDialog(JobModel job) {
        Dialog<JobModel> dialog = new Dialog<>();
        dialog.initOwner(App.getPrimaryStage());
        dialog.setTitle("Edit Job");
        dialog.setHeaderText("Edit job posting: " + job.getTitle());

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStylesheets().add(
                getClass().getResource("/com/khademni/dialogs.css").toExternalForm());
        dialogPane.getStyleClass().add("job-dialog");
        ButtonType editButtonType = new ButtonType("Edit", ButtonBar.ButtonData.OK_DONE);
        dialogPane.getButtonTypes().addAll(editButtonType, ButtonType.CANCEL);

        VBox content = createJobForm(job);
        dialogPane.setContent(content);

        javafx.scene.Node okButtonEdit = dialog.getDialogPane().lookupButton(editButtonType);
        okButtonEdit.addEventFilter(ActionEvent.ACTION, evt -> {
            JobModel candidate = extractJobFormData(content, job);
            if (candidate.getTitle() == null || candidate.getTitle().trim().length() < 3
                    || candidate.getCompany() == null || candidate.getCompany().trim().length() < 3
                    || candidate.getLocation() == null || candidate.getLocation().trim().length() < 3
                    || candidate.getDescription() == null || candidate.getDescription().trim().length() < 20
                    || candidate.getSalaryRange() == null || candidate.getSalaryRange().trim().isEmpty()) {
                evt.consume();
                showAlert(Alert.AlertType.ERROR, "Validation",
                        "Please provide valid Title, Company, Location (min 3 chars), Description (min 20 chars), and Salary (must be a valid integer).");
            }
        });

        dialog.setResultConverter(buttonType -> {
            if (buttonType == editButtonType) {
                return extractJobFormData(content, job);
            }
            return null;
        });

        Optional<JobModel> result = dialog.showAndWait();
        if (result.isPresent()) {
            updateJobInDB(result.get());
        }
    }

    private void updateJobInDB(JobModel job) {
        try {
            Connection conn = MyDataBase.getConnection();
            if (conn == null) {
                showAlert(Alert.AlertType.ERROR, "Database Error", "Unable to connect to database");
                return;
            }

            String query = "UPDATE jobs SET title=?, company=?, location=?, description=?, category=?, salary_range=?, job_type=?, requirements=? WHERE id=?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, job.getTitle());
            stmt.setString(2, job.getCompany());
            stmt.setString(3, job.getLocation());
            stmt.setString(4, job.getDescription());
            stmt.setString(5, job.getCategory());
            stmt.setString(6, job.getSalaryRange());
            stmt.setString(7, job.getJobType());
            stmt.setString(8, String.join(", ", job.getRequirements()));
            stmt.setInt(9, job.getId());

            stmt.executeUpdate();
            stmt.close();
            conn.close();

            showAlert(Alert.AlertType.INFORMATION, "Success", "Job updated successfully!");
            loadAllJobs();
            displayJobsTable();

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Error updating job: " + e.getMessage());
        }
    }

    // ==================== DELETE ====================
    private void deleteJob(JobModel job) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.initOwner(App.getPrimaryStage());
        confirmAlert.setTitle("Delete Job");
        confirmAlert.setHeaderText("Confirm Deletion");
        confirmAlert.setContentText("Are you sure you want to delete this job?\n\n" + job.getTitle());

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                Connection conn = MyDataBase.getConnection();
                if (conn == null) {
                    showAlert(Alert.AlertType.ERROR, "Database Error", "Unable to connect to database");
                    return;
                }

                String query = "DELETE FROM jobs WHERE id=?";
                PreparedStatement stmt = conn.prepareStatement(query);
                stmt.setInt(1, job.getId());
                stmt.executeUpdate();
                stmt.close();
                conn.close();

                showAlert(Alert.AlertType.INFORMATION, "Success", "Job deleted successfully!");
                loadAllJobs();
                displayJobsTable();

            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Database Error", "Error deleting job: " + e.getMessage());
            }
        }
    }

    // ==================== VIEW ====================
    private void showJobDetails(JobModel job) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(App.getPrimaryStage());
        alert.setTitle("Job Details");
        alert.setHeaderText(job.getTitle() + " at " + job.getCompany());

        VBox content = new VBox(12);
        content.setStyle("-fx-padding: 10;");

        addDetailRow(content, "Company", job.getCompany());
        addDetailRow(content, "Location", job.getLocation());
        addDetailRow(content, "Job Type", job.getJobType());
        addDetailRow(content, "Salary Range", job.getSalaryRange());
        addDetailRow(content, "Category", job.getCategory());
        addDetailRow(content, "Posted Date", formatPostedDate(job.getPostedDate()));

        Label descTitle = new Label("Description:");
        descTitle.setStyle("-fx-font-weight: 700; -fx-font-size: 12;");
        TextArea descArea = new TextArea(job.getDescription());
        descArea.setWrapText(true);
        descArea.setPrefRowCount(4);
        descArea.setEditable(false);

        Label reqTitle = new Label("Requirements:");
        reqTitle.setStyle("-fx-font-weight: 700; -fx-font-size: 12;");
        TextArea reqArea = new TextArea(String.join("\n", job.getRequirements()));
        reqArea.setWrapText(true);
        reqArea.setPrefRowCount(4);
        reqArea.setEditable(false);

        content.getChildren().addAll(descTitle, descArea, reqTitle, reqArea);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        alert.getDialogPane().setContent(scroll);
        alert.showAndWait();
    }

    private void showApplicationsForJob(JobModel job) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/khademni/JobApplications.fxml"));
            BorderPane root = loader.load();

            // pass the selected job to the controller
            com.khademni.controller.JobApplicationController controller = loader.getController();
            controller.setSelectedJob(job);

            Stage stage = new Stage();
            stage.setTitle("Applications for: " + job.getTitle());
            stage.setScene(new Scene(root, 900, 600));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to open applications view: " + e.getMessage());
        }
    }

    private void addDetailRow(VBox container, String label, String value) {
        HBox row = new HBox(8);
        Label keyLabel = new Label(label + ":");
        keyLabel.setStyle("-fx-font-weight: 700; -fx-min-width: 100;");
        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-text-fill: #333333;");
        row.getChildren().addAll(keyLabel, valueLabel);
        container.getChildren().add(row);
    }

    // ==================== FORM HELPERS ====================
    private VBox createJobForm(JobModel editingJob) {
        VBox form = new VBox(12);
        form.setStyle("-fx-padding: 20;");

        TextField titleField = createFormField("Job Title", editingJob != null ? editingJob.getTitle() : "");
        TextField companyField = createFormField("Company Name", editingJob != null ? editingJob.getCompany() : "");
        TextField locationField = createFormField("Location", editingJob != null ? editingJob.getLocation() : "");
        TextField categoryField = createFormField("Category", editingJob != null ? editingJob.getCategory() : "");
        TextField salaryField = createIntegerSalaryField("Salary (in TND)",
                editingJob != null ? editingJob.getSalaryRange() : "");
        TextField typeField = createFormField("Job Type", editingJob != null ? editingJob.getJobType() : "");

        TextArea descArea = new TextArea(editingJob != null ? editingJob.getDescription() : "");
        descArea.setStyle("-fx-control-inner-background: #fafafa; -fx-focus-color: #6c0df2;");
        descArea.setPrefRowCount(4);
        descArea.setWrapText(true);
        addFormLabel("Description", form);
        form.getChildren().add(descArea);

        TextArea reqArea = new TextArea(editingJob != null ? String.join("\n", editingJob.getRequirements()) : "");
        reqArea.setStyle("-fx-control-inner-background: #fafafa; -fx-focus-color: #6c0df2;");
        reqArea.setPrefRowCount(4);
        reqArea.setWrapText(true);
        addFormLabel("Requirements (one per line)", form);
        form.getChildren().add(reqArea);

        // Store fields for later extraction
        form.setUserData(new Object[] { titleField, companyField, locationField, categoryField, salaryField, typeField,
                descArea, reqArea });

        form.getChildren().addAll(
                createFormSection("Title", titleField),
                createFormSection("Company", companyField),
                createFormSection("Location", locationField),
                createFormSection("Category", categoryField),
                createFormSection("Salary Range", salaryField),
                createFormSection("Job Type", typeField));

        return form;
    }

    private HBox createFormSection(String label, TextField field) {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);
        Label labelNode = new Label(label + ":");
        labelNode.setPrefWidth(120);
        labelNode.setStyle("-fx-font-weight: 700;");
        HBox.setHgrow(field, Priority.ALWAYS);
        field.setStyle(
                "-fx-padding: 8 12 8 12; -fx-border-color: #e5e7eb; -fx-border-radius: 8; -fx-background-radius: 8;");
        box.getChildren().addAll(labelNode, field);
        return box;
    }

    private TextField createFormField(String label, String value) {
        TextField field = new TextField(value);
        field.setStyle(
                "-fx-padding: 8 12 8 12; -fx-border-color: #e5e7eb; -fx-border-radius: 8; -fx-background-radius: 8;");
        field.setPromptText(label);
        return field;
    }

    private TextField createIntegerSalaryField(String label, String value) {
        TextField field = new TextField(value);
        field.setStyle(
                "-fx-padding: 8 12 8 12; -fx-border-color: #e5e7eb; -fx-border-radius: 8; -fx-background-radius: 8; -fx-focus-color: #6c0df2;");
        field.setPromptText(label);

        // Add text formatter to only accept integers
        field.setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            // Allow empty string or only digits
            if (newText.isEmpty() || newText.matches("\\d+")) {
                return change;
            }
            return null;
        }));

        field.styleProperty().addListener((obs, oldVal, newVal) -> {
            if (field.getText().isEmpty()) {
                field.setStyle(
                        "-fx-padding: 8 12 8 12; -fx-border-color: #fca5a5; -fx-border-radius: 8; -fx-background-radius: 8; -fx-focus-color: #ef4444;");
            }
        });

        return field;
    }

    private void addFormLabel(String label, VBox container) {
        Label l = new Label(label + ":");
        l.setStyle("-fx-font-weight: 700;");
        container.getChildren().add(l);
    }

    private JobModel extractJobFormData(VBox form, JobModel original) {
        Object[] fields = (Object[]) form.getUserData();
        if (fields == null || fields.length < 8) {

            return extractFromFormChildren(form, original);
        }

        TextField titleField = (TextField) fields[0];
        TextField companyField = (TextField) fields[1];
        TextField locationField = (TextField) fields[2];
        TextField categoryField = (TextField) fields[3];
        TextField salaryField = (TextField) fields[4];
        TextField typeField = (TextField) fields[5];
        TextArea descArea = (TextArea) fields[6];
        TextArea reqArea = (TextArea) fields[7];

        String[] requirements = reqArea.getText().split("\n");

        JobModel job = new JobModel(
                original != null ? original.getId() : 0,
                titleField.getText(),
                companyField.getText(),
                locationField.getText(),
                descArea.getText(),
                categoryField.getText(),
                salaryField.getText(),
                typeField.getText(),
                original != null ? original.getPostedDate() : LocalDateTime.now(),
                requirements,
                App.currentUser != null ? App.currentUser.getId() : 1);

        return job;
    }

    private JobModel extractFromFormChildren(VBox form, JobModel original) {

        List<TextField> textFields = new ArrayList<>();
        List<TextArea> textAreas = new ArrayList<>();

        for (javafx.scene.Node node : form.getChildren()) {
            if (node instanceof TextField) {
                textFields.add((TextField) node);
            } else if (node instanceof TextArea) {
                textAreas.add((TextArea) node);
            }
        }

        String[] requirements = !textAreas.isEmpty() && textAreas.size() > 1 ? textAreas.get(1).getText().split("\n")
                : new String[0];

        return new JobModel(
                original != null ? original.getId() : 0,
                textFields.size() > 0 ? textFields.get(0).getText() : "",
                textFields.size() > 1 ? textFields.get(1).getText() : "",
                textFields.size() > 2 ? textFields.get(2).getText() : "",
                textAreas.size() > 0 ? textAreas.get(0).getText() : "",
                textFields.size() > 3 ? textFields.get(3).getText() : "",
                textFields.size() > 4 ? textFields.get(4).getText() : "",
                textFields.size() > 5 ? textFields.get(5).getText() : "",
                original != null ? original.getPostedDate() : LocalDateTime.now(),
                requirements,
                App.currentUser != null ? App.currentUser.getId() : 1);
    }

    private String formatPostedDate(LocalDateTime dateTime) {
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

    // ==================== SEARCH ====================
    @FXML
    private void performSearch() {
        String query = searchJobsField.getText().toLowerCase().trim();
        filteredJobs.clear();

        if (query.isEmpty()) {
            filteredJobs.addAll(allJobs);
        } else {
            for (JobModel job : allJobs) {
                if (job.getTitle().toLowerCase().contains(query) ||
                        job.getCompany().toLowerCase().contains(query) ||
                        job.getLocation().toLowerCase().contains(query) ||
                        job.getDescription().toLowerCase().contains(query)) {
                    filteredJobs.add(job);
                }
            }
        }

        List<JobModel> temp = new ArrayList<>(allJobs);
        allJobs.clear();
        allJobs.addAll(filteredJobs);
        displayJobsTable();
        allJobs.clear();
        allJobs.addAll(temp);
    }

    // ==================== UTILITIES ====================
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.initOwner(App.getPrimaryStage());
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
