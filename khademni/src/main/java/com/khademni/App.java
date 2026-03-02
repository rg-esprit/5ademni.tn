package com.khademni;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

// user model
import com.khademni.model.UserModel;
import com.khademni.controller.ChatbotController;
import com.khademni.utils.SessionManager;

import java.io.IOException;

/**
 * JavaFX App
 */
public class    App extends Application {

    private static Scene scene;
    private static Stage primaryStage;
    private static javafx.application.HostServices hostServices;
    public static UserModel currentUser;
    private static ChatbotController chatbotController;

    @Override
    public void start(Stage stage) throws IOException {
        com.khademni.utils.MyDataBase.init();
        primaryStage = stage;
        hostServices = getHostServices();

        scene = new Scene(loadFXML("login"), 540, 700);
        scene.getStylesheets().add(App.class.getResource("login.css").toExternalForm());
        stage.setTitle("5ademni.tn — Sign In");
        stage.setFullScreen(false); // Login page usually shouldn't be fullscreen by default
        stage.setScene(scene);
        stage.show();
    }

    public static void setRoot(String fxml) throws IOException {
        Parent fxmlRoot = loadFXML(fxml);

        // Update title and CSS based on the scene
        scene.getStylesheets().clear();

        if (fxml.equals("login")) {
            primaryStage.setTitle("5ademni.tn — Sign In");
            scene.getStylesheets().add(App.class.getResource("login.css").toExternalForm());
            scene.setRoot(fxmlRoot);

        } else if (fxml.equals("signup")) {
            primaryStage.setTitle("5ademni.tn — Create Account");
            scene.getStylesheets().add(App.class.getResource("signup.css").toExternalForm());
            scene.setRoot(fxmlRoot);

        } else if (fxml.equals("profile")) {
            primaryStage.setTitle("5ademni.tn — My Profile");
            scene.getStylesheets().add(App.class.getResource("profile.css").toExternalForm());
            scene.setRoot(wrapWithChatbot(fxmlRoot));

        } else if (fxml.equals("jobs")) {
            primaryStage.setTitle("5ademni.tn — Jobs & Opportunities");
            scene.getStylesheets().add(App.class.getResource("jobs.css").toExternalForm());
            scene.setRoot(wrapWithChatbot(fxmlRoot)); // change to fxmlRoot if you don't want chatbot here

        } else if (fxml.equals("jobs-management")) {
            primaryStage.setTitle("5ademni.tn — Jobs Management");
            scene.getStylesheets().add(App.class.getResource("jobs-management.css").toExternalForm());
            scene.setRoot(wrapWithChatbot(fxmlRoot));

        } else if (fxml.equals("review")) {
            primaryStage.setTitle("5ademni.tn — Reviews");
            scene.getStylesheets().add(App.class.getResource("review.css").toExternalForm());
            scene.setRoot(wrapWithChatbot(fxmlRoot));

        } else if (fxml.equals("contrat")) {
            primaryStage.setTitle("5ademni.tn — Gestion Contrats");
            scene.getStylesheets().add(App.class.getResource("contrat.css").toExternalForm());
            scene.setRoot(wrapWithChatbot(fxmlRoot));

        } else if (fxml.equals("paiement_form")) {
            primaryStage.setTitle("5ademni.tn — Paiement Manuel");
            scene.getStylesheets().add(App.class.getResource("contrat.css").toExternalForm());
            scene.setRoot(wrapWithChatbot(fxmlRoot));

        } else if (fxml.equals("Conversation")) {
            primaryStage.setTitle("5ademni.tn — Mes Discussions");
            scene.getStylesheets().add(App.class.getResource("Conversation.css").toExternalForm());
            scene.setRoot(wrapWithChatbot(fxmlRoot));

        } else if (fxml.equals("Message")) {
            primaryStage.setTitle("5ademni.tn — Chat");
            scene.getStylesheets().add(App.class.getResource("Message.css").toExternalForm());
            scene.setRoot(wrapWithChatbot(fxmlRoot));

        } else {
            // Default fallback
            primaryStage.setTitle("5ademni.tn");
            // Keep a safe default stylesheet only if it exists in your resources
            // scene.getStylesheets().add(App.class.getResource("app.css").toExternalForm());
            scene.setRoot(wrapWithChatbot(fxmlRoot));
        }

        // keep fullscreen behavior from your branch
        primaryStage.setFullScreen(true);
    }

    /**
     * Wraps a page root with the chatbot overlay if the user is logged in.
     */
    private static Parent wrapWithChatbot(Parent fxmlRoot) {
        if (currentUser != null) {
            if (chatbotController == null) {
                chatbotController = new ChatbotController();
            }
            StackPane wrapper = new StackPane();
            wrapper.getChildren().addAll(fxmlRoot, chatbotController.getOverlay());
            return wrapper;
        }
        return fxmlRoot;
    }

    public static void setRoot(Parent node) {
        scene.setRoot(node);
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        try {
            com.khademni.utils.SqlFixer.main(args);
        } catch (Exception e) {
            System.err.println("Database fix failed: " + e.getMessage());
        }
        launch();
    }

    public static UserModel getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(UserModel user) {
        currentUser = user;
        SessionManager.setCurrentUser(user);
        // Reset chatbot when user changes (login/logout)
        chatbotController = null;
    }

    public static javafx.application.HostServices getAppHostServices() {
        return hostServices;
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

}
