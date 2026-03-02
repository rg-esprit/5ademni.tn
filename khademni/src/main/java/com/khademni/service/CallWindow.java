package com.khademni.service;

import javafx.application.Platform;
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

import javax.sound.sampled.*;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;

public class CallWindow {

    private Stage stage;
    private String participantName;
    private boolean isIncoming;
    private CallClient callClient;
    private boolean isVideoCall;
    private MediaPlayer ringtonePlayer;
    private boolean isRinging = false;
    private Thread ringtoneThread;
    private Thread timerThread;
    private Runnable onCloseCallback;

    // Attributs audio
    private TargetDataLine microphone;
    private boolean isMicroMuted = false;
    private boolean isSpeakerOn = true;

    // Enregistrement vocal
    private boolean isRecording = false;
    private ByteArrayOutputStream audioOutputStream;
    private Thread recordingThread;
    private AudioFormat audioFormat;

    public CallWindow(String participantName, boolean isIncoming, CallClient callClient, boolean isVideoCall) {
        this.participantName = participantName;
        this.isIncoming = isIncoming;
        this.callClient = callClient;
        this.isVideoCall = isVideoCall;
        this.audioFormat = new AudioFormat(16000, 16, 1, true, true);

        if (!isIncoming) {
            initMicrophone();
        }

        createWindow();

        if (!isIncoming) {
            Platform.runLater(() -> show());
        }
    }

    public CallWindow(String participantName, boolean isIncoming, CallClient callClient) {
        this(participantName, isIncoming, callClient, false);
    }

    public void setOnCloseCallback(Runnable callback) {
        this.onCloseCallback = callback;
    }

    private void createWindow() {
        stage = new Stage();
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setTitle(isIncoming ? "Appel entrant" : (isVideoCall ? "Appel vidéo" : "Appel audio"));

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #1e293b, #0f172a);");
        root.setPadding(new Insets(30));

        // Centre
        VBox centerBox = new VBox(20);
        centerBox.setAlignment(Pos.CENTER);

        String icon = isVideoCall ? "📹" : "📞";
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 64px; -fx-text-fill: white;");

        Label nameLabel = new Label(participantName);
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");

        // Status avec message vocal optionnel
        VBox statusBox = new VBox(10);
        statusBox.setAlignment(Pos.CENTER);

        String statusText = isIncoming ? "Appel entrant..." :
                (isVideoCall ? "Appel vidéo en cours..." : "Appel en cours...");
        Label statusLabel = new Label(statusText);
        statusLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14px;");

        Button voiceMessageBtn = new Button("🎤 Laisser un message vocal");
        voiceMessageBtn.setStyle("-fx-background-color: #8b5cf6; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20; -fx-background-radius: 20; -fx-cursor: hand;");
        voiceMessageBtn.setVisible(false);
        voiceMessageBtn.setManaged(false);
        voiceMessageBtn.setOnAction(e -> openVoiceMessageDialog());

        statusBox.getChildren().addAll(statusLabel, voiceMessageBtn);

        // Timer
        Label timerLabel = new Label("00:00");
        timerLabel.setStyle("-fx-text-fill: #10b981; -fx-font-size: 18px; -fx-font-family: monospace;");

        centerBox.getChildren().addAll(iconLabel, nameLabel, statusBox, timerLabel);
        root.setCenter(centerBox);

        // Boutons
        HBox buttonBox = new HBox(20);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(20, 0, 0, 0));

