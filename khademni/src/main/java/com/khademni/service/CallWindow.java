package com.khademni.service;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class CallWindow {

    private Stage stage;
    private String participantName;
    private boolean isIncoming;
    private CallClient callClient;
    private MediaPlayer ringtonePlayer;

    public CallWindow(String participantName, boolean isIncoming, CallClient callClient) {
        this.participantName = participantName;
        this.isIncoming = isIncoming;
        this.callClient = callClient;
        createWindow();
    }

    private void createWindow() {
        stage = new Stage();
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setTitle("Appel");

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #1a1a1a;");
        root.setPadding(new Insets(20));

        // Centre: Informations de l'appel
        VBox centerBox = new VBox(20);
        centerBox.setAlignment(Pos.CENTER);

        Label statusLabel = new Label(isIncoming ? "Appel entrant..." : "Appel en cours...");
        statusLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px;");

        Label nameLabel = new Label(participantName);
        nameLabel.setStyle("-fx-text-fill: #2563eb; -fx-font-size: 24px; -fx-font-weight: bold;");

        centerBox.getChildren().addAll(nameLabel, statusLabel);
        root.setCenter(centerBox);

        // Bas: Boutons
        HBox buttonBox = new HBox(20);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(20));

        if (isIncoming) {
            Button acceptBtn = new Button("✅ Accepter");
            acceptBtn.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-size: 16px; -fx-padding: 10 30;");
            acceptBtn.setOnAction(e -> acceptCall());

            Button rejectBtn = new Button("❌ Refuser");
            rejectBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-size: 16px; -fx-padding: 10 30;");
            rejectBtn.setOnAction(e -> rejectCall());

            buttonBox.getChildren().addAll(acceptBtn, rejectBtn);

            // Jouer la sonnerie
            playRingtone();

        } else {
            Button endBtn = new Button("🔴 Raccrocher");
            endBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-size: 16px; -fx-padding: 10 30;");
            endBtn.setOnAction(e -> endCall());

            buttonBox.getChildren().add(endBtn);
        }

        root.setBottom(buttonBox);

        Scene scene = new Scene(root, 400, 300);
        stage.setScene(scene);
    }

    private void playRingtone() {
        try {
            String ringtoneFile = getClass().getResource("/sounds/ringtone.mp3").toExternalForm();
            Media ringtone = new Media(ringtoneFile);
            ringtonePlayer = new MediaPlayer(ringtone);
            ringtonePlayer.setCycleCount(MediaPlayer.INDEFINITE);
            ringtonePlayer.play();
        } catch (Exception e) {
            System.err.println("Erreur sonnerie: " + e.getMessage());
        }
    }

    private void acceptCall() {
        if (ringtonePlayer != null) ringtonePlayer.stop();
        callClient.acceptCall(participantName);
        stage.close();
    }

    private void rejectCall() {
        if (ringtonePlayer != null) ringtonePlayer.stop();
        callClient.rejectCall(participantName);
        stage.close();
    }

    private void endCall() {
        callClient.endCall();
        stage.close();
    }

    public void show() {
        stage.show();
    }
}