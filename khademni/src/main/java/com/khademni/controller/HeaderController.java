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
        // App.setRoot("messages");
    }

    @FXML
    private void goToBlogs() throws IOException {
        // App.setRoot("blogs");
    }

    @FXML
    private void goToProfile() throws IOException {
        App.setRoot("profile");
    }

    @FXML
    private void goToContrat() throws IOException {
        App.setRoot("contrat");
    }

    @FXML
    private void goToFreelancerSpace() throws IOException {
        App.setRoot("freelancer_space");
    }

    @FXML
    private void goToClientSpace() throws IOException {
        App.setRoot("client_space");
    }

    @FXML
    private void goToPaiement() throws IOException {
        App.setRoot("paiement");
    }
}
