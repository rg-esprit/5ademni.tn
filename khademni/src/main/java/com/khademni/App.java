package com.khademni;

import com.khademni.utils.MyDataBase;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

// user model
import com.khademni.model.UserModel;
import com.khademni.utils.SessionManager;

import java.io.IOException;

/**
 * JavaFX App
 */
public class App extends Application {

    private static Scene scene;
    private static Stage primaryStage;
    public static UserModel currentUser;

    @Override
    public void start(Stage stage) throws IOException {
        MyDataBase.init();
        primaryStage = stage;

        scene = new Scene(loadFXML("login"), 540, 700);
        scene.getStylesheets().add(App.class.getResource("login.css").toExternalForm());
        stage.setTitle("5ademni.tn — Sign In");
        stage.setFullScreen(true);
        stage.setScene(scene);
        stage.show();
    }

    public static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));

        // Update title and CSS based on the scene
        scene.getStylesheets().clear();
        if (fxml.equals("login")) {
            primaryStage.setTitle("5ademni.tn — Sign In");
            scene.getStylesheets().add(App.class.getResource("login.css").toExternalForm());
        } else if (fxml.equals("signup")) {
            primaryStage.setTitle("5ademni.tn — Create Account");
            scene.getStylesheets().add(App.class.getResource("signup.css").toExternalForm());
        } else if (fxml.equals("profile")) {
            primaryStage.setTitle("5ademni.tn — My Profile");
            scene.getStylesheets().add(App.class.getResource("profile.css").toExternalForm());
        } else if (fxml.equals("jobs")) {
            primaryStage.setTitle("5ademni.tn — Jobs & Opportunities");
            scene.getStylesheets().add(App.class.getResource("jobs.css").toExternalForm());
        } else if (fxml.equals("jobs-management")) {
            primaryStage.setTitle("5ademni.tn — Jobs Management");
            scene.getStylesheets().add(App.class.getResource("jobs-management.css").toExternalForm());
        } else if (fxml.equals("category")) {
            primaryStage.setTitle("5ademni.tn — Categories");
            scene.getStylesheets().add(App.class.getResource("category.css").toExternalForm());
        } else if (fxml.equals("gig")) {
            primaryStage.setTitle("5ademni.tn — Gigs");
            scene.getStylesheets().add(App.class.getResource("gig.css").toExternalForm());
        } else if (fxml.equals("review")) {
            primaryStage.setTitle("5ademni.tn — Reviews");
            scene.getStylesheets().add(App.class.getResource("review.css").toExternalForm());
        }
        else if (fxml.equals("Conversation")) {
            primaryStage.setTitle("5ademni.tn — Mes Discussions");
            // On peut réutiliser profile.css ou créer un chat.css spécifique
            scene.getStylesheets().add(App.class.getResource("Conversation.css").toExternalForm());
        } else if (fxml.equals("Message")) {
            primaryStage.setTitle("5ademni.tn — Chat");
            scene.getStylesheets().add(App.class.getResource("Message.css").toExternalForm());
        }
        
        // Only set full screen if it's not already, to avoid focus issues
        if (!primaryStage.isFullScreen()) {
            primaryStage.setFullScreen(true);
        }
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        launch();
    }

    public static UserModel getCurrentUser() {
        return currentUser;
        
    }

    public static void setCurrentUser(UserModel user) {
        currentUser = user;
         SessionManager.setCurrentUser(user); 
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

}