        if (isIncoming) {
            // APPEL ENTRANT
            Button acceptBtn = new Button("✅ Accepter");
            acceptBtn.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-size: 16px; -fx-padding: 10 30; -fx-cursor: hand; -fx-background-radius: 5;");
            acceptBtn.setOnAction(e -> acceptCall());

            Button rejectBtn = new Button("❌ Refuser");
            rejectBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-size: 16px; -fx-padding: 10 30; -fx-cursor: hand; -fx-background-radius: 5;");
            rejectBtn.setOnAction(e -> rejectCall());

            buttonBox.getChildren().addAll(acceptBtn, rejectBtn);
            playRingtone();

        } else {
            // APPEL EN COURS
            Button muteBtn = new Button("🎤");
            muteBtn.setStyle("-fx-background-color: #334155; -fx-text-fill: white; -fx-font-size: 18px; -fx-min-width: 50; -fx-min-height: 50; -fx-background-radius: 25; -fx-cursor: hand;");
            muteBtn.setOnAction(e -> toggleMicrophone(muteBtn));

            Button endBtn = new Button("🔴");
            endBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-size: 18px; -fx-min-width: 60; -fx-min-height: 60; -fx-background-radius: 30; -fx-cursor: hand;");
            endBtn.setOnAction(e -> endCall());

            Button speakerBtn = new Button("🔊");
            speakerBtn.setStyle("-fx-background-color: #334155; -fx-text-fill: white; -fx-font-size: 18px; -fx-min-width: 50; -fx-min-height: 50; -fx-background-radius: 25; -fx-cursor: hand;");
            speakerBtn.setOnAction(e -> toggleSpeaker(speakerBtn));

            if (isVideoCall) {
                Button videoBtn = new Button("📹");
                videoBtn.setStyle("-fx-background-color: #334155; -fx-text-fill: white; -fx-font-size: 18px; -fx-min-width: 50; -fx-min-height: 50; -fx-background-radius: 25; -fx-cursor: hand;");
                videoBtn.setOnAction(e -> toggleVideo());
                buttonBox.getChildren().add(videoBtn);
            }

            buttonBox.getChildren().addAll(muteBtn, endBtn, speakerBtn);
            startTimer(timerLabel, statusLabel, voiceMessageBtn);
        }

        root.setBottom(buttonBox);

        Scene scene = new Scene(root, 400, 500);
        stage.setScene(scene);

