package com.khademni;


import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;


import java.io.IOException;


/**
 * JavaFX App
 */
public class App extends Application {

    private static Scene scene;
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;
        

        
        scene = new Scene(loadFXML("login"), 540, 700);
        scene.getStylesheets().add(App.class.getResource("login.css").toExternalForm());
        stage.setTitle("5ademni.tn — Sign In");
        stage.setScene(scene);
        stage.show();
    }

    public static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
        
        // Update title and CSS based on the scene
        if (fxml.equals("login")) {
            primaryStage.setTitle("5ademni.tn — Sign In");
            scene.getStylesheets().clear();
            scene.getStylesheets().add(App.class.getResource("login.css").toExternalForm());
        } else if (fxml.equals("signup")) {
            primaryStage.setTitle("5ademni.tn — Create Account");
            scene.getStylesheets().clear();
            scene.getStylesheets().add(App.class.getResource("signup.css").toExternalForm());
        }
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        launch();
    }

}