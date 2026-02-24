package com.khademni.controller;

import com.khademni.model.ReviewModel;
import com.khademni.utils.MyDataBase;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ReviewController {

    @FXML
    private Label statusLabel;

    @FXML
    private TextField txtClientId;

    @FXML
    private TextField txtFreelancerId;

    @FXML
    private TextField txtRating;

    @FXML
    private TextArea txtReviewText;

    @FXML
    private TableView<ReviewModel> tableReview;

    @FXML
    private TableColumn<ReviewModel, Integer> colId;

    @FXML
    private TableColumn<ReviewModel, Integer> colClient;

    @FXML
    private TableColumn<ReviewModel, Integer> colFreelancer;

    @FXML
    private TableColumn<ReviewModel, Integer> colRating;

    @FXML
    private TableColumn<ReviewModel, String> colReviewText;

    private ObservableList<ReviewModel> reviewList = FXCollections.observableArrayList();
    private int idCounter = 1;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colClient.setCellValueFactory(new PropertyValueFactory<>("clientId"));
        colFreelancer.setCellValueFactory(new PropertyValueFactory<>("freelancerId"));
        colRating.setCellValueFactory(new PropertyValueFactory<>("rating"));
        colReviewText.setCellValueFactory(new PropertyValueFactory<>("reviewText"));

        tableReview.setItems(reviewList);
        
        // Load existing reviews from database
        loadReviewsFromDatabase();
    }
    
    private void loadReviewsFromDatabase() {
        try {
            String query = "SELECT * FROM reviews";
            Connection conn = MyDataBase.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                ResultSet rs = stmt.executeQuery();
                
                reviewList.clear();
                while (rs.next()) {
                    ReviewModel review = new ReviewModel(
                        rs.getInt("id"),
                        rs.getInt("client_id"),
                        rs.getInt("freelancer_id"),
                        rs.getInt("rating"),
                        rs.getString("review_text")
                    );
                    reviewList.add(review);
                    
                    // Update idCounter to be higher than max id
                    if (review.getId() >= idCounter) {
                        idCounter = review.getId() + 1;
                    }
                }
            }
        } catch (SQLException e) {
            showError("Error loading reviews: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void ajouterReview() {
        // Clear previous status
        hideStatus();

        try {
            // Validate input
            if (txtClientId.getText().trim().isEmpty() || txtFreelancerId.getText().trim().isEmpty() ||
                txtRating.getText().trim().isEmpty() || txtReviewText.getText().trim().isEmpty()) {
                showError("Please fill in all fields.");
                return;
            }

            int clientId = Integer.parseInt(txtClientId.getText().trim());
            int freelancerId = Integer.parseInt(txtFreelancerId.getText().trim());
            int rating = Integer.parseInt(txtRating.getText().trim());
            String reviewText = txtReviewText.getText().trim();

            // Validate rating range
            if (rating < 1 || rating > 5) {
                showError("Rating must be between 1 and 5.");
                return;
            }

            // Check if client ID exists in users table
            if (!userExists(clientId)) {
                showError("Client ID " + clientId + " does not exist in users table.");
                return;
            }

            // Check if freelancer ID exists in users table
            if (!userExists(freelancerId)) {
                showError("Freelancer ID " + freelancerId + " does not exist in users table.");
                return;
            }

            // Insert into database
            String insertQuery = "INSERT INTO reviews (client_id, freelancer_id, rating, review_text) VALUES (?, ?, ?, ?)";
            Connection conn = MyDataBase.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(insertQuery, PreparedStatement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, clientId);
                stmt.setInt(2, freelancerId);
                stmt.setInt(3, rating);
                stmt.setString(4, reviewText);
                
                int rowsAffected = stmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    // Get the generated ID
                    ResultSet generatedKeys = stmt.getGeneratedKeys();
                    int generatedId = idCounter++;
                    if (generatedKeys.next()) {
                        generatedId = generatedKeys.getInt(1);
                        idCounter = generatedId + 1;
                    }
                    
                    // Add to table view
                    ReviewModel review = new ReviewModel(
                            generatedId,
                            clientId,
                            freelancerId,
                            rating,
                            reviewText
                    );
                    reviewList.add(review);
                    showSuccess("Review added successfully!");
                    clearFields();
                } else {
                    showError("Failed to add review to database.");
                }
            }

        } catch (NumberFormatException e) {
            showError("Please enter valid numbers for IDs and rating.");
        } catch (SQLException e) {
            showError("Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean userExists(int userId) throws SQLException {
        String query = "SELECT COUNT(*) FROM users WHERE id = ?";
        Connection conn = MyDataBase.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    private void showError(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
            statusLabel.setStyle("-fx-text-fill: #dc2626; -fx-background-color: #fef2f2; -fx-border-color: #fecaca; -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 12 20 12 20; -fx-font-size: 14; -fx-font-weight: 600;");
            statusLabel.setVisible(true);
            statusLabel.setManaged(true);
        }
    }

    private void showSuccess(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
            statusLabel.setStyle("-fx-text-fill: #059669; -fx-background-color: #ecfdf5; -fx-border-color: #a7f3d0; -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 12 20 12 20; -fx-font-size: 14; -fx-font-weight: 600;");
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

    @FXML
    private void supprimerReview() {
        ReviewModel selected = tableReview.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                // Delete from database
                String deleteQuery = "DELETE FROM reviews WHERE id = ?";
                Connection conn = MyDataBase.getConnection();
                try (PreparedStatement stmt = conn.prepareStatement(deleteQuery)) {
                    stmt.setInt(1, selected.getId());
                    int rowsAffected = stmt.executeUpdate();
                    
                    if (rowsAffected > 0) {
                        reviewList.remove(selected);
                        showSuccess("Review deleted successfully!");
                    } else {
                        showError("Failed to delete review from database.");
                    }
                }
            } catch (SQLException e) {
                showError("Database error: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            showError("Please select a review to delete.");
        }
    }

    private void clearFields() {
        txtClientId.clear();
        txtFreelancerId.clear();
        txtRating.clear();
        txtReviewText.clear();
    }
}
