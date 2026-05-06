package com.khademni.utils;

import com.khademni.App;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import javafx.scene.control.TextField;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class CustomAlert {

    public static void showInfo(String title, String message) {
        show(Alert.AlertType.INFORMATION, title, message);
    }

    public static void showSuccess(String title, String message) {
        show(Alert.AlertType.CONFIRMATION, title, message); // We'll treat confirmation as success if no Choice is needed
    }

    public static void showWarning(String title, String message) {
        show(Alert.AlertType.WARNING, title, message);
    }

    public static void showError(String title, String message) {
        show(Alert.AlertType.ERROR, title, message);
    }

    public static void show(Alert.AlertType type, String title, String message) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.initOwner(App.getPrimaryStage());

        VBox root = new VBox(20);
        root.setPadding(new Insets(30));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: white; -fx-background-radius: 24; -fx-border-radius: 24;");
        
        DropShadow ds = new DropShadow();
        ds.setRadius(30);
        ds.setColor(Color.rgb(0, 0, 0, 0.15));
        root.setEffect(ds);

        // Icon based on type
        Label icon = new Label();
        String iconChar = "ℹ";
        String iconColor = "#3b82f6";
        String bgColor = "#eff6ff";

        switch (type) {
            case ERROR:
                iconChar = "✕";
                iconColor = "#ef4444";
                bgColor = "#fef2f2";
                break;
            case WARNING:
                iconChar = "⚠";
                iconColor = "#f59e0b";
                bgColor = "#fffbeb";
                break;
            case CONFIRMATION:
            case INFORMATION:
                iconChar = "✓";
                iconColor = "#10b981";
                bgColor = "#ecfdf5";
                break;
        }

        icon.setText(iconChar);
        icon.setStyle("-fx-font-size: 32; -fx-text-fill: " + iconColor + "; -fx-background-color: " + bgColor + 
                     "; -fx-background-radius: 100; -fx-min-width: 70; -fx-min-height: 70; -fx-alignment: center;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 20; -fx-font-weight: 800; -fx-text-fill: #111827;");

        Label msgLabel = new Label(message);
        msgLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #4b5563; -fx-text-alignment: center;");
        msgLabel.setWrapText(true);
        msgLabel.setMaxWidth(340);

        Button btn = new Button("D'accord");
        btn.setMinWidth(120);
        btn.setStyle("-fx-background-color: #111827; -fx-text-fill: white; -fx-font-weight: 700; " +
                    "-fx-padding: 12 24; -fx-background-radius: 12; -fx-cursor: hand;");
        btn.setOnAction(e -> stage.close());

        root.getChildren().addAll(icon, titleLabel, msgLabel, btn);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);
        stage.showAndWait();
    }

    public static boolean confirmDelete(String title, String message) {
        AtomicBoolean result = new AtomicBoolean(false);
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.initOwner(App.getPrimaryStage());

        VBox root = new VBox(20);
        root.setPadding(new Insets(30));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: white; -fx-background-radius: 24; -fx-border-radius: 24;");
        
        DropShadow ds = new DropShadow();
        ds.setRadius(30);
        ds.setColor(Color.rgb(0, 0, 0, 0.15));
        root.setEffect(ds);

        Label icon = new Label("🗑");
        icon.setStyle("-fx-font-size: 32; -fx-text-fill: #ef4444; -fx-background-color: #fef2f2; " +
                     "-fx-background-radius: 100; -fx-min-width: 70; -fx-min-height: 70; -fx-alignment: center;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 20; -fx-font-weight: 800; -fx-text-fill: #111827;");

        Label msgLabel = new Label(message);
        msgLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #4b5563; -fx-text-alignment: center;");
        msgLabel.setWrapText(true);
        msgLabel.setMaxWidth(340);

        HBox btns = new HBox(12);
        btns.setAlignment(Pos.CENTER);

        Button cancelBtn = new Button("Annuler");
        cancelBtn.setStyle("-fx-background-color: #f3f4f6; -fx-text-fill: #374151; -fx-font-weight: 700; " +
                         "-fx-padding: 12 24; -fx-background-radius: 12; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> stage.close());

        Button confirmBtn = new Button("Supprimer");
        confirmBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: 700; " +
                          "-fx-padding: 12 24; -fx-background-radius: 12; -fx-cursor: hand;");
        confirmBtn.setOnAction(e -> {
            result.set(true);
            stage.close();
        });

        btns.getChildren().addAll(cancelBtn, confirmBtn);
        root.getChildren().addAll(icon, titleLabel, msgLabel, btns);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);
        stage.showAndWait();
        
        return result.get();
    }

    public static Optional<String> showOTPDialog(String title, String message) {
        AtomicReference<String> result = new AtomicReference<>(null);
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.initOwner(App.getPrimaryStage());

        VBox root = new VBox(20);
        root.setPadding(new Insets(30));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: white; -fx-background-radius: 24; -fx-border-radius: 24;");
        
        DropShadow ds = new DropShadow();
        ds.setRadius(30);
        ds.setColor(Color.rgb(0, 0, 0, 0.15));
        root.setEffect(ds);

        Label icon = new Label("✉");
        icon.setStyle("-fx-font-size: 32; -fx-text-fill: #6c5ce7; -fx-background-color: #f3f0ff; " +
                     "-fx-background-radius: 100; -fx-min-width: 70; -fx-min-height: 70; -fx-alignment: center;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 20; -fx-font-weight: 800; -fx-text-fill: #111827;");

        Label msgLabel = new Label(message);
        msgLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #4b5563; -fx-text-alignment: center;");
        msgLabel.setWrapText(true);
        msgLabel.setMaxWidth(340);

        TextField otpField = new TextField();
        otpField.setPromptText("Code OTP");
        otpField.setMaxWidth(200);
        otpField.setStyle("-fx-background-color: #f9fafb; -fx-border-color: #e5e7eb; -fx-border-radius: 12; " +
                         "-fx-background-radius: 12; -fx-padding: 12; -fx-font-size: 16; -fx-alignment: center; -fx-font-weight: bold;");

        HBox btns = new HBox(12);
        btns.setAlignment(Pos.CENTER);

        Button cancelBtn = new Button("Annuler");
        cancelBtn.setStyle("-fx-background-color: #f3f4f6; -fx-text-fill: #374151; -fx-font-weight: 700; " +
                         "-fx-padding: 12 24; -fx-background-radius: 12; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> stage.close());

        Button verifyBtn = new Button("Vérifier");
        verifyBtn.setStyle("-fx-background-color: #6c5ce7; -fx-text-fill: white; -fx-font-weight: 700; " +
                          "-fx-padding: 12 24; -fx-background-radius: 12; -fx-cursor: hand;");
        verifyBtn.setOnAction(e -> {
            result.set(otpField.getText());
            stage.close();
        });

        btns.getChildren().addAll(cancelBtn, verifyBtn);
        root.getChildren().addAll(icon, titleLabel, msgLabel, otpField, btns);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);
        stage.showAndWait();
        
        return Optional.ofNullable(result.get());
    }
}
