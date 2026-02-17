package com.khademni.controller;

import com.khademni.App;
import com.khademni.model.JobModel;
import com.khademni.utils.MyDataBase;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.animation.*;
import javafx.util.Duration;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JobsController {

    @FXML private TextField jobSearchField;
    @FXML private ComboBox<String> jobCategoryFilter;
    @FXML private ComboBox<String> jobLocationFilter;
    @FXML private ComboBox<String> jobSalaryFilter;
    @FXML private VBox jobsContainer;
    @FXML private VBox emptyStateBox;

    private List<JobModel> allJobs = new ArrayList<>();
    private List<JobModel> filteredJobs = new ArrayList<>();

    @FXML
    public void initialize() {
        setupComboBoxes();
        loadJobsFromDB();
        displayJobs(filteredJobs);
    }

    private void setupComboBoxes() {
        jobCategoryFilter.getItems().addAll("All Categories","Software Development","Design","Marketing","Sales","Business","HR","Finance");
        jobCategoryFilter.setValue("All Categories");

        jobLocationFilter.getItems().addAll("All Locations","Tunisia","Remote","Tunis","Sfax","Sousse","Gabes");
        jobLocationFilter.setValue("All Locations");

        jobSalaryFilter.getItems().addAll("All Ranges","500 - 1000 TND","1000 - 2000 TND","2000 - 3500 TND","3500+ TND");
        jobSalaryFilter.setValue("All Ranges");

        jobCategoryFilter.setOnAction(e -> applyFilters());
        jobLocationFilter.setOnAction(e -> applyFilters());
        jobSalaryFilter.setOnAction(e -> applyFilters());
    }

    // ==================== DATABASE ====================
    private void loadJobsFromDB() {
        allJobs.clear();
        filteredJobs.clear();
        try (Connection conn = MyDataBase.getConnection()) {
            if (conn == null) { loadSampleJobs(); return; }

            String query = "SELECT * FROM jobs ORDER BY posted_date DESC";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {

                while (rs.next()) {
                    JobModel job = mapResultSetToJob(rs);
                    allJobs.add(job);
                    filteredJobs.add(job);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading jobs: " + e.getMessage());
            loadSampleJobs();
        }
    }

    private JobModel mapResultSetToJob(ResultSet rs) throws SQLException {
        String[] requirements = rs.getString("requirements") != null ?
                rs.getString("requirements").split(",\\s*") : new String[0];

        return new JobModel(
                rs.getInt("id"),
                rs.getString("title"),
                rs.getString("company"),
                rs.getString("location"),
                rs.getString("description"),
                rs.getString("category"),
                rs.getString("salary_range"),
                rs.getString("job_type"),
                rs.getDate("posted_date").toLocalDate(),
                requirements,
                rs.getInt("user_id")
        );
    }

    private void loadSampleJobs() {
        allJobs.add(new JobModel(1, "Senior Java Developer","Tech Innovators Inc","Tunis",
                "We are looking for an experienced Java developer to join our team...",
                "Software Development","2000 - 3500 TND","Full-time",LocalDate.now().minusDays(2),
                new String[]{"Java 17+","Spring Boot","MySQL","REST APIs"},1));

        allJobs.add(new JobModel(2,"UI/UX Designer","Creative Studio","Remote",
                "Join our design team to create stunning user experiences...",
                "Design","1500 - 2500 TND","Full-time",LocalDate.now().minusDays(5),
                new String[]{"Figma","Adobe XD","Prototyping"},2));

        filteredJobs.addAll(allJobs);
    }

    // ==================== FILTER & SEARCH ====================
    @FXML private void performSearch() { applyFilters(); }

    @FXML private void resetFilters() {
        jobSearchField.clear();
        jobCategoryFilter.setValue("All Categories");
        jobLocationFilter.setValue("All Locations");
        jobSalaryFilter.setValue("All Ranges");
        filteredJobs.clear();
        filteredJobs.addAll(allJobs);
        displayJobs(filteredJobs);
    }

    private void applyFilters() {
        String searchQuery = jobSearchField.getText().toLowerCase().trim();
        String selectedCategory = jobCategoryFilter.getValue();
        String selectedLocation = jobLocationFilter.getValue();
        String selectedSalary = jobSalaryFilter.getValue();

        filteredJobs.clear();

        for (JobModel job : allJobs) {
            boolean matchesSearch = searchQuery.isEmpty() ||
                    job.getTitle().toLowerCase().contains(searchQuery) ||
                    job.getCompany().toLowerCase().contains(searchQuery) ||
                    job.getDescription().toLowerCase().contains(searchQuery);

            boolean matchesCategory = selectedCategory.equals("All Categories") ||
                    job.getCategory().equals(selectedCategory);

            boolean matchesLocation = selectedLocation.equals("All Locations") ||
                    job.getLocation().equals(selectedLocation);

            boolean matchesSalary = selectedSalary.equals("All Ranges") ||
                    job.getSalaryRange().equals(selectedSalary);

            if (matchesSearch && matchesCategory && matchesLocation && matchesSalary)
                filteredJobs.add(job);
        }

        displayJobs(filteredJobs);
    }

    // ==================== DISPLAY ====================
    private void displayJobs(List<JobModel> jobs) {
        jobsContainer.getChildren().clear();
        emptyStateBox.setVisible(jobs.isEmpty());

        int idx = 0;
        for (JobModel job : jobs) {
            VBox card = createJobCard(job);
            // prepare initial state for load animation
            card.setOpacity(0);
            card.setTranslateY(12);
            jobsContainer.getChildren().add(card);

            // staggered fade + slide up animation
            FadeTransition ft = new FadeTransition(Duration.millis(420), card);
            ft.setFromValue(0);
            ft.setToValue(1);
            TranslateTransition tt = new TranslateTransition(Duration.millis(420), card);
            tt.setFromY(12);
            tt.setToY(0);
            ParallelTransition pt = new ParallelTransition(ft, tt);
            pt.setDelay(Duration.millis(idx * 80));
            pt.play();
            idx++;
        }
    }

    private VBox createJobCard(JobModel job) {
        VBox card = new VBox(16);
        card.getStyleClass().add("job-card");
        card.setStyle("-fx-padding: 20; -fx-background-color: white; -fx-border-radius: 12; " +
                     "-fx-border-color: #f0f0f0; -fx-border-width: 1; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.06), 30, 0, 0, 8);");

        // Header
        HBox header = new HBox(16); 
        header.setAlignment(Pos.TOP_LEFT);
        Circle logo = new Circle(32); 
        logo.getStyleClass().add("job-company-logo");
        logo.setStyle("-fx-fill: #6c0df2;");
        
        VBox info = new VBox(4);
        Label company = new Label(job.getCompany()); 
        company.getStyleClass().add("job-company-name");
        company.setStyle("-fx-font-size: 12; -fx-font-weight: 600; -fx-text-fill: #6c0df2;");
        
        Label title = new Label(job.getTitle()); 
        title.getStyleClass().add("job-title"); 
        title.setWrapText(true);
        title.setStyle("-fx-font-size: 16; -fx-font-weight: 800; -fx-text-fill: #141118;");
        
        info.getChildren().addAll(company, title); 
        HBox.setHgrow(info, Priority.ALWAYS);
        header.getChildren().addAll(logo, info); 
        card.getChildren().add(header);

        // Meta
        HBox meta = new HBox(12); 
        meta.setAlignment(Pos.CENTER_LEFT); 
        meta.getStyleClass().add("job-meta-container");
        
        Label type = new Label(job.getJobType()); 
        type.getStyleClass().add("job-meta-tag");
        type.setStyle("-fx-background-color: #f3e8ff; -fx-text-fill: #6c0df2; -fx-padding: 4 8 4 8; -fx-border-radius: 6; -fx-font-size: 11; -fx-font-weight: 600;");
        
        Label loc = new Label("📍 "+ job.getLocation()); 
        loc.getStyleClass().add("job-location-tag");
        loc.setStyle("-fx-text-fill: #666666; -fx-font-size: 12;");
        
        Label salary = new Label("💰 " + job.getSalaryRange()); 
        salary.getStyleClass().add("job-salary-tag");
        salary.setStyle("-fx-text-fill: #666666; -fx-font-size: 12;");
        
        meta.getChildren().addAll(type, loc, salary); 
        card.getChildren().add(meta);

        // Description
        Label desc = new Label(job.getDescription()); 
        desc.getStyleClass().add("job-description"); 
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: #666666; -fx-font-size: 13; -fx-wrap-text: true;");
        card.getChildren().add(desc);

        // Requirements
        if (job.getRequirements().length > 0) {
            Label reqLabel = new Label("Key Requirements:"); 
            reqLabel.getStyleClass().add("job-requirements-label");
            reqLabel.setStyle("-fx-font-size: 12; -fx-font-weight: 700; -fx-text-fill: #141118;");
            
            VBox reqList = new VBox(6);
            for (String r : job.getRequirements()) {
                Label l = new Label("• " + r); 
                l.getStyleClass().add("job-requirement-item"); 
                l.setStyle("-fx-font-size: 11; -fx-text-fill: #666666;");
                reqList.getChildren().add(l);
            }
            card.getChildren().addAll(reqLabel, reqList);
        }

        // Footer
        HBox footer = new HBox(16); 
        footer.setAlignment(Pos.CENTER_LEFT); 
        footer.getStyleClass().add("job-card-footer");
        
        Label posted = new Label("Posted " + formatPostedDate(job.getPostedDate())); 
        posted.getStyleClass().add("job-posted-date");
        posted.setStyle("-fx-text-fill: #999999; -fx-font-size: 11;");
        
        Region spacer = new Region(); 
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button save = new Button("Save"); 
        save.getStyleClass().add("job-save-btn");
        save.setStyle("-fx-background-color: #f0f0f0; -fx-text-fill: #333333; -fx-font-weight: 600; " +
                      "-fx-padding: 8 16 8 16; -fx-border-radius: 8; -fx-cursor: hand;");
        save.setOnAction(e -> handleSaveJob(job));
        
        Button edit = new Button("✏️ Edit");
        edit.setStyle("-fx-background-color: #6c0df2; -fx-text-fill: white; -fx-font-weight: 600; " +
                      "-fx-padding: 8 16 8 16; -fx-border-radius: 8; -fx-cursor: hand;");
        edit.setOnAction(e -> showEditJobDialog(job));
        
        Button delete = new Button("🗑️ Delete");
        delete.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: 600; " +
                        "-fx-padding: 8 16 8 16; -fx-border-radius: 8; -fx-cursor: hand;");
        delete.setOnAction(e -> deleteJob(job));
        
        Button apply = new Button("Apply Now"); 
        apply.getStyleClass().add("job-apply-btn");
        apply.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: 600; " +
                       "-fx-padding: 8 16 8 16; -fx-border-radius: 8; -fx-cursor: hand;");
        apply.setOnAction(e -> handleApplyJob(job));
        
        // Only show edit/delete if user is the owner or admin
        if (App.currentUser != null && (App.currentUser.getId() == job.getUserId() || isAdmin())) {
            footer.getChildren().addAll(posted, spacer, edit, delete, apply);
        } else {
            footer.getChildren().addAll(posted, spacer, save, apply);
        }
        
        card.getChildren().add(footer);

        // Hover lift + subtle glow
        DropShadow hoverShadow = new DropShadow(22, Color.web("#6c0df2", 0.12));
        card.setOnMouseEntered(ev -> {
            // lift
            TranslateTransition lift = new TranslateTransition(Duration.millis(160), card);
            lift.setToY(-6);
            lift.play();
            card.setEffect(hoverShadow);
        });

        card.setOnMouseExited(ev -> {
            TranslateTransition down = new TranslateTransition(Duration.millis(160), card);
            down.setToY(0);
            down.play();
            card.setEffect(null);
        });

        return card;
    }

    private String formatPostedDate(LocalDate date) {
        long days = ChronoUnit.DAYS.between(date,LocalDate.now());
        if(days==0) return "today";
        if(days==1) return "yesterday";
        if(days<7) return days+" days ago";
        if(days<30) return (days/7)+" weeks ago";
        return (days/30)+" months ago";
    }

    // ==================== CREATE ====================
    @FXML
    private void showAddJobDialog() {
        if(App.currentUser==null){ showAlert("Authentication Required","Please log in to add jobs"); return; }
        
        Dialog<JobModel> dialog = new Dialog<>();
        dialog.setTitle("Add New Job");
        dialog.setHeaderText("Create a new job posting");

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialogPane.setStyle("-fx-font-size: 12;");

        VBox content = createJobForm(null);
        dialogPane.setContent(content);

        javafx.scene.Node okButton = dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, evt -> {
            JobModel candidate = extractJobFormData(content, null);
            if (candidate.getTitle() == null || candidate.getTitle().trim().length() < 3
                    || candidate.getCompany() == null || candidate.getCompany().trim().length() < 3
                    || candidate.getLocation() == null || candidate.getLocation().trim().length() < 3
                    || candidate.getDescription() == null || candidate.getDescription().trim().length() < 20
                    || candidate.getSalaryRange() == null || candidate.getSalaryRange().trim().isEmpty()) {
                evt.consume();
                showAlert("Validation Error", "Please provide valid Title, Company, Location (min 3 chars), Description (min 20 chars), and Salary (integer).");
            }
        });

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
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
                showAlert("Database Error", "Unable to connect to database");
                return;
            }

            String query = "INSERT INTO jobs (title, company, location, description, category, salary_range, job_type, posted_date, requirements, user_id) " +
                          "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, job.getTitle());
            stmt.setString(2, job.getCompany());
            stmt.setString(3, job.getLocation());
            stmt.setString(4, job.getDescription());
            stmt.setString(5, job.getCategory());
            stmt.setString(6, job.getSalaryRange());
            stmt.setString(7, job.getJobType());
            stmt.setDate(8, java.sql.Date.valueOf(job.getPostedDate()));
            stmt.setString(9, String.join(", ", job.getRequirements()));
            stmt.setInt(10, App.currentUser != null ? App.currentUser.getId() : 1);

            stmt.executeUpdate();
            stmt.close();
            conn.close();

            showAlert("Success", "Job created successfully!");
            loadJobsFromDB();
            displayJobs(filteredJobs);

        } catch (SQLException e) {
            showAlert("Database Error", "Error creating job: " + e.getMessage());
        }
    }

    // ==================== UPDATE ====================
    private void showEditJobDialog(JobModel job) {
        if(App.currentUser==null || (App.currentUser.getId() != job.getUserId() && !isAdmin())) {
            showAlert("Permission Denied", "You can only edit your own jobs");
            return;
        }
        
        Dialog<JobModel> dialog = new Dialog<>();
        dialog.setTitle("Edit Job");
        dialog.setHeaderText("Edit job posting: " + job.getTitle());

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialogPane.setStyle("-fx-font-size: 12;");

        VBox content = createJobForm(job);
        dialogPane.setContent(content);

        javafx.scene.Node okButton = dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, evt -> {
            JobModel candidate = extractJobFormData(content, job);
            if (candidate.getTitle() == null || candidate.getTitle().trim().length() < 3
                    || candidate.getCompany() == null || candidate.getCompany().trim().length() < 3
                    || candidate.getLocation() == null || candidate.getLocation().trim().length() < 3
                    || candidate.getDescription() == null || candidate.getDescription().trim().length() < 20
                    || candidate.getSalaryRange() == null || candidate.getSalaryRange().trim().isEmpty()) {
                evt.consume();
                showAlert("Validation Error", "Please provide valid Title, Company, Location (min 3 chars), Description (min 20 chars), and Salary (integer).");
            }
        });

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
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
                showAlert("Database Error", "Unable to connect to database");
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

            showAlert("Success", "Job updated successfully!");
            loadJobsFromDB();
            displayJobs(filteredJobs);

        } catch (SQLException e) {
            showAlert("Database Error", "Error updating job: " + e.getMessage());
        }
    }

    // ==================== DELETE ====================
    private void deleteJob(JobModel job) {
        if(App.currentUser==null || (App.currentUser.getId() != job.getUserId() && !isAdmin())) {
            showAlert("Permission Denied", "You can only delete your own jobs");
            return;
        }
        
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Delete Job");
        confirmAlert.setHeaderText("Confirm Deletion");
        confirmAlert.setContentText("Are you sure you want to delete this job?\n\n" + job.getTitle());

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                Connection conn = MyDataBase.getConnection();
                if (conn == null) {
                    showAlert("Database Error", "Unable to connect to database");
                    return;
                }

                String query = "DELETE FROM jobs WHERE id=?";
                PreparedStatement stmt = conn.prepareStatement(query);
                stmt.setInt(1, job.getId());
                stmt.executeUpdate();
                stmt.close();
                conn.close();

                showAlert("Success", "Job deleted successfully!");
                loadJobsFromDB();
                displayJobs(filteredJobs);

            } catch (SQLException e) {
                showAlert("Database Error", "Error deleting job: " + e.getMessage());
            }
        }
    }

    // ==================== FORM HELPERS ====================
    private VBox createJobForm(JobModel editingJob) {
        VBox form = new VBox(12);
        form.setStyle("-fx-padding: 20;");

        TextField titleField = createFormField("Job Title", editingJob != null ? editingJob.getTitle() : "");
        TextField companyField = createFormField("Company Name", editingJob != null ? editingJob.getCompany() : "");
        TextField locationField = createFormField("Location", editingJob != null ? editingJob.getLocation() : "");
        TextField categoryField = createFormField("Category", editingJob != null ? editingJob.getCategory() : "");
        TextField salaryField = createIntegerSalaryField("Salary (in TND)", editingJob != null ? editingJob.getSalaryRange() : "");
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

        form.setUserData(new Object[]{titleField, companyField, locationField, categoryField, salaryField, typeField, descArea, reqArea});

        form.getChildren().addAll(
            createFormSection("Title", titleField),
            createFormSection("Company", companyField),
            createFormSection("Location", locationField),
            createFormSection("Category", categoryField),
            createFormSection("Salary (TND)", salaryField),
            createFormSection("Job Type", typeField)
        );

        return form;
    }

    private HBox createFormSection(String label, TextField field) {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);
        Label labelNode = new Label(label + ":");
        labelNode.setPrefWidth(120);
        labelNode.setStyle("-fx-font-weight: 700;");
        HBox.setHgrow(field, Priority.ALWAYS);
        field.setStyle("-fx-padding: 8 12 8 12; -fx-border-color: #e5e7eb; -fx-border-radius: 8; -fx-background-radius: 8;");
        box.getChildren().addAll(labelNode, field);
        return box;
    }

    private TextField createFormField(String label, String value) {
        TextField field = new TextField(value);
        field.setStyle("-fx-padding: 8 12 8 12; -fx-border-color: #e5e7eb; -fx-border-radius: 8; -fx-background-radius: 8;");
        field.setPromptText(label);
        return field;
    }

    private TextField createIntegerSalaryField(String label, String value) {
        TextField field = new TextField(value);
        field.setStyle("-fx-padding: 8 12 8 12; -fx-border-color: #e5e7eb; -fx-border-radius: 8; -fx-background-radius: 8; -fx-focus-color: #6c0df2;");
        field.setPromptText(label);
        
        field.setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.isEmpty() || newText.matches("\\d+")) {
                return change;
            }
            return null;
        }));
        
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
            original != null ? original.getPostedDate() : LocalDate.now(),
            requirements,
            App.currentUser != null ? App.currentUser.getId() : 1
        );

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

        String[] requirements = !textAreas.isEmpty() && textAreas.size() > 1 ? 
            textAreas.get(1).getText().split("\n") : new String[0];

        return new JobModel(
            original != null ? original.getId() : 0,
            textFields.size() > 0 ? textFields.get(0).getText() : "",
            textFields.size() > 1 ? textFields.get(1).getText() : "",
            textFields.size() > 2 ? textFields.get(2).getText() : "",
            textAreas.size() > 0 ? textAreas.get(0).getText() : "",
            textFields.size() > 3 ? textFields.get(3).getText() : "",
            textFields.size() > 4 ? textFields.get(4).getText() : "",
            textFields.size() > 5 ? textFields.get(5).getText() : "",
            original != null ? original.getPostedDate() : LocalDate.now(),
            requirements,
            App.currentUser != null ? App.currentUser.getId() : 1
        );
    }

    private boolean isAdmin() {
        // Check if user is admin (you may need to update this based on your User model)
        return App.currentUser != null && App.currentUser.getId() == 1; // Assuming ID 1 is admin
    }
    private void handleApplyJob(JobModel job) {
        if(App.currentUser==null){ showAlert("Authentication Required","Please log in to apply"); return; }
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/khademni/JobApplications.fxml"));
            BorderPane root = loader.load();

            JobApplicationController controller = loader.getController();
            controller.setSelectedJob(job);
            controller.openAddApplicationDialog();

            Stage stage = new Stage();
            stage.setTitle("Apply for: " + job.getTitle());
            stage.setScene(new Scene(root, 900, 600));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();
        } catch (IOException e) {
            showAlert("Error", "Failed to open application form: " + e.getMessage());
        }
    }

    private void handleSaveJob(JobModel job) {
        if(App.currentUser==null){ showAlert("Authentication Required","Please log in to save jobs"); return; }
        showAlert("Job Saved",job.getTitle()+" has been added to your saved jobs");
    }

    private void showAlert(String title,String msg){
        Alert a=new Alert(Alert.AlertType.INFORMATION); a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
}
