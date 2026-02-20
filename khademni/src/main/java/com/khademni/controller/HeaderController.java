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
         System.out.println("Navigating to Blogs...");
        App.setRoot("Article/AjouterArticle");
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
}
