package com.khademni.controller;

import java.io.IOException;

import com.khademni.App;

import javafx.fxml.FXML;

public class HeaderController {

    @FXML
    private javafx.scene.control.Hyperlink jobsManagementLink;

    @FXML
    public void initialize() {
        if (App.getCurrentUser() != null && App.getCurrentUser().isIsAdmin()) {
            jobsManagementLink.setVisible(true);
            jobsManagementLink.setManaged(true);
        } else {
            jobsManagementLink.setVisible(false);
            jobsManagementLink.setManaged(false);
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
        // App.setRoot("messages");
    }

    @FXML
    private void goToBlogs() throws IOException {
        // App.setRoot("blogs");
    }

    @FXML
    private void goToReviews() throws IOException {
        App.setRoot("review");
    }

    @FXML
    private void goToProfile() throws IOException {
        App.setRoot("profile");
    }
}
