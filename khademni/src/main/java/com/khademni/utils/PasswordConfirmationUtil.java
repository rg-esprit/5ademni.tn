package com.khademni.utils;

import com.khademni.App;
import com.khademni.controller.PasswordConfirmationController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class PasswordConfirmationUtil {

    public static boolean showConfirmation() {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("password_confirmation.fxml"));
            Parent root = loader.load();
            PasswordConfirmationController controller = loader.getController();

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Confirmation de mot de passe");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            return controller.isConfirmed();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
