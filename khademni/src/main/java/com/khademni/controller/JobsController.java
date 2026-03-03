package com.khademni.controller;

import com.khademni.App;
import com.khademni.model.JobModel;
import com.khademni.utils.AIService;
import com.khademni.utils.MyDataBase;
import com.khademni.utils.ConversationHelper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
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
import javafx.event.ActionEvent;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.AbstractMap;

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

    // --- new UI fields for saved jobs tab ---
    // saved jobs functionality uses a separate window opened via button

    private List<JobModel> allJobs = new ArrayList<>();
    private List<JobModel> filteredJobs = new ArrayList<>();
    private ContextMenu autocompleteMenu;
    private PauseTransition autocompleteDebounce;

    @FXML
    public void initialize() {
        System.out.println("DEBUG: JobsController initializing...");
        try {
            setupComboBoxes();
            System.out.println("DEBUG: ComboBoxes setup done");

            // guarantee saved_jobs table is ready before any save operations
            ensureSavedJobsTableExists();

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
        // Initialize autocomplete menu
        autocompleteMenu = new ContextMenu();
        autocompleteMenu.setStyle("-fx-padding: 0; -fx-border-radius: 8;");

        // Initialize debounce timer for autocomplete
        autocompleteDebounce = new PauseTransition(Duration.millis(300));
        autocompleteDebounce.setOnFinished(event -> {
            String currentText = jobSearchField.getText().toLowerCase().trim();
            if (currentText.length() >= 1) {
                showSearchSuggestions(currentText);
            } else {
                autocompleteMenu.hide();
            }
        });

        // Live search - update results as user types
        jobSearchField.textProperty().addListener((observable, oldValue, newValue) -> {
            autocompleteDebounce.playFromStart();
            applyFilters(); // Automatically filter as user types
        });

        // Handle keyboard navigation in autocomplete
        jobSearchField.setOnKeyPressed(event -> {
            if (!autocompleteMenu.isShowing())
                return;

            switch (event.getCode()) {
                case ESCAPE:
                    autocompleteMenu.hide();
                    event.consume();
                    break;
                default:
                    break;
            }
        });

        // Hide autocomplete when focus is lost
        jobSearchField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                javafx.application.Platform.runLater(() -> autocompleteMenu.hide());
            }
        });
    }

    /**
     * Shows intelligent search suggestions based on job titles, companies,
     * categories, and locations
     */
    private class SuggestionItem implements Comparable<SuggestionItem> {
        String text;
        String type; // "title", "company", "category", "location"
        int matchScore; // Higher = better match
        int jobCount; // Number of jobs with this suggestion

        SuggestionItem(String text, String type, int matchScore, int jobCount) {
            this.text = text;
            this.type = type;
            this.matchScore = matchScore;
            this.jobCount = jobCount;
        }

        @Override
        public int compareTo(SuggestionItem other) {
            // Sort by score first (descending), then by job count (descending)
            if (this.matchScore != other.matchScore) {
                return other.matchScore - this.matchScore;
            }
            return other.jobCount - this.jobCount;
        }
    }

    private void showSearchSuggestions(String query) {
        // First, get jobs that match category, location, and salary filters
        String selectedCategory = jobCategoryFilter.getValue();
        String selectedLocation = jobLocationFilter.getValue();
        int minSalary = (int) jobSalarySlider.getValue();

        List<JobModel> availableJobs = allJobs.stream()
                .filter(job -> {
                    boolean matchesCategory = selectedCategory.equals("All Categories") ||
                            job.getCategory().equals(selectedCategory);
                    boolean matchesLocation = selectedLocation.equals("All Locations") ||
                            job.getLocation().equals(selectedLocation);
                    int jobMaxSalary = extractMaxSalary(job.getSalaryRange());
                    boolean matchesSalary = jobMaxSalary >= minSalary;
                    return matchesCategory && matchesLocation && matchesSalary;
                })
                .collect(java.util.stream.Collectors.toList());

        // Now create suggestions only from available jobs
        List<SuggestionItem> suggestions = availableJobs.stream()
                .flatMap(job -> {
                    java.util.List<AbstractMap.SimpleEntry<String, String>> entries = new java.util.ArrayList<>();

                    // Title matches
                    if (job.getTitle().toLowerCase().contains(query)) {
                        entries.add(new AbstractMap.SimpleEntry<>(job.getTitle(), "title"));
                    }

                    // Company matches
                    if (job.getCompany().toLowerCase().contains(query)) {
                        entries.add(new AbstractMap.SimpleEntry<>(job.getCompany(), "company"));
                    }

                    // User name matches
                    if (job.getUserName() != null && job.getUserName().toLowerCase().contains(query)) {
                        entries.add(new AbstractMap.SimpleEntry<>(job.getUserName(), "user"));
                    }

                    return entries.stream();
                })
                .collect(java.util.stream.Collectors.groupingBy(
                        AbstractMap.SimpleEntry::getKey,
                        java.util.stream.Collectors.mapping(
                                AbstractMap.SimpleEntry::getValue,
                                java.util.stream.Collectors.collectingAndThen(
                                        java.util.stream.Collectors.toList(),
                                        list -> new AbstractMap.SimpleEntry<>(list.get(0), list.size())))))
                .entrySet()
                .stream()
                .map(entry -> {
                    String text = entry.getKey();
                    String type = entry.getValue().getKey();
                    int count = entry.getValue().getValue();
                    int score = calculateMatchScore(text, query, type);
                    return new SuggestionItem(text, type, score, count);
                })
                .sorted()
                .limit(8)
                .collect(java.util.stream.Collectors.toList());

        // Build UI items
        autocompleteMenu.getItems().clear();

        if (suggestions.isEmpty()) {
            autocompleteMenu.hide();
            return;
        }

        suggestions.forEach(suggestion -> {
            CustomMenuItem item = createSuggestionMenuItem(suggestion, query);
            autocompleteMenu.getItems().add(item);
        });

        // Show the menu
        if (!autocompleteMenu.isShowing() && jobSearchField.isFocused()) {
            autocompleteMenu.show(jobSearchField, javafx.geometry.Side.BOTTOM, 0, 2);
        }
    }

    /**
     * Calculates match score for intelligent ranking
     */
    private int calculateMatchScore(String text, String query, String type) {
        String queryLower = query.toLowerCase();
        String textLower = text.toLowerCase();
        int baseScore = 0;

        if (textLower.startsWith(queryLower)) {
            baseScore = 100; // Exact prefix match (highest priority)
        } else if (textLower.indexOf(queryLower) == 0) {
            baseScore = 80; // Starts with query
        } else {
            int index = textLower.indexOf(queryLower);
            if (index > 0) {
                baseScore = 60 - (index / 10); // Match in middle, penalize for later position
            } else {
                baseScore = 0;
            }
        }

        // Type bonus - titles get preference
        if (type.equals("title")) {
            baseScore += 30;
        } else if (type.equals("company")) {
            baseScore += 20;
        } else if (type.equals("user")) {
            baseScore += 15;
        }

        return baseScore;
    }

    /**
     * Creates a stylized MenuItem for the suggestion dropdown
     */
    private CustomMenuItem createSuggestionMenuItem(SuggestionItem suggestion, String query) {
        HBox content = new HBox(10);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setPadding(new javafx.geometry.Insets(8, 12, 8, 12));

        // Icon based on type
        Label icon = new Label();
        switch (suggestion.type) {
            case "title":
                icon.setText("💼");
                break;
            case "company":
                icon.setText("🏢");
                break;
            case "user":
                icon.setText("👤");
                break;
        }
        icon.setStyle("-fx-font-size: 14px;");

        // Main text with highlight
        VBox textBox = new VBox(2);
        Label mainText = new Label(suggestion.text);
        mainText.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #1f2937;");
        mainText.setMaxWidth(300);
        mainText.setWrapText(true);

        Label subText = new Label(suggestion.type.substring(0, 1).toUpperCase() + suggestion.type.substring(1)
                + " · " + suggestion.jobCount + " job" + (suggestion.jobCount > 1 ? "s" : ""));
        subText.setStyle("-fx-font-size: 11px; -fx-text-fill: #9ca3af;");

        textBox.getChildren().addAll(mainText, subText);

        // Count badge
        Label badge = new Label(String.valueOf(suggestion.jobCount));
        badge.setStyle("-fx-background-color: #ede9fe; -fx-text-fill: #6c0df2; -fx-font-weight: 700; "
                + "-fx-padding: 4 8 4 8; -fx-border-radius: 12; -fx-font-size: 11px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        content.getChildren().addAll(icon, textBox, spacer, badge);

        CustomMenuItem item = new CustomMenuItem(content);
        item.setHideOnClick(true);

        // Handle selection
        item.setOnAction(event -> {
            jobSearchField.setText(suggestion.text);
            jobSearchField.positionCaret(suggestion.text.length());
            autocompleteMenu.hide();
            applyFilters();
        });

        // Hover effect
        content.setOnMouseEntered(event -> {
            content.setStyle("-fx-background-color: #f3e8ff; -fx-border-radius: 6;");
        });

        content.setOnMouseExited(event -> {
            content.setStyle("-fx-background-color: transparent;");
        });

        return item;
    }

    private void setupSalarySlider() {
        jobSalarySlider.setMin(0);
        jobSalarySlider.setMax(10000); // default; refreshSalarySliderMax() will update it
        jobSalarySlider.setValue(0);
        jobSalarySlider.setBlockIncrement(100);
        jobSalarySlider.setMajorTickUnit(1000);

        updateSalaryLabel((int) jobSalarySlider.getValue());

        jobSalarySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateSalaryLabel(newVal.intValue());
            applyFilters();
        });
        // Set max based on whatever jobs were already loaded before this call
        refreshSalarySliderMax();
    }

    private void updateSalaryLabel(int value) {
        salaryFilterLabel.setText("Min Salary: " + value + " TND");
    }

    /**
     * Recalculates the slider's max from the jobs already loaded in allJobs (no
     * extra DB call).
     */
    private void refreshSalarySliderMax() {
        if (jobSalarySlider == null)
            return;
        int maxInData = allJobs.stream()
                .mapToInt(j -> extractMaxSalary(j.getSalaryRange()))
                .max()
                .orElse(0);
        // Add 20% headroom so the slider feels roomy; floor at 10000
        int newMax = Math.max(10000, (int) Math.ceil(maxInData * 1.2 / 1000.0) * 1000);
        if (newMax != (int) jobSalarySlider.getMax()) {
            double currentValue = jobSalarySlider.getValue();
            jobSalarySlider.setMax(newMax);
            jobSalarySlider.setMajorTickUnit(Math.max(1000, newMax / 5));
            // keep current value clamped
            jobSalarySlider.setValue(Math.min(currentValue, newMax));
        }
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

            // Load jobs with user names, emails, and accepted freelancer info
            String query = "SELECT j.*, u.first_name, u.last_name, u.email, " +
                    "(SELECT ja.user_id FROM job_applications ja WHERE ja.job_id = j.id AND ja.status = 'ACCEPTED' LIMIT 1) as accepted_freelancer_id "
                    +
                    "FROM jobs j LEFT JOIN users u ON j.user_id = u.id ORDER BY j.posted_date DESC, j.id DESC";
            try (Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(query)) {

                while (rs.next()) {
                    JobModel job = mapResultSetToJob(rs);
                    allJobs.add(job);
                    filteredJobs.add(job);
                }
            } catch (SQLException e) {
                // If user join fails, try loading jobs without user names
                System.out.println("User join failed, loading jobs without user names: " + e.getMessage());
                loadJobsWithoutUserNames(conn);
            }
        } catch (SQLException e) {
            System.err.println("Error loading jobs: " + e.getMessage());
            loadSampleJobs();
        }
        // Update the filter slider ceiling to reflect the highest salary now in allJobs
        refreshSalarySliderMax();
    }

    private void loadJobsWithoutUserNames(Connection conn) throws SQLException {
        String query = "SELECT * FROM jobs ORDER BY posted_date DESC, id DESC";
        try (Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                String[] requirements = rs.getString("requirements") != null
                        ? rs.getString("requirements").split(",\\s*")
                        : new String[0];

                JobModel job = new JobModel(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("company"),
                        rs.getString("location"),
                        rs.getString("description"),
                        rs.getString("category"),
                        rs.getString("salary_range"),
                        rs.getString("job_type"),
                        rs.getTimestamp("posted_date").toLocalDateTime(),
                        requirements,
                        rs.getInt("user_id"),
                        "Unknown User",
                        "",
                        rs.getInt("progress"),
                        rs.getString("status"),
                        0);
                allJobs.add(job);
                filteredJobs.add(job);
            }
        }
    }

    private JobModel mapResultSetToJob(ResultSet rs) throws SQLException {
        String[] requirements = rs.getString("requirements") != null ? rs.getString("requirements").split(",\\s*")
                : new String[0];

        // Get user name and email from joined users table
        String firstName = rs.getString("first_name");
        String lastName = rs.getString("last_name");
        String userName = "Unknown User";
        if (firstName != null && lastName != null) {
            userName = firstName + " " + lastName;
        }
        String userEmail = rs.getString("email");
        if (userEmail == null) {
            userEmail = "";
        }

        int progress = rs.getInt("progress");
        String status = rs.getString("status");

        return new JobModel(
                rs.getInt("id"),
                rs.getString("title"),
                rs.getString("company"),
                rs.getString("location"),
                rs.getString("description"),
                rs.getString("category"),
                rs.getString("salary_range"),
                rs.getString("job_type"),
                rs.getTimestamp("posted_date").toLocalDateTime(),
                requirements,
                rs.getInt("user_id"),
                userName,
                userEmail,
                progress,
                status,
                rs.getInt("accepted_freelancer_id"));
    }

    private void loadSampleJobs() {
        allJobs.add(new JobModel(1, "Senior Java Developer", "Tech Innovators Inc", "Tunis",
                "We are looking for an experienced Java developer to join our team...",
                "Software Development", "2000 - 3500 TND", "Full-time", LocalDateTime.now().minusDays(2),
                new String[] { "Java 17+", "Spring Boot", "MySQL", "REST APIs" }, 1, "Ahmed Ben Ali",
                "ahmed.benali@techmail.com"));

        allJobs.add(new JobModel(2, "UI/UX Designer", "Creative Studio", "Remote",
                "Join our design team to create stunning user experiences...",
                "Design", "1500 - 2500 TND", "Full-time", LocalDateTime.now().minusDays(5),
                new String[] { "Figma", "Adobe XD", "Prototyping" }, 2, "Fatima Karray", "fatima.karray@design.com"));

        allJobs.add(new JobModel(3, "Frontend Developer React", "Digital Solutions", "Sfax",
                "Looking for a React expert to build modern web applications...",
                "Software Development", "1800 - 3000 TND", "Full-time", LocalDateTime.now().minusDays(1),
                new String[] { "React", "JavaScript", "CSS", "REST APIs" }, 1, "Ahmed Ben Ali",
                "ahmed.benali@techmail.com"));

        allJobs.add(new JobModel(4, "Marketing Manager", "Brand Leaders", "Tunis",
                "Lead our marketing team and develop strategy for brand growth...",
                "Marketing", "2200 - 3500 TND", "Full-time", LocalDateTime.now().minusDays(3),
                new String[] { "Digital Marketing", "Analytics", "Team Leadership" }, 3, "Salem Mezzi",
                "salem.mezzi@brandmail.com"));

        allJobs.add(new JobModel(5, "Data Analyst", "Analytics Pro", "Remote",
                "Analyze data and provide insights for business decisions...",
                "Finance", "1600 - 2800 TND", "Full-time", LocalDateTime.now().minusDays(4),
                new String[] { "Python", "SQL", "Tableau", "Excel" }, 2, "Fatima Karray", "fatima.karray@design.com"));

        allJobs.add(new JobModel(6, "Android Developer", "Mobile Magic", "Tunis",
                "Develop Android applications for innovative mobile solutions...",
                "Software Development", "1900 - 3200 TND", "Full-time", LocalDateTime.now().minusDays(6),
                new String[] { "Android", "Kotlin", "Java", "Firebase" }, 4, "Marouane Saidane",
                "marouane.saidane@mobile.com"));

        allJobs.add(new JobModel(7, "Graphic Designer", "Design Studio Pro", "Sousse",
                "Create stunning visual designs for web and print media...",
                "Design", "1400 - 2400 TND", "Part-time", LocalDateTime.now().minusDays(7),
                new String[] { "Adobe Creative Suite", "UI Design", "Branding" }, 3, "Salem Mezzi",
                "salem.mezzi@brandmail.com"));

        allJobs.add(new JobModel(8, "Sales Executive", "Sales Force", "Tunis",
                "Drive sales growth and manage client relationships...",
                "Sales", "1500 - 2800 TND", "Full-time", LocalDateTime.now().minusDays(8),
                new String[] { "Client Relations", "CRM", "Sales Strategy" }, 5, "Noureddine Bouabdallah",
                "noureddine.bouabdallah@sales.com"));

        allJobs.add(new JobModel(9, "DevOps Engineer", "Cloud Systems", "Remote",
                "Manage infrastructure and CI/CD pipelines for cloud solutions...",
                "Software Development", "2500 - 4000 TND", "Full-time", LocalDateTime.now().minusDays(2),
                new String[] { "Docker", "Kubernetes", "AWS", "Linux" }, 1, "Ahmed Ben Ali",
                "ahmed.benali@techmail.com"));

        allJobs.add(new JobModel(10, "HR Manager", "People First", "Tunis",
                "Manage recruitment, training, and employee relations...",
                "HR", "2000 - 3200 TND", "Full-time", LocalDateTime.now().minusDays(5),
                new String[] { "Recruitment", "Team Building", "HR Systems" }, 6, "Layla Mansour",
                "layla.mansour@hr.com"));

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
                    (job.getUserName() != null && job.getUserName().toLowerCase().contains(searchQuery));

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

        VBox titleBox = new VBox(4);
        Label titleLabel = new Label(job.getTitle());
        titleLabel.setStyle("-fx-font-size: 20; -fx-font-weight: 800; -fx-text-fill: #141118;");

        HBox companyStatusBox = new HBox(8);
        companyStatusBox.setAlignment(Pos.CENTER_LEFT);
        Label companyLabel = new Label(job.getCompany());
        companyLabel.setStyle("-fx-font-size: 15; -fx-font-weight: 600; -fx-text-fill: #6c0df2;");

        Label statusBadge = new Label(job.getStatus());
        String statusColors = switch (job.getStatus().toUpperCase()) {
            case "COMPLETED" -> "-fx-background-color: #dcfce7; -fx-text-fill: #166534;";
            case "IN_PROGRESS" -> "-fx-background-color: #fef9c3; -fx-text-fill: #854d0e;";
            case "CLOSED" -> "-fx-background-color: #fee2e2; -fx-text-fill: #991b1b;";
            default -> "-fx-background-color: #f3f4f6; -fx-text-fill: #374151;";
        };
        statusBadge.setStyle(statusColors
                + " -fx-font-size: 10; -fx-font-weight: 700; -fx-padding: 2 8; -fx-background-radius: 10;");

        companyStatusBox.getChildren().addAll(companyLabel, statusBadge);
        titleBox.getChildren().addAll(titleLabel, companyStatusBox);

        HBox.setHgrow(titleBox, Priority.ALWAYS);
        header.getChildren().addAll(logo, titleBox);
        card.getChildren().add(header);

        // Progress Section
        VBox progBox = new VBox(5);
        progBox.setPadding(new Insets(0, 5, 10, 5));
        HBox progHeader = new HBox();
        Label progLabel = new Label("Project Advancement");
        progLabel.setStyle("-fx-font-size: 12; -fx-font-weight: 600; -fx-text-fill: #4b5563;");
        Region progSpacer = new Region();
        HBox.setHgrow(progSpacer, Priority.ALWAYS);
        Label progVal = new Label(job.getProgress() + "%");
        progVal.setStyle("-fx-font-size: 12; -fx-font-weight: 800; -fx-text-fill: #6c0df2;");
        progHeader.getChildren().addAll(progLabel, progSpacer, progVal);

        ProgressBar pb = new ProgressBar(job.getProgress() / 100.0);
        pb.setMaxWidth(Double.MAX_VALUE);
        pb.setPrefHeight(8);
        pb.setStyle(
                "-fx-accent: #6c0df2; -fx-control-inner-background: #f3f4f6; -fx-background-radius: 10; -fx-padding: 0;");

        progBox.getChildren().addAll(progHeader, pb);

        // Visibility restriction: Only Owner, Admin, or the Assigned Freelancer can see
        // progress
        boolean canSeeProgress = isAdmin() ||
                (App.currentUser != null && (App.currentUser.getId() == job.getUserId()
                        || App.currentUser.getId() == job.getAcceptedFreelancerId()));

        if (canSeeProgress) {
            card.getChildren().add(progBox);
        }

        // Meta
        HBox meta = new HBox(12);
        meta.setAlignment(Pos.CENTER_LEFT);
        meta.getStyleClass().add("job-meta-container");

        // User info (Posted By) - at the TOP position
        VBox userInfo = new VBox(2);
        userInfo.setStyle("-fx-padding: 6 8 6 8; -fx-background-color: #f9f5ff; -fx-border-radius: 6;");

        Label userName = new Label("👤 " + (job.getUserName() != null ? job.getUserName() : "Unknown User"));
        userName.getStyleClass().add("job-user-name");
        userName.setStyle("-fx-text-fill: #6c0df2; -fx-font-size: 12; -fx-font-weight: 600;");

        Label userEmail = new Label("📧 "
                + (job.getUserEmail() != null && !job.getUserEmail().isEmpty() ? job.getUserEmail() : "No email"));
        userEmail.getStyleClass().add("job-user-email");
        userEmail.setStyle("-fx-text-fill: #666666; -fx-font-size: 11;");

        userInfo.getChildren().addAll(userName, userEmail);

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

        meta.getChildren().addAll(userInfo, type, loc, salary, category);
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

        // Message button to contact job owner
        Button messageBtn = new Button("💬 Message");
        messageBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: 600; " +
                "-fx-padding: 8 16 8 16; -fx-border-radius: 8; -fx-cursor: hand;");
        messageBtn.setOnAction(e -> handleMessageJobOwner(job));

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);

        // t affichi edit/delete if user is the owner or admin
        if (App.currentUser != null && (App.currentUser.getId() == job.getUserId() || isAdmin())) {
            footer.getChildren().addAll(posted, footerSpacer, edit, delete, apply);
        } else if (App.currentUser != null && App.currentUser.getId() == job.getAcceptedFreelancerId()) {
            // Freelancer update button
            Button updateProgBtn = new Button("⚡ Update Progress");
            updateProgBtn.setStyle("-fx-background-color: #8b5cf6; -fx-text-fill: white; -fx-font-weight: 700; " +
                    "-fx-padding: 8 16; -fx-background-radius: 8; -fx-cursor: hand;");
            updateProgBtn.setOnAction(e -> showUpdateProgressDialog(job));
            footer.getChildren().addAll(posted, footerSpacer, save, updateProgBtn);
        } else {
            // Non-owner: show Save, Message and Apply buttons
            footer.getChildren().addAll(posted, footerSpacer, save, messageBtn, apply);
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

    // ==================== partie mtaa create job ====================
    @FXML
    private void showAddJobDialog() {

        if (App.currentUser == null) {
            showAlert("Authentication Required", "Please log in to add jobs.");
            return;
        }

        // creation de dialog
        Dialog<JobModel> dialog = new Dialog<>();
        dialog.initOwner(App.getPrimaryStage());
        dialog.setTitle("Add New Job");
        dialog.setHeaderText("Create a new job posting");

        DialogPane dialogPane = dialog.getDialogPane();

        // design
        dialogPane.getStylesheets().add(
                getClass().getResource("/com/khademni/dialogs.css").toExternalForm());
        dialogPane.getStyleClass().add("job-dialog");

        // Boutons
        ButtonType postButtonType = new ButtonType("Post", ButtonBar.ButtonData.OK_DONE);
        dialogPane.getButtonTypes().addAll(postButtonType, ButtonType.CANCEL);

        // Contenu (Form Helper)
        VBox formContent = createJobForm(null, false);
        formContent.getStyleClass().add("job-form");

        ScrollPane scrollPane = new ScrollPane(formContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(600);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: white;");

        dialogPane.setContent(scrollPane);

        // tvalidation 9bal m tsaker
        javafx.scene.Node okButton = dialogPane.lookupButton(postButtonType);
        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            try {
                JobModel job = extractJobFormData(formContent, null, false);

                if (!isJobValid(job)) {
                    event.consume();
                    showAlert(
                            "Validation Error",
                            """
                                    Please check the following:
                                    • Title / Company / Location: minimum 3 characters
                                    • Description: minimum 10 characters
                                    • Salary: required
                                    """,
                            dialog.getDialogPane().getScene().getWindow());
                }
            } catch (Exception ex) {
                // prevent crash inside validation
                ex.printStackTrace();
                event.consume();
                showAlert("Error", "Unexpected error in form: " + ex.getMessage());
            }
        });

        dialog.setResultConverter(button -> {
            if (button == postButtonType) {
                return extractJobFormData(formContent, null, false);
            }
            return null;
        });

        Optional<JobModel> result = dialog.showAndWait();
        try {
            if (result.isPresent()) {
                insertJobToDB(result.get());
            }
        } catch (Throwable ex) {
            // catch any unexpected runtime exception from insert logic
            System.err.println("CRITICAL ERROR in showAddJobDialog: " + ex.getMessage());
            ex.printStackTrace();
            showAlert("Error", "An unexpected error occurred while posting: " + ex.getMessage());
        }
    }

    private boolean isJobValid(JobModel job) {
        return job != null
                && job.getTitle() != null && job.getTitle().trim().length() >= 3
                && job.getCompany() != null && job.getCompany().trim().length() >= 3
                && job.getLocation() != null && job.getLocation().trim().length() >= 3
                && job.getDescription() != null && job.getDescription().trim().length() >= 10
                && job.getSalaryRange() != null && !job.getSalaryRange().trim().isEmpty();
    }

    private void insertJobToDB(JobModel job) {
        try {
            Connection conn = MyDataBase.getConnection();
            if (conn == null) {
                showAlert("Database Error", "Unable to connect to database");
                return;
            }

            String query = "INSERT INTO jobs (title, company, location, description, category, salary_range, job_type, posted_date, requirements, user_id, progress, status) "
                    +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
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
            stmt.setInt(11, job.getProgress());
            stmt.setString(12, job.getStatus());

            stmt.executeUpdate();
            stmt.close();
            conn.close();

            showAlert("Success", "Job created successfully!");
            // clear any active filters/search so user sees the new posting
            if (jobSearchField != null)
                jobSearchField.clear();
            if (jobCategoryFilter != null)
                jobCategoryFilter.setValue("All Categories");
            if (jobLocationFilter != null)
                jobLocationFilter.setValue("All Locations");
            if (jobSalarySlider != null)
                jobSalarySlider.setValue(0);
            filteredJobs.clear();
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
        dialog.initOwner(App.getPrimaryStage());
        dialog.setTitle("Edit Job");
        dialog.setHeaderText("Edit job posting: " + job.getTitle());

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStylesheets().add(
                getClass().getResource("/com/khademni/dialogs.css").toExternalForm());
        dialogPane.getStyleClass().add("job-dialog");
        // Boutons
        ButtonType editButtonType = new ButtonType("Edit", ButtonBar.ButtonData.OK_DONE);
        dialogPane.getButtonTypes().addAll(editButtonType, ButtonType.CANCEL);
        dialogPane.setStyle("-fx-font-size: 12;");

        VBox formContent = createJobForm(job, true);

        ScrollPane scrollPane = new ScrollPane(formContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(600);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: white;");

        dialogPane.setContent(scrollPane);

        javafx.scene.Node okButton = dialog.getDialogPane().lookupButton(editButtonType);
        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, evt -> {
            JobModel candidate = extractJobFormData(formContent, job, true);
            if (candidate.getTitle() == null || candidate.getTitle().trim().length() < 3
                    || candidate.getCompany() == null || candidate.getCompany().trim().length() < 3
                    || candidate.getLocation() == null || candidate.getLocation().trim().length() < 3
                    || candidate.getDescription() == null || candidate.getDescription().trim().length() < 10
                    || candidate.getSalaryRange() == null || candidate.getSalaryRange().trim().isEmpty()) {
                evt.consume();
                showAlert("Validation Error",
                        "Please provide valid Title, Company, Location (min 3 chars), Description (min 10 chars), and Salary (integer).",
                        dialog.getDialogPane().getScene().getWindow());
            }
        });

        dialog.setResultConverter(buttonType -> {
            if (buttonType == editButtonType) {
                return extractJobFormData(formContent, job, true);
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

            String query = "UPDATE jobs SET title=?, company=?, location=?, description=?, category=?, salary_range=?, job_type=?, requirements=?, progress=?, status=? WHERE id=?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, job.getTitle());
                stmt.setString(2, job.getCompany());
                stmt.setString(3, job.getLocation());
                stmt.setString(4, job.getDescription());
                stmt.setString(5, job.getCategory());
                stmt.setString(6, job.getSalaryRange());
                stmt.setString(7, job.getJobType());
                stmt.setString(8, String.join(", ", job.getRequirements()));
                stmt.setInt(9, job.getProgress());
                stmt.setString(10, job.getStatus());
                stmt.setInt(11, job.getId());

                stmt.executeUpdate();
            }
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
        confirmAlert.initOwner(App.getPrimaryStage());
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
    private VBox createJobForm(JobModel editingJob, boolean isEdit) {
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
        categoryCombo.setMaxWidth(Double.MAX_VALUE);
        categoryCombo
                .setStyle("-fx-padding: 8; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #e5e7eb;");

        // Custom category field (visible only when "Other" is selected)
        TextField customCategoryField = createFormField("Enter custom category", "");
        customCategoryField.setVisible(false);
        customCategoryField.setManaged(false);

        // Determine initial value
        java.util.List<String> predefinedCategories = categoryCombo.getItems();
        String existingCategory = editingJob != null ? editingJob.getCategory() : null;
        if (existingCategory != null && !predefinedCategories.contains(existingCategory)) {
            categoryCombo.setValue("Other");
            customCategoryField.setText(existingCategory);
            customCategoryField.setVisible(true);
            customCategoryField.setManaged(true);
        } else {
            categoryCombo.setValue(existingCategory != null ? existingCategory : "Software Development");
        }

        categoryCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean isOther = "Other".equals(newVal);
            customCategoryField.setVisible(isOther);
            customCategoryField.setManaged(isOther);
            if (!isOther)
                customCategoryField.clear();
        });

        VBox categoryBox = new VBox(6, categoryCombo, customCategoryField);
        categoryBox.setMaxWidth(Double.MAX_VALUE);

        // Salary TextField
        String initialSalaryStr = "";
        if (editingJob != null && editingJob.getSalaryRange() != null) {
            initialSalaryStr = editingJob.getSalaryRange().replaceAll("[^0-9]", "");
        }

        TextField salaryField = createFormField("Salary (TND)", initialSalaryStr);
        // Only allow digits
        salaryField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                salaryField.setText(newVal.replaceAll("[^\\d]", ""));
            }
        });

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

        TextArea reqArea = new TextArea(editingJob != null ? String.join("\n", editingJob.getRequirements()) : "");
        reqArea.setStyle("-fx-control-inner-background: #fafafa; -fx-focus-color: #6c0df2;");
        reqArea.setPrefRowCount(4);
        reqArea.setWrapText(true);

        Label descLabel = new Label("Description");
        descLabel.setStyle("-fx-font-weight: 700;");
        Button descAI = createAIButton(descArea, reqArea, "Job Description", titleField, companyField, categoryCombo);
        HBox descHeader = new HBox(10, descLabel, descAI);
        descHeader.setAlignment(Pos.CENTER_LEFT);
        form.getChildren().add(descHeader);
        form.getChildren().add(descArea);

        Label reqLabel = new Label("Requirements (one per line)");
        reqLabel.setStyle("-fx-font-weight: 700;");
        Button reqAI = createAIButton(reqArea, null, "Job Requirements", titleField, companyField, categoryCombo);
        HBox reqHeader = new HBox(10, reqLabel, reqAI);
        reqHeader.setAlignment(Pos.CENTER_LEFT);
        form.getChildren().add(reqHeader);
        form.getChildren().add(reqArea);

        // Progress Slider
        Slider progressSlider = new Slider(0, 100, editingJob != null ? editingJob.getProgress() : 0);
        progressSlider.setShowTickLabels(true);
        progressSlider.setShowTickMarks(true);
        progressSlider.setMajorTickUnit(25);
        progressSlider.setBlockIncrement(5);
        Label progressLabel = new Label("Progress: " + (int) progressSlider.getValue() + "%");
        progressLabel.setStyle("-fx-font-weight: 700;");
        progressSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            progressLabel.setText("Progress: " + newVal.intValue() + "%");
        });
        VBox progressBox = new VBox(5, progressLabel, progressSlider);

        // Status ComboBox
        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("OPEN", "IN_PROGRESS", "COMPLETED", "CLOSED");
        statusCombo.setValue(editingJob != null ? editingJob.getStatus() : "OPEN");
        statusCombo.setMaxWidth(Double.MAX_VALUE);
        statusCombo
                .setStyle("-fx-padding: 8; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #e5e7eb;");

        // Store references for extraction
        form.setUserData(new Object[] { titleField, companyField, locationField, categoryCombo, salaryField, typeCombo,
                descArea, reqArea, progressSlider, statusCombo, customCategoryField });

        form.getChildren().addAll(
                createFormSection("Title", titleField),
                createFormSection("Company", companyField),
                createFormSection("Location", locationBox),
                createFormSection("Category", categoryBox),
                createFormSection("Salary (TND)", salaryField),
                createFormSection("Job Type", typeCombo));

        if (isEdit) {
            form.getChildren().addAll(progressBox, createFormSection("Status", statusCombo));
        }

        return form;
    }

    private void showMapPickerDialog(TextField locationField) {
        Dialog<String> dialog = new Dialog<>();

        // Fix for Z-order: discovery of the active modal/window owner
        if (locationField.getScene() != null && locationField.getScene().getWindow() != null) {
            dialog.initOwner(locationField.getScene().getWindow());
        } else {
            dialog.initOwner(App.getPrimaryStage());
        }
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

    private JobModel extractJobFormData(VBox form, JobModel original, boolean isEdit) {
        Object[] fields = (Object[]) form.getUserData();
        if (fields == null || fields.length < 11) {
            // Fallback for safety, though standard flow goes through here
            return null;
        }

        TextField titleField = (TextField) fields[0];
        TextField companyField = (TextField) fields[1];
        TextField locationField = (TextField) fields[2];
        ComboBox<String> categoryCombo = (ComboBox<String>) fields[3];
        TextField salaryField = (TextField) fields[4];
        ComboBox<String> typeCombo = (ComboBox<String>) fields[5];
        TextArea descArea = (TextArea) fields[6];
        TextArea reqArea = (TextArea) fields[7];
        Slider progressSlider = (Slider) fields[8];
        ComboBox<String> statusCombo = (ComboBox<String>) fields[9];
        TextField customCategoryField = (TextField) fields[10];

        // Resolve category: use custom text when "Other" is selected
        String categoryValue = "Other".equals(categoryCombo.getValue())
                ? (customCategoryField.getText().trim().isEmpty() ? "Other" : customCategoryField.getText().trim())
                : categoryCombo.getValue();

        String[] requirements = reqArea.getText().split("\n");

        String salaryValue = salaryField.getText().trim().isEmpty() ? "0" : salaryField.getText().trim();

        int progressValue = isEdit ? (int) progressSlider.getValue() : 0;
        String statusValue = isEdit ? statusCombo.getValue() : "OPEN";

        String userName = "Unknown User";
        String userEmail = "";
        if (App.currentUser != null) {
            userName = App.currentUser.getFirstName() + " " + App.currentUser.getLastName();
            userEmail = App.currentUser.getEmail();
        }

        JobModel job = new JobModel(
                original != null ? original.getId() : 0,
                titleField.getText(),
                companyField.getText(),
                locationField.getText(),
                descArea.getText(),
                categoryValue,
                salaryValue,
                typeCombo.getValue(),
                original != null ? original.getPostedDate() : LocalDateTime.now(),
                requirements,
                App.currentUser != null ? App.currentUser.getId() : 1,
                userName,
                userEmail,
                progressValue,
                statusValue,
                original != null ? original.getAcceptedFreelancerId() : 0);

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

        // insert into saved_jobs table, ignore duplicates
        try (Connection conn = MyDataBase.getConnection()) {
            String sql = "INSERT IGNORE INTO saved_jobs (user_id, job_id) VALUES (?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, App.currentUser.getId());
                ps.setInt(2, job.getId());
                ps.executeUpdate();
            }
            showAlert("Job Saved", job.getTitle() + " has been added to your saved jobs");
        } catch (Throwable e) {
            System.err.println("ERROR in handleSaveJob: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Unable to save job: " + e.getMessage());
        }
    }

    private void showAlert(String title, String msg) {
        showAlert(title, msg, App.getPrimaryStage());
    }

    private void showAlert(String title, String msg, javafx.stage.Window owner) {
        try {
            Alert a = new Alert(Alert.AlertType.INFORMATION);
            if (owner != null) {
                a.initOwner(owner);
            } else if (App.getPrimaryStage() != null) {
                a.initOwner(App.getPrimaryStage());
            }
            a.setTitle(title);
            a.setHeaderText(null);
            a.setContentText(msg);
            a.showAndWait();
        } catch (Exception e) {
            // fallback: print to console if alert cannot be shown
            System.err.println("showAlert failed: " + e.getMessage());
        }
    }

    // simple window to display saved jobs list
    private void showSavedJobsWindow() {
        if (App.currentUser == null) {
            showAlert("Authentication Required", "Please log in to view saved jobs");
            return;
        }
        List<JobModel> saved = loadSavedJobsForUser(App.currentUser.getId());
        VBox container = new VBox(16);
        container.setStyle("-fx-padding: 24 0 0 0; -fx-max-width: 900; -fx-pref-width: 900;");
        populateContainerWithAnimation(container, saved);
        if (saved.isEmpty()) {
            Label none = new Label("You haven't saved any jobs yet.");
            none.setStyle("-fx-font-size:14px; -fx-text-fill:#666;");
            container.getChildren().add(none);
        }
        ScrollPane scroll = new ScrollPane(container);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.setStyle("-fx-background-color: #f7f5f8; -fx-background: #f7f5f8;");

        Stage stage = new Stage();
        stage.initOwner(App.getPrimaryStage());
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Saved Jobs");
        stage.setScene(new Scene(scroll, 920, 600));
        stage.show();
    }

    /**
     * Creates the saved_jobs table if for some reason it wasn't created by the
     * startup schema check. (The table is also created inside
     * MyDataBase.checkAndFixSchema.)
     */
    private void ensureSavedJobsTableExists() {
        try (Connection conn = MyDataBase.getConnection();
                Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS saved_jobs ("
                    + "user_id INT NOT NULL, job_id INT NOT NULL, "
                    + "saved_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + "PRIMARY KEY(user_id, job_id))");
        } catch (SQLException e) {
            System.err.println("Unable to ensure saved_jobs table: " + e.getMessage());
        }
    }

    /**
     * Load all jobs a user has saved. We join with the jobs table so we can
     * re‑use the same JobModel mapping logic.
     */
    private List<JobModel> loadSavedJobsForUser(int userId) {
        List<JobModel> list = new ArrayList<>();
        try (Connection conn = MyDataBase.getConnection()) {
            String sql = "SELECT j.*, u.first_name, u.last_name, u.email, " +
                    "(SELECT ja.user_id FROM job_applications ja WHERE ja.job_id = j.id AND ja.status = 'ACCEPTED' LIMIT 1) as accepted_freelancer_id "
                    +
                    "FROM jobs j LEFT JOIN users u ON j.user_id = u.id " +
                    "JOIN saved_jobs s ON s.job_id = j.id " +
                    "WHERE s.user_id = ? ORDER BY s.saved_at DESC";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapResultSetToJob(rs));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Refresh contents of the saved jobs tab (if it exists).
     */
    // kept for potential reuse but not used directly in single-page UI
    private void refreshSavedJobs() {
        // not needed when using separate window, left for compatibility
    }

    /**
     * Utility used both for main list and saved list so we avoid duplicating
     * animation/display logic.
     */
    private void populateContainerWithAnimation(VBox container, List<JobModel> jobs) {
        container.getChildren().clear();
        int idx = 0;
        for (JobModel job : jobs) {
            VBox card = createJobCard(job);
            card.setOpacity(0);
            card.setTranslateY(12);
            container.getChildren().add(card);
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

    /**
     * Handler attached to the "Saved Jobs" button; simply selects the tab and
     * triggers a refresh.
     */
    @FXML
    private void openSavedJobsTab(ActionEvent event) {
        showSavedJobsWindow();
    }

    private void showUpdateProgressDialog(JobModel job) {
        Dialog<AbstractMap.SimpleEntry<Integer, String>> dialog = new Dialog<>();
        dialog.initOwner(App.getPrimaryStage());
        dialog.setTitle("Update Work Progress");
        dialog.setHeaderText("Report your progress for: " + job.getTitle());

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/com/khademni/dialogs.css").toExternalForm());
        dialogPane.getStyleClass().add("job-dialog");

        ButtonType saveButtonType = new ButtonType("Submit Report", ButtonBar.ButtonData.OK_DONE);
        dialogPane.getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setPrefWidth(400);

        Label progLabel = new Label("Current Progress: " + job.getProgress() + "%");
        progLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");

        Slider slider = new Slider(0, 100, job.getProgress());
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setMajorTickUnit(25);
        slider.setBlockIncrement(5);

        Label newValLabel = new Label("New Progress: " + (int) slider.getValue() + "%");
        newValLabel.setStyle("-fx-text-fill: #6c0df2; -fx-font-weight: bold;");
        slider.valueProperty()
                .addListener((obs, old, val) -> newValLabel.setText("New Progress: " + val.intValue() + "%"));

        Label descLabel = new Label("What did you accomplish?");
        descLabel.setStyle("-fx-font-weight: bold;");
        TextArea descArea = new TextArea();
        descArea.setPromptText("Describe the work you've completed...");
        descArea.setPrefRowCount(4);
        descArea.setWrapText(true);

        content.getChildren().addAll(progLabel, slider, newValLabel, descLabel, descArea);
        dialogPane.setContent(content);

        dialog.setResultConverter(btn -> {
            if (btn == saveButtonType) {
                return new AbstractMap.SimpleEntry<>((int) slider.getValue(), descArea.getText());
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            saveProgressUpdate(job, result.getKey(), result.getValue());
        });
    }

    private void saveProgressUpdate(JobModel job, int newProgress, String description) {
        if (description == null || description.trim().isEmpty()) {
            showAlert("Input Required", "Please describe what you did.");
            return;
        }

        try (Connection conn = MyDataBase.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Insert work log
                String logSql = "INSERT INTO work_logs (job_id, freelancer_id, progress_change, description) VALUES (?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(logSql)) {
                    ps.setInt(1, job.getId());
                    ps.setInt(2, App.currentUser.getId());
                    ps.setInt(3, newProgress);
                    ps.setString(4, description);
                    ps.executeUpdate();
                }

                // 2. Update job progress
                // If progress is 100, set status to COMPLETED
                String status = newProgress >= 100 ? "COMPLETED" : "IN_PROGRESS";
                String jobSql = "UPDATE jobs SET progress = ?, status = ? WHERE id = ?";
                try (PreparedStatement ps = conn.prepareStatement(jobSql)) {
                    ps.setInt(1, newProgress);
                    ps.setString(2, status);
                    ps.setInt(3, job.getId());
                    ps.executeUpdate();
                }

                conn.commit();
                showAlert("Success", "Progress updated successfully!");
                loadJobsFromDB();
                displayJobs(allJobs); // uses allJobs as base for refresh
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to update progress: " + e.getMessage());
        }
    }

    private Button createAIButton(TextArea target, TextArea secondaryTarget, String contextType, TextField titleF,
            TextField companyF, ComboBox<String> categoryC) {
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

            java.util.Map<String, String> context = new java.util.HashMap<>();
            context.put("contextType", contextType);
            context.put("Job Title", titleF.getText());
            context.put("Company", companyF.getText());
            context.put("Category", categoryC.getValue());

            AIService.rewriteProfessionally(original, context).thenAccept(improved -> {
                javafx.application.Platform.runLater(() -> {
                    String cleanImproved = improved.trim();
                    if (cleanImproved.startsWith("```")) {
                        cleanImproved = cleanImproved.replaceAll("(?s)^```(?:json)?\\s*(.*?)\\s*```$", "$1").trim();
                    }

                    if (secondaryTarget != null && cleanImproved.startsWith("{")) {
                        try {
                            org.json.JSONObject obj = new org.json.JSONObject(cleanImproved);
                            target.setText(obj.optString("description", cleanImproved));
                            secondaryTarget.setText(obj.optString("requirements", ""));
                        } catch (Exception ex) {
                            target.setText(cleanImproved);
                        }
                    } else {
                        target.setText(cleanImproved);
                    }
                    aiBtn.setDisable(false);
                    aiBtn.setText("✨ AI Enhance");
                });
            });
        });
        return aiBtn;
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

    private void handleMessageJobOwner(JobModel job) {
        if (App.currentUser == null) {
            showAlert("Authentication Required", "Please log in to message the job owner.");
            return;
        }

        if (job.getUserId() <= 0) {
            showAlert("Error", "This job has no owner assigned.");
            return;
        }

        if (job.getUserId() == App.currentUser.getId()) {
            showAlert("Error", "You cannot message yourself.");
            return;
        }

        try {
            int conversationId = ConversationHelper.createOrGetConversation(
                App.currentUser.getId(),
                job.getUserId(),
                "Job: " + job.getTitle()
            );
            openConversation(conversationId);
        } catch (SQLException e) {
            showAlert("Error", "Error creating conversation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void openConversation(int conversationId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/khademni/Message.fxml"));
            Parent root = loader.load();

            MessageController messageController = loader.getController();
            messageController.setConversationId(conversationId);

            Stage stage = (Stage) jobsContainer.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Conversation");

        } catch (Exception e) {
            showAlert("Error", "Error opening conversation: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
