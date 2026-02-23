package com.khademni.controller;

import com.khademni.App;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

import java.io.IOException;

public class HeaderController {

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

}