        stage.setOnCloseRequest(e -> {
            if (!isIncoming) {
                endCall();
            }
            if (onCloseCallback != null) {
                onCloseCallback.run();
            }
        });
    }

    private void toggleVideo() {
        System.out.println("📹 Vidéo togglée");
    }

    private void toggleMicrophone(Button muteBtn) {
        if (callClient != null) {
            callClient.toggleMicrophone();

            if (callClient.isMicroMuted()) {
                muteBtn.setText("🔇");
                showToast("Micro coupé");
            } else {
                muteBtn.setText("🎤");
                showToast("Micro activé");
            }
        } else {
            if (microphone != null && microphone.isOpen()) {
                if (muteBtn.getText().equals("🎤")) {
                    microphone.stop();
                    isMicroMuted = true;
                    muteBtn.setText("🔇");
                    showToast("Micro coupé");
                } else {
                    microphone.start();
                    isMicroMuted = false;
                    muteBtn.setText("🎤");
                    showToast("Micro activé");
                }
            }
        }
    }

    private void toggleSpeaker(Button speakerBtn) {
        if (callClient != null) {
            callClient.toggleSpeaker();

            if (callClient.isSpeakerOn()) {
                speakerBtn.setText("🔊");
                showToast("Haut-parleur activé");
            } else {
                speakerBtn.setText("🔈");
                showToast("Haut-parleur désactivé");
            }
        } else {
            if (speakerBtn.getText().equals("🔊")) {
                speakerBtn.setText("🔈");
                isSpeakerOn = false;
                showToast("Haut-parleur désactivé");
            } else {
                speakerBtn.setText("🔊");
                isSpeakerOn = true;
                showToast("Haut-parleur activé");
            }
        }
    }

    private void playRingtone() {
        try {
            URL soundUrl = getClass().getResource("/sounds/ringtone.mp3");
            if (soundUrl == null) {
                soundUrl = getClass().getResource("/sounds/ringtonet.mp3");
            }

            if (soundUrl == null) {
                System.err.println("❌ Fichier sonnerie introuvable");
                beepRingtone();
                return;
            }

            Media ringtone = new Media(soundUrl.toExternalForm());
            ringtonePlayer = new MediaPlayer(ringtone);
            ringtonePlayer.setCycleCount(MediaPlayer.INDEFINITE);
            ringtonePlayer.setVolume(0.8);
            ringtonePlayer.play();

        } catch (Exception e) {
            System.err.println("❌ Erreur sonnerie: " + e.getMessage());
            beepRingtone();
        }
    }

    private void beepRingtone() {
        isRinging = true;
        ringtoneThread = new Thread(() -> {
            while (isRinging) {
                Toolkit.getDefaultToolkit().beep();
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                    break;
                }
            }
        });
        ringtoneThread.setDaemon(true);
        ringtoneThread.start();
    }

    private void stopRingtone() {
        isRinging = false;
        if (ringtonePlayer != null) {
            ringtonePlayer.stop();
            ringtonePlayer = null;
        }
        if (ringtoneThread != null) {
            ringtoneThread.interrupt();
            ringtoneThread = null;
        }
    }

    private void startTimer(Label timerLabel, Label statusLabel, Button voiceMessageBtn) {
        timerThread = new Thread(() -> {
            int seconds = 0;
            boolean userOfflineNotified = false;

            while (stage != null && stage.isShowing()) {
                try {
                    Thread.sleep(1000);
                    seconds++;
                    int minutes = seconds / 60;
                    int secs = seconds % 60;
                    String time = String.format("%02d:%02d", minutes, secs);
                    Platform.runLater(() -> timerLabel.setText(time));

                    // Après 10 secondes sans réponse, afficher l'option message vocal
                    if (seconds == 10 && !userOfflineNotified) {
                        Platform.runLater(() -> {
                            statusLabel.setText("👤 Utilisateur hors ligne");
                            statusLabel.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 14px; -fx-font-weight: bold;");
                            voiceMessageBtn.setVisible(true);
                            voiceMessageBtn.setManaged(true);
                        });
                        userOfflineNotified = true;
                    }

                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        timerThread.setDaemon(true);
        timerThread.start();
    }

    private void openVoiceMessageDialog() {
        Stage voiceStage = new Stage();
        voiceStage.setTitle("Laisser un message vocal");
        voiceStage.initStyle(StageStyle.UTILITY);

        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: white; -fx-background-radius: 10;");

        Label titleLabel = new Label("🎤 Laisser un message vocal");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1f2937;");

        Label instructionLabel = new Label("Parlez après avoir cliqué sur 'Enregistrer'");
        instructionLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #6b7280;");

        Label timerLabel = new Label("00:00");
        timerLabel.setStyle("-fx-font-size: 24px; -fx-font-family: monospace; -fx-text-fill: #8b5cf6; -fx-font-weight: bold;");

        Button recordBtn = new Button("⏺️ Enregistrer");
        recordBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 25; -fx-background-radius: 25; -fx-cursor: hand; -fx-font-weight: bold;");

        Button stopBtn = new Button("⏹️ Arrêter");
        stopBtn.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 25; -fx-background-radius: 25; -fx-cursor: hand; -fx-font-weight: bold;");
        stopBtn.setVisible(false);
        stopBtn.setManaged(false);

        Button sendBtn = new Button("📤 Envoyer");
        sendBtn.setStyle("-fx-background-color: #8b5cf6; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 25; -fx-background-radius: 25; -fx-cursor: hand; -fx-font-weight: bold;");
        sendBtn.setVisible(false);
        sendBtn.setManaged(false);

        Button cancelBtn = new Button("❌ Annuler");
        cancelBtn.setStyle("-fx-background-color: #6b7280; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 25; -fx-background-radius: 25; -fx-cursor: hand;");

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.getChildren().addAll(recordBtn, cancelBtn);

        root.getChildren().addAll(titleLabel, instructionLabel, timerLabel, buttonBox, stopBtn, sendBtn);

        Scene scene = new Scene(root, 400, 400);
        voiceStage.setScene(scene);
        voiceStage.show();

        // Logique d'enregistrement
        final ByteArrayOutputStream[] audioStream = new ByteArrayOutputStream[1];
        final Thread[] recordThread = new Thread[1];

        recordBtn.setOnAction(e -> {
            recordBtn.setVisible(false);
            recordBtn.setManaged(false);
            stopBtn.setVisible(true);
            stopBtn.setManaged(true);
            buttonBox.getChildren().remove(recordBtn);
            buttonBox.getChildren().add(0, stopBtn);

            instructionLabel.setText("🎤 Enregistrement en cours... Parlez maintenant");

            // Démarrer l'enregistrement
            audioStream[0] = new ByteArrayOutputStream();

            recordThread[0] = new Thread(() -> {
                try {
                    DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);
                    TargetDataLine mic = (TargetDataLine) AudioSystem.getLine(info);
                    mic.open(audioFormat);
                    mic.start();

                    byte[] buffer = new byte[4096];
                    int seconds = 0;

                    while (!Thread.currentThread().isInterrupted()) {
                        int bytesRead = mic.read(buffer, 0, buffer.length);
                        if (bytesRead > 0) {
                            audioStream[0].write(buffer, 0, bytesRead);

                            // Mettre à jour le timer
                            seconds++;
                            int minutes = seconds / 60;
                            int secs = seconds % 60;
                            String time = String.format("%02d:%02d", minutes, secs);
                            Platform.runLater(() -> timerLabel.setText(time));
                        }
                        Thread.sleep(100);
                    }

                    mic.stop();
                    mic.close();

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
            recordThread[0].setDaemon(true);
            recordThread[0].start();
        });

        stopBtn.setOnAction(e -> {
            if (recordThread[0] != null) {
                recordThread[0].interrupt();
            }

            stopBtn.setVisible(false);
            stopBtn.setManaged(false);
            sendBtn.setVisible(true);
            sendBtn.setManaged(true);
            cancelBtn.setVisible(false);
            cancelBtn.setManaged(false);

            instructionLabel.setText("✅ Enregistrement terminé");
        });

        sendBtn.setOnAction(e -> {
            if (audioStream[0] != null && callClient != null) {
                byte[] audioData = audioStream[0].toByteArray();
                String duration = timerLabel.getText();

                String targetId = callClient.getCurrentCallUser();
                if (targetId != null) {
                    callClient.sendVoiceMessage(targetId, audioData, duration);
                    showToast("✅ Message vocal envoyé à " + participantName);
                }
            }

            voiceStage.close();
            endCall();
        });

        cancelBtn.setOnAction(e -> {
            if (recordThread[0] != null) {
                recordThread[0].interrupt();
            }
            voiceStage.close();
        });
    }

    private void stopTimer() {
        if (timerThread != null) {
            timerThread.interrupt();
            timerThread = null;
        }
    }

    private void showToast(String message) {
        System.out.println("🔔 " + message);
    }

    private void acceptCall() {
        stopRingtone();
        if (callClient != null) {
            callClient.acceptCall(participantName);
        }
        stage.close();

        Platform.runLater(() -> {
            CallWindow ongoingCall = new CallWindow(participantName, false, callClient, isVideoCall);
            ongoingCall.show();
        });
    }

    private void rejectCall() {
        stopRingtone();
        if (callClient != null) {
            callClient.rejectCall(participantName);
        }
        stage.close();
    }

    private void endCall() {
        stopTimer();
        stopRingtone();

        if (callClient != null) {
            callClient.endCall();
        }

        if (microphone != null) {
            try {
                microphone.stop();
                microphone.close();
            } catch (Exception e) {
                // Ignorer
            }
            microphone = null;
        }

        if (stage != null) {
            stage.close();
        }

        if (onCloseCallback != null) {
            onCloseCallback.run();
        }
    }

    public void show() {
        if (stage != null) {
            stage.show();
            stage.toFront();
        }
    }

    private void initMicrophone() {
        try {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);
            microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(audioFormat);
            microphone.start();
            System.out.println("✅ Microphone initialisé");
        } catch (LineUnavailableException e) {
            System.err.println("❌ Erreur initialisation microphone: " + e.getMessage());
        }
    }
}