package com.khademni.controller;

import java.io.IOException;

import com.khademni.App;

import javafx.fxml.FXML;

public class HeaderController {

    @FXML
    private void goToJobs() throws IOException {
        // App.setRoot("primary");
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
        // App.setRoot("blogs");
    }

    @FXML
    private void goToProfile() throws IOException {
        App.setRoot("profile");
    }


}
