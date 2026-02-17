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

    @FXML
    private TextField jobSearchField;
    @FXML
    private ComboBox<String> jobCategoryFilter;
    @FXML
    private ComboBox<String> jobLocationFilter;
    @FXML
    private Slider jobSalarySlider;
    @FXML
    private Label salaryFilterLabel;
    @FXML
    private VBox jobsContainer;
    @FXML
    private VBox emptyStateBox;

    private List<JobModel> allJobs = new ArrayList<>();
    private List<JobModel> filteredJobs = new ArrayList<>();

    @FXML
    public void initialize() {
        System.out.println("DEBUG: JobsController initializing...");
        try {
            setupComboBoxes();
            System.out.println("DEBUG: ComboBoxes setup done");
            loadJobsFromDB();
            System.out.println("DEBUG: Jobs loaded");
            setupSalarySlider();
            System.out.println("DEBUG: Salary Slider setup done");
            displayJobs(filteredJobs);
            System.out.println("DEBUG: Jobs displayed");
        } catch (Exception e) {
            System.err.println("DEBUG: Error in JobsController initialize: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupComboBoxes() {
        jobCategoryFilter.getItems().addAll("All Categories", "Software Development", "Design", "Marketing", "Sales",
                "Business", "HR", "Finance");
        jobCategoryFilter.setValue("All Categories");

        jobLocationFilter.getItems().addAll("All Locations", "Tunisia", "Remote", "Tunis", "Sfax", "Sousse", "Gabes");
        jobLocationFilter.setValue("All Locations");

        jobCategoryFilter.setOnAction(e -> applyFilters());
        jobLocationFilter.setOnAction(e -> applyFilters());

        // Setup smart search with autocomplete
        setupSmartSearch();

        // Slider setup logic moved to separate method
    }

    /**
     * Sets up smart search with autocomplete and live filtering
     */
    private void setupSmartSearch() {
        // Live search - update results as user types
        jobSearchField.textProperty().addListener((observable, oldValue, newValue) -> {
            applyFilters(); // Automatically filter as user types
        });

        // Add autocomplete suggestions (optional enhancement)
        // This will show suggestions based on existing job titles and companies
        jobSearchField.setOnKeyReleased(event -> {
            String currentText = jobSearchField.getText().toLowerCase().trim();
            if (currentText.length() >= 2) { // Start suggesting after 2 characters
                showSearchSuggestions(currentText);
            }
        });
    }

    /**
     * Shows smart search suggestions based on job titles and companies
     */
    private void showSearchSuggestions(String query) {
        // Collect unique suggestions from job titles and companies
        java.util.Set<String> suggestions = new java.util.LinkedHashSet<>();

        for (JobModel job : allJobs) {
            String title = job.getTitle().toLowerCase();
            String company = job.getCompany().toLowerCase();

            // Add matching job titles
            if (title.contains(query)) {
                suggestions.add(job.getTitle());
            }

            // Add matching companies
            if (company.contains(query)) {
                suggestions.add(job.getCompany());
            }

            // Limit to top 5 suggestions
            if (suggestions.size() >= 5)
                break;
        }

        // Note: For full autocomplete dropdown, you would need to use a custom control
        // or third-party library like ControlsFX AutoCompleteTextField
        // For now, the live search provides instant results
    }

    private void setupSalarySlider() {
        // Find max salary in DB or use fixed 10000 per user request
        int maxSalary = 10000;

        jobSalarySlider.setMin(0);
        jobSalarySlider.setMax(maxSalary);
        jobSalarySlider.setValue(0);
        jobSalarySlider.setBlockIncrement(100);
        jobSalarySlider.setMajorTickUnit(1000);

        updateSalaryLabel((int) jobSalarySlider.getValue());

        jobSalarySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateSalaryLabel(newVal.intValue());
            applyFilters();
        });
    }

    private void updateSalaryLabel(int value) {
        salaryFilterLabel.setText("Min Salary: " + value + " TND");
    }

    private int extractMaxSalary(String salaryRange) {
        if (salaryRange == null)
            return 0;
        // Simple regex to find the last number in the string which is usually the max
        // E.g. "2000 - 3500" -> 3500. "3500+" -> 3500. "1500" -> 1500.
        try {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)").matcher(salaryRange);
            int max = 0;
            while (m.find()) {
                int val = Integer.parseInt(m.group(1));
                if (val > max)
                    max = val;
            }
            return max;
        } catch (Exception e) {
            return 0;
        }
    }

    // ==================== lehna partie mtaa db ====================
    private void loadJobsFromDB() {
        allJobs.clear();
        filteredJobs.clear();
        try (Connection conn = MyDataBase.getConnection()) {
            if (conn == null) {
                loadSampleJobs();
                return;
            }

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
        String[] requirements = rs.getString("requirements") != null ? rs.getString("requirements").split(",\\s*")
                : new String[0];

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
                rs.getInt("user_id"));
    }

    private void loadSampleJobs() {
        allJobs.add(new JobModel(1, "Senior Java Developer", "Tech Innovators Inc", "Tunis",
                "We are looking for an experienced Java developer to join our team...",
                "Software Development", "2000 - 3500 TND", "Full-time", LocalDate.now().minusDays(2),
                new String[] { "Java 17+", "Spring Boot", "MySQL", "REST APIs" }, 1));

        allJobs.add(new JobModel(2, "UI/UX Designer", "Creative Studio", "Remote",
                "Join our design team to create stunning user experiences...",
                "Design", "1500 - 2500 TND", "Full-time", LocalDate.now().minusDays(5),
                new String[] { "Figma", "Adobe XD", "Prototyping" }, 2));

        filteredJobs.addAll(allJobs);
    }

    // ==================== hethi partie mtaa search wl filtres ====================
    @FXML
    private void performSearch() {
        applyFilters();
    }

    @FXML
    private void resetFilters() {
        jobSearchField.clear();
        jobCategoryFilter.setValue("All Categories");
        jobLocationFilter.setValue("All Locations");
        jobSalarySlider.setValue(0);
        filteredJobs.clear();
        filteredJobs.addAll(allJobs);
        displayJobs(filteredJobs);
    }

    private void applyFilters() {
        String searchQuery = jobSearchField.getText().toLowerCase().trim();
        String selectedCategory = jobCategoryFilter.getValue();
        String selectedLocation = jobLocationFilter.getValue();
        int minSalary = (int) jobSalarySlider.getValue();

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

            // Salary Filter: Check if max salary of job >= slider min
            int jobMaxSalary = extractMaxSalary(job.getSalaryRange());
            boolean matchesSalary = jobMaxSalary >= minSalary;

            if (matchesSearch && matchesCategory && matchesLocation && matchesSalary)
                filteredJobs.add(job);
        }

        displayJobs(filteredJobs);
    }

    // ==================== houni el display ====================
    private void displayJobs(List<JobModel> jobs) {
        jobsContainer.getChildren().clear();
        emptyStateBox.setVisible(jobs.isEmpty());

        int idx = 0;
        for (JobModel job : jobs) {
            VBox card = createJobCard(job);

            // hethi partie mtaa animation w animation

            card.setOpacity(0);
            card.setTranslateY(12);
            jobsContainer.getChildren().add(card);

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
        type.setStyle(
                "-fx-background-color: #f3e8ff; -fx-text-fill: #6c0df2; -fx-padding: 4 8 4 8; -fx-border-radius: 6; -fx-font-size: 11; -fx-font-weight: 600;");

        String fullLocation = job.getLocation();
        String displayLocation = fullLocation;
        if (fullLocation != null && fullLocation.length() > 35) {
            displayLocation = fullLocation.substring(0, 32) + "...";
        }
        Label loc = new Label("📍 " + displayLocation);
        if (fullLocation != null) {
            Tooltip locTooltip = new Tooltip(fullLocation);
            locTooltip.setStyle("-fx-font-size: 13px;");
            loc.setTooltip(locTooltip);
        }
        loc.getStyleClass().add("job-location-tag");
        loc.setStyle("-fx-text-fill: #666666; -fx-font-size: 12;");

        Label salary = new Label("💰 " + job.getSalaryRange() + " TND");
        salary.getStyleClass().add("job-salary-tag");
        salary.setStyle("-fx-text-fill: #666666; -fx-font-size: 12;");

        Label category = new Label("📌 " + job.getCategory());
        category.getStyleClass().add("job-category-tag");
        category.setStyle("-fx-text-fill: #666666; -fx-font-size: 12;");

        meta.getChildren().addAll(type, loc, salary, category);
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

        // t affichi edit/delete if user is the owner or admin
        if (App.currentUser != null && (App.currentUser.getId() == job.getUserId() || isAdmin())) {
            footer.getChildren().addAll(posted, spacer, edit, delete, apply);
        } else {
            footer.getChildren().addAll(posted, spacer, save, apply);
        }

        card.getChildren().add(footer);

        DropShadow hoverShadow = new DropShadow(22, Color.web("#6c0df2", 0.12));
        card.setOnMouseEntered(ev -> {

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
        long days = ChronoUnit.DAYS.between(date, LocalDate.now());
        if (days == 0)
            return "today";
        if (days == 1)
            return "yesterday";
        if (days < 7)
            return days + " days ago";
        if (days < 30)
            return (days / 7) + " weeks ago";
        return (days / 30) + " months ago";
    }

    // ==================== partie mtaa create job ====================
    @FXML
    private void showAddJobDialog() {

        if (App.currentUser == null) {
            showAlert("Authentication Required", "Please log in to add jobs.");
            return;
        }

        // creation de dialog
        Dialog<JobModel> dialog = new Dialog<>();
        dialog.setTitle("Add New Job");
        dialog.setHeaderText("Create a new job posting");

        DialogPane dialogPane = dialog.getDialogPane();

        // design
        dialogPane.getStylesheets().add(
                getClass().getResource("/com/khademni/dialogs.css").toExternalForm());
        dialogPane.getStyleClass().add("job-dialog");

        // Boutons
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Contenu (Form Helper)
        VBox content = createJobForm(null);
        content.getStyleClass().add("job-form");
        dialogPane.setContent(content);

        // tvalidation 9bal m tsaker
        javafx.scene.Node okButton = dialogPane.lookupButton(ButtonType.OK);
        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {

            JobModel job = extractJobFormData(content, null);

            if (!isJobValid(job)) {
                event.consume();
                showAlert(
                        "Validation Error",
                        """
                                Please check the following:
                                • Title / Company / Location: minimum 3 characters
                                • Description: minimum 20 characters
                                • Salary: required
                                """);
            }
        });

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                return extractJobFormData(content, null);
            }
            return null;
        });

        Optional<JobModel> result = dialog.showAndWait();
        result.ifPresent(this::insertJobToDB);
    }

    private boolean isJobValid(JobModel job) {
        return job != null
                && job.getTitle() != null && job.getTitle().trim().length() >= 3
                && job.getCompany() != null && job.getCompany().trim().length() >= 3
                && job.getLocation() != null && job.getLocation().trim().length() >= 3
                && job.getDescription() != null && job.getDescription().trim().length() >= 20
                && job.getSalaryRange() != null && !job.getSalaryRange().trim().isEmpty();
    }

    private void insertJobToDB(JobModel job) {
        try {
            Connection conn = MyDataBase.getConnection();
            if (conn == null) {
                showAlert("Database Error", "Unable to connect to database");
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

    // ==================== EL UPDATE ====================
    private void showEditJobDialog(JobModel job) {
        if (App.currentUser == null || (App.currentUser.getId() != job.getUserId() && !isAdmin())) {
            showAlert("Permission Denied", "You can only edit your own jobs");
            return;
        }

        Dialog<JobModel> dialog = new Dialog<>();
        dialog.setTitle("Edit Job");
        dialog.setHeaderText("Edit job posting: " + job.getTitle());

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStylesheets().add(
                getClass().getResource("/com/khademni/dialogs.css").toExternalForm());
        dialogPane.getStyleClass().add("job-dialog");
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
                showAlert("Validation Error",
                        "Please provide valid Title, Company, Location (min 3 chars), Description (min 20 chars), and Salary (integer).");
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

    // ==================== EL DELETE ====================
    private void deleteJob(JobModel job) {
        if (App.currentUser == null || (App.currentUser.getId() != job.getUserId() && !isAdmin())) {
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

    // ==================== partie forum helper====================
    private VBox createJobForm(JobModel editingJob) {
        VBox form = new VBox(12);
        form.setStyle("-fx-padding: 20;");

        TextField titleField = createFormField("Job Title", editingJob != null ? editingJob.getTitle() : "");
        TextField companyField = createFormField("Company Name", editingJob != null ? editingJob.getCompany() : "");

        // Location Autocomplete with Map Button
        TextField locationField = createFormField("Location", editingJob != null ? editingJob.getLocation() : "");
        HBox.setHgrow(locationField, Priority.ALWAYS);
        setupLocationAutocomplete(locationField);

        Button mapBtn = new Button("🌍 Map");
        mapBtn.setStyle(
                "-fx-background-color: #e0f2fe; -fx-text-fill: #0284c7; -fx-font-weight: bold; -fx-cursor: hand;");
        mapBtn.setOnAction(e -> showMapPickerDialog(locationField));

        HBox locationBox = new HBox(5, locationField, mapBtn);
        locationBox.setAlignment(Pos.CENTER_LEFT);

        // Category ComboBox
        ComboBox<String> categoryCombo = new ComboBox<>();
        categoryCombo.getItems().addAll("Software Development", "Design", "Marketing", "Sales", "Business", "HR",
                "Finance", "Other");
        categoryCombo.setValue(editingJob != null ? editingJob.getCategory() : "Software Development");
        categoryCombo.setMaxWidth(Double.MAX_VALUE);
        categoryCombo
                .setStyle("-fx-padding: 8; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #e5e7eb;");

        // Salary Slider
        int initialSalary = 0;
        if (editingJob != null && editingJob.getSalaryRange() != null) {
            try {
                // Try to parse existing salary if it's just a number
                initialSalary = Integer.parseInt(editingJob.getSalaryRange().replaceAll("[^0-9]", ""));
            } catch (NumberFormatException e) {
                // ignore
            }
        }

        Slider salarySlider = new Slider(0, 5000, initialSalary);
        salarySlider.setShowTickLabels(true);
        salarySlider.setShowTickMarks(true);
        salarySlider.setMajorTickUnit(1000);
        salarySlider.setBlockIncrement(100);

        Label salaryLabel = new Label("Salary: " + (int) salarySlider.getValue() + " TND");
        salaryLabel.setStyle("-fx-font-weight: 700;");

        salarySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            salaryLabel.setText("Salary: " + newVal.intValue() + " TND");
        });

        VBox salaryBox = new VBox(5, salaryLabel, salarySlider);

        // Job Type ComboBox
        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("Full-time", "Part-time", "Freelance", "Internship", "Contract");
        typeCombo.setValue(editingJob != null ? editingJob.getJobType() : "Full-time");
        typeCombo.setMaxWidth(Double.MAX_VALUE);
        typeCombo
                .setStyle("-fx-padding: 8; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #e5e7eb;");

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

        // Store references for extraction
        form.setUserData(new Object[] { titleField, companyField, locationField, categoryCombo, salarySlider, typeCombo,
                descArea, reqArea });

        form.getChildren().addAll(
                createFormSection("Title", titleField),
                createFormSection("Company", companyField),
                createFormSection("Location", locationBox),
                createFormSection("Category", categoryCombo),
                // Salary section is self-contained so we add it directly mostly
                salaryBox,
                createFormSection("Job Type", typeCombo));

        return form;
    }

    private void showMapPickerDialog(TextField locationField) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Pick Location");
        dialog.setHeaderText("Click on the map to select location");

        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().add(ButtonType.CLOSE);
        pane.setPrefSize(800, 600);

        javafx.scene.web.WebView webView = new javafx.scene.web.WebView();
        javafx.scene.web.WebEngine engine = webView.getEngine();

        // Simple HTML for Leaflet Map
        String html = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <link rel="stylesheet" href="https://unpkg.com/leaflet@1.7.1/dist/leaflet.css" />
                        <script src="https://unpkg.com/leaflet@1.7.1/dist/leaflet.js"></script>
                        <style>
                            body, html, #map { margin: 0; padding: 0; height: 100%; width: 100%; }
                        </style>
                    </head>
                    <body>
                        <div id="map"></div>
                        <script>
                            var map = L.map('map').setView([36.8065, 10.1815], 10); // Tunis center
                            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                                attribution: '&copy; OpenStreetMap contributors'
                            }).addTo(map);

                            var marker;

                            map.on('click', function(e) {
                                var lat = e.latlng.lat;
                                var lon = e.latlng.lng;

                                if (marker) map.removeLayer(marker);
                                marker = L.marker([lat, lon]).addTo(map);

                // Reverse Geocoding via Nominatim
                                fetch(`https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lon}`)
                                    .then(response => response.json())
                                    .then(data => {
                                        // Use full address for precision
                                        var fullAddress = data.display_name;
                                        // Call Java
                                        if (window.javaObj) {
                                            window.javaObj.setLocation(fullAddress);
                                        } else {
                                            console.log("Java object not found");
                                        }
                                    });
                            });
                        </script>
                    </body>
                    </html>
                """;

        engine.loadContent(html);

        // Register bridge when page loads
        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                netscape.javascript.JSObject window = (netscape.javascript.JSObject) engine.executeScript("window");
                window.setMember("javaObj", new MapBridge(locationField, dialog));
            }
        });

        pane.setContent(webView);
        dialog.showAndWait();

    }

    private void setupLocationAutocomplete(TextField locationField) {
        ContextMenu suggestions = new ContextMenu();

        // Debounce timer
        PauseTransition pause = new PauseTransition(Duration.millis(400));

        pause.setOnFinished(event -> {
            String query = locationField.getText().trim();
            if (query.length() < 3)
                return;

            fetchSuggestions(query, suggestions, locationField);
        });

        locationField.textProperty().addListener((obs, oldVal, newVal) -> {
            suggestions.hide();
            if (newVal == null || newVal.length() < 3) {
                return;
            }
            pause.playFromStart();
        });

        locationField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused)
                suggestions.hide();
        });
    }

    private void fetchSuggestions(String query, ContextMenu suggestions, TextField locationField) {
        // Run in background thread
        new Thread(() -> {
            try {
                String encodedQuery = java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8);
                // Viewbox for Tunisia (approx): 7.5E, 30N to 11.6E, 37.5N
                // bounded=0 means prioritize this area but search globally if needed
                String url = "https://nominatim.openstreetmap.org/search?q=" + encodedQuery
                        + "&format=json&limit=5&addressdetails=1"
                        + "&viewbox=7.5,37.6,11.6,30.2&bounded=0";

                java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create(url))
                        .header("User-Agent", "KhademniApp/1.0")
                        .build();

                java.net.http.HttpResponse<String> response = client.send(request,
                        java.net.http.HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    List<String> results = parseNominatimResponse(response.body());

                    javafx.application.Platform.runLater(() -> {
                        if (results.isEmpty())
                            return;

                        List<CustomMenuItem> items = new ArrayList<>();
                        for (String place : results) {
                            // Customize the look to be Google-style
                            Label icon = new Label("📍");
                            icon.setStyle("-fx-font-size: 16px; -fx-padding: 0 8 0 0;");

                            Label text = new Label(place);
                            text.setStyle("-fx-font-size: 13px; -fx-text-fill: #333;");
                            text.setWrapText(true);
                            text.setMaxWidth(400); // Prevent overly wide menus

                            HBox content = new HBox(icon, text);
                            content.setAlignment(Pos.CENTER_LEFT);
                            content.setPadding(new javafx.geometry.Insets(5, 10, 5, 10));

                            CustomMenuItem item = new CustomMenuItem(content);
                            item.setHideOnClick(true);
                            item.setOnAction(e -> {
                                locationField.setText(place);
                                locationField.positionCaret(place.length());
                                suggestions.hide();
                            });
                            items.add(item);
                        }

                        suggestions.getItems().setAll(items);
                        if (!suggestions.isShowing() && locationField.isFocused()) {
                            suggestions.show(locationField, javafx.geometry.Side.BOTTOM, 0, 0);
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private List<String> parseNominatimResponse(String json) {
        List<String> results = new ArrayList<>();
        try {
            // Simple regex parsing to avoid adding JSON dependency
            // Look for "display_name":"..."
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"display_name\":\"([^\"]+)\"").matcher(json);
            while (m.find()) {
                // Unicode unescaping might be needed but usually raw string works for basic
                // display
                String name = m.group(1);
                // Clean up unicode escapes if any simple ones
                results.add(name);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }

    private HBox createFormSection(String label, javafx.scene.Node field) {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);
        Label labelNode = new Label(label + ":");
        labelNode.setPrefWidth(120);
        labelNode.setStyle("-fx-font-weight: 700;");
        HBox.setHgrow(field, Priority.ALWAYS);

        // Style handled in creation

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

    private void addFormLabel(String label, VBox container) {
        Label l = new Label(label + ":");
        l.setStyle("-fx-font-weight: 700;");
        container.getChildren().add(l);
    }

    private JobModel extractJobFormData(VBox form, JobModel original) {
        Object[] fields = (Object[]) form.getUserData();
        if (fields == null || fields.length < 8) {
            // Fallback for safety, though standard flow goes through here
            return null;
        }

        TextField titleField = (TextField) fields[0];
        TextField companyField = (TextField) fields[1];
        TextField locationField = (TextField) fields[2];
        ComboBox<String> categoryCombo = (ComboBox<String>) fields[3];
        Slider salarySlider = (Slider) fields[4];
        ComboBox<String> typeCombo = (ComboBox<String>) fields[5];
        TextArea descArea = (TextArea) fields[6];
        TextArea reqArea = (TextArea) fields[7];

        String[] requirements = reqArea.getText().split("\n");

        String salaryValue = String.valueOf((int) salarySlider.getValue());

        JobModel job = new JobModel(
                original != null ? original.getId() : 0,
                titleField.getText(),
                companyField.getText(),
                locationField.getText(),
                descArea.getText(),
                categoryCombo.getValue(),
                salaryValue,
                typeCombo.getValue(),
                original != null ? original.getPostedDate() : LocalDate.now(),
                requirements,
                App.currentUser != null ? App.currentUser.getId() : 1);

        return job;
    }

    private boolean isAdmin() {
        return App.currentUser != null && App.currentUser.getId() == 1;
    }

    private void handleApplyJob(JobModel job) {
        if (App.currentUser == null) {
            showAlert("Authentication Required", "Please log in to apply");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/khademni/JobApplications.fxml"));
            BorderPane root = loader.load();

            JobApplicationController controller = loader.getController();
            controller.setSelectedJob(job);

            // Logic moved to JobApplicationController.initializeView()
            // controller.openAddApplicationDialog();

            Stage stage = new Stage();
            stage.setTitle("Job Applications: " + job.getTitle());
            stage.setScene(new Scene(root, 900, 600));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();
        } catch (IOException e) {
            showAlert("Error", "Failed to open application form: " + e.getMessage());
        }
    }

    private void handleSaveJob(JobModel job) {
        if (App.currentUser == null) {
            showAlert("Authentication Required", "Please log in to save jobs");
            return;
        }
        showAlert("Job Saved", job.getTitle() + " has been added to your saved jobs");
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    // Public static class for JS bridge
    public static class MapBridge {
        private final TextField locationField;
        private final Dialog<String> dialog;

        public MapBridge(TextField locationField, Dialog<String> dialog) {
            this.locationField = locationField;
            this.dialog = dialog;
        }

        public void setLocation(String loc) {
            javafx.application.Platform.runLater(() -> {
                locationField.setText(loc);
                dialog.setResult(loc);
                dialog.close();
            });
        }
    }
}
