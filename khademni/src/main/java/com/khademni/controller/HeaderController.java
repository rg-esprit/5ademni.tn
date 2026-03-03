package com.khademni.controller;

import java.io.IOException;
import java.io.InputStream;

import com.khademni.App;
import com.khademni.model.UserModel;
import com.khademni.utils.VercelBlobUploader;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;

public class HeaderController {

    @FXML
    private javafx.scene.control.Hyperlink jobsManagementLink;

    @FXML
    private Label headerAvatarInitials;

    @FXML
    private ImageView headerAvatarImage;

    @FXML
    public void initialize() {
        if (App.getCurrentUser() != null && App.getCurrentUser().isIsAdmin()) {
            jobsManagementLink.setVisible(true);
            jobsManagementLink.setManaged(true);
        } else {
            jobsManagementLink.setVisible(false);
            jobsManagementLink.setManaged(false);
        }

        loadHeaderAvatar();
    }

    private void loadHeaderAvatar() {
        UserModel user = App.getCurrentUser();
        if (user == null) return;

        // Set initials
        if (headerAvatarInitials != null) {
            String initials = "";
            if (user.getFirstName() != null && !user.getFirstName().isEmpty())
                initials += user.getFirstName().charAt(0);
            if (user.getLastName() != null && !user.getLastName().isEmpty())
                initials += user.getLastName().charAt(0);
            headerAvatarInitials.setText(initials.toUpperCase());
        }

        // Load profile image from Vercel Blob
        String imgUrl = user.getProfileImg();
        if (headerAvatarImage != null && imgUrl != null && !imgUrl.isBlank()) {
            Thread loader = new Thread(() -> {
                try {
                    InputStream is = VercelBlobUploader.downloadAsStream(imgUrl);
                    javafx.application.Platform.runLater(() -> {
                        Image img = new Image(is, 40, 40, true, true);
                        headerAvatarImage.setImage(img);
                        Circle clip = new Circle(20, 20, 20);
                        headerAvatarImage.setClip(clip);
                        headerAvatarImage.setVisible(true);
                        headerAvatarImage.setManaged(true);
                        // Hide initials + gradient circle behind image
                        if (headerAvatarInitials != null) {
                            headerAvatarInitials.setVisible(false);
                            headerAvatarInitials.setManaged(false);
                        }
                    });
                } catch (Exception e) {
                    System.err.println("Failed to load header avatar: " + e.getMessage());
                }
            }, "header-avatar-loader");
            loader.setDaemon(true);
            loader.start();
        }
    }

    @FXML
    private void goToJobs() throws IOException {
        // Role-aware: admins see management, regular users see public jobs
        if (App.getCurrentUser() != null && App.getCurrentUser().isIsAdmin()) {
            App.setRoot("jobs-management");
        } else {
            App.setRoot("jobs");
        }
    }

    @FXML
    private void goToJobsPublic() {
        System.out.println("DEBUG: goToJobsPublic called");
        try {
            App.setRoot("jobs");
            System.out.println("DEBUG: App.setRoot('jobs') successful");
        } catch (IOException e) {
            System.err.println("DEBUG: Error in goToJobsPublic: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void goToJobsAdmin() throws IOException {
        App.setRoot("jobs-management");
    }

    @FXML
    private void goToDashboard() throws IOException {
        // App.setRoot("secondary");
    }

    @FXML
    private void goToMessages() throws IOException {
        App.setRoot("Conversation");
    }

    @FXML
    private void goToBlogs() throws IOException {
         System.out.println("Navigating to Blogs...");
        App.setRoot("Article/AjouterArticle");
    }

    @FXML
    private void goToReviews() throws IOException {
        App.setRoot("review");
    }

    @FXML
    private void goToProfile() throws IOException {
         System.out.println("Navigating to Profile...");
        App.setRoot("profile");
    }

    @FXML
    private void goToArticles() throws IOException {
        App.setRoot("Article/ArticlesPublications");
    }

    @FXML
    private void goToCategory() throws IOException {
        App.setRoot("category");
    }

    @FXML
    private void goToGig() throws IOException {
        App.setRoot("gig");
    }
}
