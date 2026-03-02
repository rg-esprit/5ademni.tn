package com.khademni.controller;

import java.io.IOException;
import java.io.InputStream;

import com.khademni.App;
import com.khademni.model.UserModel;
import com.khademni.utils.VercelBlobUploader;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;

public class HeaderController {

    @FXML
    private Button jobsManagementLink;

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

        if (headerAvatarInitials != null) {
            String initials = "";
            if (user.getFirstName() != null && !user.getFirstName().isEmpty())
                initials += user.getFirstName().charAt(0);
            if (user.getLastName() != null && !user.getLastName().isEmpty())
                initials += user.getLastName().charAt(0);
            headerAvatarInitials.setText(initials.toUpperCase());
        }

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
    private void onLogout(ActionEvent event) {
        App.setCurrentUser(null);
        try {
            App.setRoot("login");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onProfile(ActionEvent event) {
        try {
            App.setRoot("profile");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onJobsAndServices(ActionEvent event) {
        try {
            App.setRoot("offre");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onContrat(ActionEvent event) {
        try {
            App.setRoot("contrat");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onPaiement(ActionEvent event) {
        try {
            App.setRoot("paiement");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onFreelancer(ActionEvent event) {
        try {
            App.setRoot("freelancer_space");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onClient(ActionEvent event) {
        try {
            App.setRoot("client_space");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onReviews(ActionEvent event) {
        try {
            App.setRoot("review");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goToJobsAdmin(ActionEvent event) {
        try {
            App.setRoot("jobs-management");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goToBlogs() throws IOException {
        App.setRoot("Article/AjouterArticle");
    }

    @FXML
    private void goToArticles() throws IOException {
        App.setRoot("Article/ArticlesPublications");
    }
}
