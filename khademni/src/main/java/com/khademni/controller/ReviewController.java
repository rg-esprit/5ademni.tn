package com.khademni.controller;

import com.khademni.App;
import com.khademni.model.ReviewModel;
import com.khademni.model.UserModel;
import com.khademni.utils.MyDataBase;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.kordamp.ikonli.javafx.FontIcon;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ReviewController {

    // ---- FXML Fields ----
    @FXML private Label statusLabel;
    @FXML private TextField userSearchField;
    @FXML private VBox searchResultsContainer;
    @FXML private HBox selectedUserBox;
    @FXML private Label selectedUserInitials;
    @FXML private Label selectedUserName;
    @FXML private Label selectedUserEmail;
    @FXML private Button clearSelectionBtn;
    @FXML private HBox starsContainer;
    @FXML private TextArea txtReviewText;
    @FXML private Button submitReviewBtn;
    @FXML private Button cancelEditBtn;
    @FXML private VBox receivedReviewsContainer;
    @FXML private VBox givenReviewsContainer;
    @FXML private Label noReceivedLabel;
    @FXML private Label noGivenLabel;

    // ---- State ----
    private int selectedUserId = -1;
    private int selectedRating = 0;
    private int editingReviewId = -1; // -1 = creating new, otherwise editing
    private final List<Label> starLabels = new ArrayList<>();

    @FXML
    public void initialize() {
        setupStarRating();
        setupSearchListener();
        loadAllReviews();
    }

    // ==================================================
    // Star Rating Setup
    // ==================================================
    private void setupStarRating() {
        starsContainer.getChildren().clear();
        starLabels.clear();
        for (int i = 1; i <= 5; i++) {
            final int star = i;
            FontIcon icon = new FontIcon("fas-star");
            icon.setIconSize(28);
            icon.setIconColor(javafx.scene.paint.Color.web("#e0e0e0"));

            Label starLabel = new Label();
            starLabel.setGraphic(icon);
            starLabel.getStyleClass().add("star-btn");
            starLabel.setOnMouseClicked(e -> selectRating(star));
            starLabel.setOnMouseEntered(e -> hoverRating(star));
            starLabel.setOnMouseExited(e -> hoverRating(0));
            starLabels.add(starLabel);
            starsContainer.getChildren().add(starLabel);
        }
    }

    private void selectRating(int rating) {
        selectedRating = rating;
        updateStarDisplay(rating);
    }

    private void hoverRating(int hoverStar) {
        if (hoverStar > 0) {
            updateStarDisplay(hoverStar);
        } else {
            updateStarDisplay(selectedRating);
        }
    }

    private void updateStarDisplay(int filledCount) {
        for (int i = 0; i < starLabels.size(); i++) {
            FontIcon icon = (FontIcon) starLabels.get(i).getGraphic();
            if (i < filledCount) {
                icon.setIconColor(javafx.scene.paint.Color.web("#f59e0b"));
            } else {
                icon.setIconColor(javafx.scene.paint.Color.web("#e0e0e0"));
            }
        }
    }

    // ==================================================
    // User Search
    // ==================================================
    private void setupSearchListener() {
        userSearchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.trim().length() < 2) {
                searchResultsContainer.setVisible(false);
                searchResultsContainer.setManaged(false);
                return;
            }
            searchUsers(newVal.trim());
        });
    }

    private void searchUsers(String query) {
        UserModel currentUser = App.getCurrentUser();
        if (currentUser == null) return;

        Thread searchThread = new Thread(() -> {
            try {
                String sql = "SELECT id, first_name, last_name, email FROM users " +
                             "WHERE id != ? AND (LOWER(first_name) LIKE ? OR LOWER(last_name) LIKE ? " +
                             "OR LOWER(CONCAT(first_name, ' ', last_name)) LIKE ?) LIMIT 8";
                Connection conn = MyDataBase.getConnection();
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    String pattern = "%" + query.toLowerCase() + "%";
                    stmt.setInt(1, currentUser.getId());
                    stmt.setString(2, pattern);
                    stmt.setString(3, pattern);
                    stmt.setString(4, pattern);
                    ResultSet rs = stmt.executeQuery();

                    List<int[]> ids = new ArrayList<>();
                    List<String[]> data = new ArrayList<>();
                    while (rs.next()) {
                        ids.add(new int[]{rs.getInt("id")});
                        data.add(new String[]{
                            rs.getString("first_name"),
                            rs.getString("last_name"),
                            rs.getString("email")
                        });
                    }

                    Platform.runLater(() -> {
                        searchResultsContainer.getChildren().clear();
                        if (data.isEmpty()) {
                            searchResultsContainer.setVisible(false);
                            searchResultsContainer.setManaged(false);
                            return;
                        }

                        for (int i = 0; i < data.size(); i++) {
                            int userId = ids.get(i)[0];
                            String firstName = data.get(i)[0];
                            String lastName = data.get(i)[1];
                            String email = data.get(i)[2];
                            String fullName = firstName + " " + lastName;

                            HBox item = new HBox(12);
                            item.setAlignment(Pos.CENTER_LEFT);
                            item.getStyleClass().add("search-result-item");

                            // Avatar
                            StackPane avatar = new StackPane();
                            avatar.setPrefSize(36, 36);
                            avatar.setMaxSize(36, 36);
                            avatar.getStyleClass().add("review-avatar-small");
                            String initials = ("" + firstName.charAt(0) + lastName.charAt(0)).toUpperCase();
                            Label avatarLabel = new Label(initials);
                            avatarLabel.getStyleClass().add("review-avatar-initials-small");
                            avatar.getChildren().add(avatarLabel);

                            VBox nameBox = new VBox(2);
                            Label nameLabel = new Label(fullName);
                            nameLabel.getStyleClass().add("search-result-name");
                            Label emailLabel = new Label(email);
                            emailLabel.getStyleClass().add("search-result-email");
                            nameBox.getChildren().addAll(nameLabel, emailLabel);

                            item.getChildren().addAll(avatar, nameBox);
                            item.setOnMouseClicked(e -> selectUser(userId, firstName, lastName, email));

                            searchResultsContainer.getChildren().add(item);
                        }

                        searchResultsContainer.setVisible(true);
                        searchResultsContainer.setManaged(true);
                    });
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, "review-search");
        searchThread.setDaemon(true);
        searchThread.start();
    }

    private void selectUser(int userId, String firstName, String lastName, String email) {
        selectedUserId = userId;
        String fullName = firstName + " " + lastName;
        String initials = ("" + firstName.charAt(0) + lastName.charAt(0)).toUpperCase();

        selectedUserInitials.setText(initials);
        selectedUserName.setText(fullName);
        selectedUserEmail.setText(email);

        selectedUserBox.setVisible(true);
        selectedUserBox.setManaged(true);

        userSearchField.setVisible(false);
        userSearchField.setManaged(false);
        searchResultsContainer.setVisible(false);
        searchResultsContainer.setManaged(false);
    }

    @FXML
    private void clearUserSelection() {
        selectedUserId = -1;
        selectedUserBox.setVisible(false);
        selectedUserBox.setManaged(false);
        userSearchField.setVisible(true);
        userSearchField.setManaged(true);
        userSearchField.clear();
    }

    // ==================================================
    // Submit / Edit / Delete Review
    // ==================================================
    @FXML
    private void submitReview() {
        hideStatus();
        UserModel currentUser = App.getCurrentUser();
        if (currentUser == null) {
            showError("You must be logged in to submit a review.");
            return;
        }

        if (selectedUserId == -1) {
            showError("Please search and select a user to review.");
            return;
        }

        if (selectedRating == 0) {
            showError("Please select a rating (1-5 stars).");
            return;
        }

        String reviewText = txtReviewText.getText() != null ? txtReviewText.getText().trim() : "";
        if (reviewText.isEmpty()) {
            showError("Please write a review.");
            return;
        }

        if (editingReviewId == -1) {
            // Creating new review — check for duplicates
            try {
                if (reviewExists(currentUser.getId(), selectedUserId)) {
                    showError("You have already reviewed this user. You can edit your existing review below.");
                    return;
                }
            } catch (SQLException e) {
                showError("Database error: " + e.getMessage());
                e.printStackTrace();
                return;
            }
            createReview(currentUser.getId(), selectedUserId, selectedRating, reviewText);
        } else {
            // Updating existing review
            updateReview(editingReviewId, selectedRating, reviewText);
        }
    }

    private void createReview(int clientId, int freelancerId, int rating, String reviewText) {
        try {
            String sql = "INSERT INTO reviews (client_id, freelancer_id, rating, review_text) VALUES (?, ?, ?, ?)";
            Connection conn = MyDataBase.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, clientId);
                stmt.setInt(2, freelancerId);
                stmt.setInt(3, rating);
                stmt.setString(4, reviewText);
                stmt.executeUpdate();
            }
            showSuccess("Review submitted successfully!");
            resetForm();
            loadAllReviews();
        } catch (SQLException e) {
            showError("Failed to submit review: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateReview(int reviewId, int rating, String reviewText) {
        try {
            String sql = "UPDATE reviews SET rating = ?, review_text = ? WHERE id = ?";
            Connection conn = MyDataBase.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, rating);
                stmt.setString(2, reviewText);
                stmt.setInt(3, reviewId);
                stmt.executeUpdate();
            }
            showSuccess("Review updated successfully!");
            resetForm();
            loadAllReviews();
        } catch (SQLException e) {
            showError("Failed to update review: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void deleteReview(int reviewId) {
        try {
            String sql = "DELETE FROM reviews WHERE id = ?";
            Connection conn = MyDataBase.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, reviewId);
                stmt.executeUpdate();
            }
            showSuccess("Review deleted successfully!");
            loadAllReviews();
        } catch (SQLException e) {
            showError("Failed to delete review: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean reviewExists(int clientId, int freelancerId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM reviews WHERE client_id = ? AND freelancer_id = ?";
        Connection conn = MyDataBase.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, clientId);
            stmt.setInt(2, freelancerId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    private void startEdit(ReviewModel review) {
        editingReviewId = review.getId();
        selectedRating = review.getRating();
        updateStarDisplay(selectedRating);
        txtReviewText.setText(review.getReviewText());

        // Select the freelancer user
        selectUser(review.getFreelancerId(),
                   review.getFreelancerFirstName(),
                   review.getFreelancerLastName(),
                   review.getFreelancerEmail());

        // Disable user search and show cancel
        clearSelectionBtn.setVisible(false);
        clearSelectionBtn.setManaged(false);
        cancelEditBtn.setVisible(true);
        cancelEditBtn.setManaged(true);
        submitReviewBtn.setText("Update Review");

        // Scroll to top
        txtReviewText.requestFocus();
    }

    @FXML
    private void cancelEdit() {
        resetForm();
    }

    private void resetForm() {
        editingReviewId = -1;
        selectedUserId = -1;
        selectedRating = 0;
        updateStarDisplay(0);
        txtReviewText.clear();

        selectedUserBox.setVisible(false);
        selectedUserBox.setManaged(false);
        userSearchField.setVisible(true);
        userSearchField.setManaged(true);
        userSearchField.clear();

        clearSelectionBtn.setVisible(true);
        clearSelectionBtn.setManaged(true);
        cancelEditBtn.setVisible(false);
        cancelEditBtn.setManaged(false);
        submitReviewBtn.setText("Submit Review");
    }

    // ==================================================
    // Load & Display Reviews
    // ==================================================
    private void loadAllReviews() {
        UserModel currentUser = App.getCurrentUser();
        if (currentUser == null) return;

        int userId = currentUser.getId();

        // Load reviews about you (where you are the freelancer)
        List<ReviewModel> received = loadReviews(
            "SELECT r.*, " +
            "c.first_name AS client_first, c.last_name AS client_last, c.email AS client_email, " +
            "f.first_name AS freelancer_first, f.last_name AS freelancer_last, f.email AS freelancer_email " +
            "FROM reviews r " +
            "JOIN users c ON r.client_id = c.id " +
            "JOIN users f ON r.freelancer_id = f.id " +
            "WHERE r.freelancer_id = ? ORDER BY r.id DESC", userId);

        // Load reviews you gave (where you are the client)
        List<ReviewModel> given = loadReviews(
            "SELECT r.*, " +
            "c.first_name AS client_first, c.last_name AS client_last, c.email AS client_email, " +
            "f.first_name AS freelancer_first, f.last_name AS freelancer_last, f.email AS freelancer_email " +
            "FROM reviews r " +
            "JOIN users c ON r.client_id = c.id " +
            "JOIN users f ON r.freelancer_id = f.id " +
            "WHERE r.client_id = ? ORDER BY r.id DESC", userId);

        displayReceivedReviews(received);
        displayGivenReviews(given, userId);
    }

    private List<ReviewModel> loadReviews(String sql, int userId) {
        List<ReviewModel> reviews = new ArrayList<>();
        try {
            Connection conn = MyDataBase.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    ReviewModel r = new ReviewModel(
                        rs.getInt("id"),
                        rs.getInt("client_id"),
                        rs.getInt("freelancer_id"),
                        rs.getInt("rating"),
                        rs.getString("review_text")
                    );
                    r.setClientFirstName(rs.getString("client_first"));
                    r.setClientLastName(rs.getString("client_last"));
                    r.setClientEmail(rs.getString("client_email"));
                    r.setFreelancerFirstName(rs.getString("freelancer_first"));
                    r.setFreelancerLastName(rs.getString("freelancer_last"));
                    r.setFreelancerEmail(rs.getString("freelancer_email"));
                    reviews.add(r);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return reviews;
    }

    private void displayReceivedReviews(List<ReviewModel> reviews) {
        receivedReviewsContainer.getChildren().clear();
        if (reviews.isEmpty()) {
            noReceivedLabel.setVisible(true);
            noReceivedLabel.setManaged(true);
        } else {
            noReceivedLabel.setVisible(false);
            noReceivedLabel.setManaged(false);
            for (ReviewModel review : reviews) {
                receivedReviewsContainer.getChildren().add(
                    buildReviewCard(review, review.getClientFullName(), review.getClientFirstName(), review.getClientLastName(), false)
                );
            }
        }
    }

    private void displayGivenReviews(List<ReviewModel> reviews, int currentUserId) {
        givenReviewsContainer.getChildren().clear();
        if (reviews.isEmpty()) {
            noGivenLabel.setVisible(true);
            noGivenLabel.setManaged(true);
        } else {
            noGivenLabel.setVisible(false);
            noGivenLabel.setManaged(false);
            for (ReviewModel review : reviews) {
                boolean canModify = (review.getClientId() == currentUserId);
                givenReviewsContainer.getChildren().add(
                    buildReviewCard(review, review.getFreelancerFullName(), review.getFreelancerFirstName(), review.getFreelancerLastName(), canModify)
                );
            }
        }
    }

    private VBox buildReviewCard(ReviewModel review, String displayName, String firstName, String lastName, boolean showActions) {
        VBox card = new VBox(12);
        card.getStyleClass().add("review-card");

        // Top row: avatar + name + stars
        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);

        // Avatar
        StackPane avatar = new StackPane();
        avatar.getStyleClass().add("review-card-avatar");
        String initials = "";
        if (firstName != null && !firstName.isEmpty()) initials += firstName.charAt(0);
        if (lastName != null && !lastName.isEmpty()) initials += lastName.charAt(0);
        Label avatarLabel = new Label(initials.toUpperCase());
        avatarLabel.getStyleClass().add("review-card-avatar-initials");
        avatar.getChildren().add(avatarLabel);

        // Name
        VBox nameBox = new VBox(2);
        Label nameLabel = new Label(displayName.trim());
        nameLabel.getStyleClass().add("review-card-name");
        nameBox.getChildren().add(nameLabel);

        // Stars
        HBox starsBox = new HBox(3);
        starsBox.setAlignment(Pos.CENTER_LEFT);
        for (int i = 1; i <= 5; i++) {
            FontIcon starIcon = new FontIcon("fas-star");
            starIcon.setIconSize(14);
            if (i <= review.getRating()) {
                starIcon.setIconColor(javafx.scene.paint.Color.web("#f59e0b"));
            } else {
                starIcon.setIconColor(javafx.scene.paint.Color.web("#e0e0e0"));
            }
            starsBox.getChildren().add(starIcon);
        }
        nameBox.getChildren().add(starsBox);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        topRow.getChildren().addAll(avatar, nameBox, spacer);

        // Action buttons (only for own reviews)
        if (showActions) {
            HBox actionsBox = new HBox(8);
            actionsBox.setAlignment(Pos.CENTER_RIGHT);

            Button editBtn = new Button("Edit");
            editBtn.getStyleClass().add("review-edit-btn");
            editBtn.setGraphic(createSmallIcon("fas-pen", "#6c0df2", 12));
            editBtn.setOnAction(e -> startEdit(review));

            Button deleteBtn = new Button("Delete");
            deleteBtn.getStyleClass().add("review-delete-btn");
            deleteBtn.setGraphic(createSmallIcon("fas-trash", "#dc2626", 12));
            deleteBtn.setOnAction(e -> {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Are you sure you want to delete this review?",
                    ButtonType.YES, ButtonType.NO);
                confirm.setHeaderText("Delete Review");
                confirm.initOwner(App.getPrimaryStage());
                confirm.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.YES) {
                        deleteReview(review.getId());
                    }
                });
            });

            actionsBox.getChildren().addAll(editBtn, deleteBtn);
            topRow.getChildren().add(actionsBox);
        }

        // Review text
        Label textLabel = new Label(review.getReviewText());
        textLabel.getStyleClass().add("review-card-text");
        textLabel.setWrapText(true);

        card.getChildren().addAll(topRow, textLabel);
        return card;
    }

    private FontIcon createSmallIcon(String literal, String color, int size) {
        FontIcon icon = new FontIcon(literal);
        icon.setIconSize(size);
        icon.setIconColor(javafx.scene.paint.Color.web(color));
        return icon;
    }

    // ==================================================
    // Status Messages
    // ==================================================
    private void showError(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
            statusLabel.setStyle("-fx-text-fill: #dc2626; -fx-background-color: #fef2f2; -fx-border-color: #fecaca; -fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 12 20 12 20; -fx-font-size: 14; -fx-font-weight: 600;");
            statusLabel.setVisible(true);
            statusLabel.setManaged(true);
        }
    }

    private void showSuccess(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
            statusLabel.setStyle("-fx-text-fill: #059669; -fx-background-color: #ecfdf5; -fx-border-color: #a7f3d0; -fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 12 20 12 20; -fx-font-size: 14; -fx-font-weight: 600;");
            statusLabel.setVisible(true);
            statusLabel.setManaged(true);
        }
    }

    private void hideStatus() {
        if (statusLabel != null) {
            statusLabel.setVisible(false);
            statusLabel.setManaged(false);
        }
    }
}
