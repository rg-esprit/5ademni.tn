package com.khademni.controller;

import com.khademni.App;
import com.khademni.model.MessageModel;
import com.khademni.model.UserModel;
import com.khademni.utils.MyDataBase;
import com.khademni.service.IASummaryService;
import com.khademni.service.ChatWebSocketClient;
import com.khademni.service.WebSocketServerStarter;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Callback;
import javafx.util.Duration;
import javafx.concurrent.Task;

import javax.sound.sampled.*;
import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MessageController {

    @FXML private VBox mainContainer;
    @FXML private Label lblConversationInfo;
    @FXML private Button btnRetour;
    @FXML private Button btnVideoCall;
    @FXML private Button btnAudioCall;
    @FXML private Button btnInfo;
    @FXML private ListView<HBox> listMessages;
    @FXML private TextArea txtContenu;
    @FXML private Button btnEnvoyer;
    @FXML private Button btnEmoji;
    @FXML private Button btnPieceJointe;
    @FXML private Button btnPhoto;
    @FXML private Button btnMicro;
    @FXML private Button btnReunion;
    @FXML private Label errorLabel;
    @FXML private VBox typingIndicator;
    @FXML private Label lblTyping;
    @FXML private ScrollPane emojiPicker;
    @FXML private VBox emojiContainer;
    @FXML private VBox filePreviewContainer;
    @FXML private ImageView filePreviewImage;
    @FXML private Label filePreviewName;
    @FXML private Button filePreviewCancel;
    @FXML private ProgressBar uploadProgress;
    @FXML private Button btnSummary;

    private Connection connection;
    private ObservableList<HBox> messageDisplayList = FXCollections.observableArrayList();
    private Map<Integer, MessageModel> messagesMap = new HashMap<>();
    private Map<HBox, Integer> messageIdMap = new HashMap<>();

    private int currentConversationId = -1;
    private int clientId;
    private int freelanceId;
    private String currentUser;
    private String userRole;
    private UserModel connectedUser;

    private File selectedFile;
    private File selectedImage;
    private MediaPlayer mediaPlayer;
    private File audioFile;

    private TargetDataLine microphone;
    private ByteArrayOutputStream audioOutputStream;
    private Thread recordingThread;
    private boolean isRecording = false;

    // Pour les appels
    private MediaPlayer ringtonePlayer;
    private Stage callStage;
    private boolean isInCall = false;

    // WebSocket
    private ChatWebSocketClient webSocketClient;
    private ObjectMapper mapper = new ObjectMapper();

    private static final String UPLOAD_DIR = "uploads/";
    private static final String[] EMOJIS = {
            "😊", "😂", "❤️", "👍", "🔥", "😢", "😡", "🥳", "😎", "🤔",
            "👋", "🙏", "💪", "🤝", "👏", "🎉", "✨", "⭐", "💯", "✅",
            "❌", "⚠️", "📌", "📍", "💡", "📷", "🎥", "🎤", "📞", "💻",
            "🐶", "🐱", "🐼", "🐨", "🌍", "🌈", "⚽", "🏀", "🎮", "🎵"
    };

    public MessageController() {
        try {
            connection = MyDataBase.getConnection();
            new File(UPLOAD_DIR + "images/").mkdirs();
            new File(UPLOAD_DIR + "files/").mkdirs();
            new File(UPLOAD_DIR + "audio/").mkdirs();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void initialize() {
        if (connection == null) {
            showError("Connexion DB échouée !");
            return;
        }

        // Démarrer le serveur WebSocket
        WebSocketServerStarter.start();

        connectedUser = App.getCurrentUser();

        Font emojiFont = Font.font("Segoe UI Emoji", 14);
        txtContenu.setFont(emojiFont);

        setupMessageList();
        setupEmojiPicker();
        setupTypingIndicator();
        setupFilePreview();
        setupKeyboardShortcuts();

        btnVideoCall.setOnAction(e -> startVideoCall());
        btnAudioCall.setOnAction(e -> startAudioCall());
        btnInfo.setOnAction(e -> showConversationInfo());
        btnEmoji.setOnAction(e -> handleEmojiPicker());
        btnPieceJointe.setOnAction(e -> handleFileAttachment());
        btnPhoto.setOnAction(e -> handlePhotoAttachment());
        btnMicro.setOnAction(e -> handleVoiceMessage());
        btnReunion.setOnAction(e -> scheduleMeeting());
        btnSummary.setOnAction(e -> generateChatSummary());

        disableButtons(true);

        System.out.println("MessageController initialisé");
    }

    // ==================== MÉTHODES D'APPEL AMÉLIORÉES ====================

    @FXML
    private void startVideoCall() {
        if (currentConversationId == -1) {
            showError("Aucune conversation sélectionnée");
            return;
        }

        if (isInCall) {
            showError("Un appel est déjà en cours");
            return;
        }

        String participantName = getOtherParticipantName();

        // Jouer une sonnerie
        playRingtone();

        // Afficher la fenêtre d'appel
        showCallWindow(participantName, true);
    }

    @FXML
    private void startAudioCall() {
        if (currentConversationId == -1) {
            showError("Aucune conversation sélectionnée");
            return;
        }

        if (isInCall) {
            showError("Un appel est déjà en cours");
            return;
        }

        String participantName = getOtherParticipantName();

        // Jouer une sonnerie
        playRingtone();

        // Afficher la fenêtre d'appel
        showCallWindow(participantName, false);
    }

    private void playRingtone() {
        try {
            // Essayer de charger une sonnerie depuis les ressources
            String ringtonePath = getClass().getResource("/sounds/ringtone.mp3").toExternalForm();
            Media ringtone = new Media(ringtonePath);
            ringtonePlayer = new MediaPlayer(ringtone);
            ringtonePlayer.setCycleCount(MediaPlayer.INDEFINITE);
            ringtonePlayer.play();
        } catch (Exception e) {
            System.err.println("Sonnerie non trouvée, utilisation de beep système");
            // Fallback: beep système
            Toolkit.getDefaultToolkit().beep();
        }
    }

    private void stopRingtone() {
        if (ringtonePlayer != null) {
            ringtonePlayer.stop();
        }
    }

    private void showCallWindow(String participantName, boolean isVideo) {
        callStage = new Stage();
        callStage.initStyle(StageStyle.UNDECORATED);
        callStage.setTitle(isVideo ? "Appel vidéo" : "Appel audio");

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #1e293b, #0f172a);");
        root.setPadding(new Insets(30));

        // Centre: Informations de l'appel
        VBox centerBox = new VBox(20);
        centerBox.setAlignment(Pos.CENTER);

        // Icône selon le type d'appel
        Label iconLabel = new Label(isVideo ? "📹" : "📞");
        iconLabel.setStyle("-fx-font-size: 64px; -fx-text-fill: white;");

        Label statusLabel = new Label("Appel en cours...");
        statusLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14px;");

        Label nameLabel = new Label(participantName);
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");

        // Timer
        Label timerLabel = new Label("00:00");
        timerLabel.setStyle("-fx-text-fill: #10b981; -fx-font-size: 18px; -fx-font-family: monospace;");

        // Démarrer le timer
        startCallTimer(timerLabel);

        centerBox.getChildren().addAll(iconLabel, nameLabel, statusLabel, timerLabel);
        root.setCenter(centerBox);

        // Bas: Boutons de contrôle
        HBox buttonBox = new HBox(20);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(20, 0, 0, 0));

        // Bouton micro (mute/unmute)
        Button muteButton = new Button("🎤");
        muteButton.setStyle("-fx-background-color: #334155; -fx-text-fill: white; -fx-font-size: 18px; -fx-min-width: 50; -fx-min-height: 50; -fx-background-radius: 25;");
        muteButton.setOnAction(e -> {
            if (muteButton.getStyle().contains("#334155")) {
                muteButton.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-size: 18px; -fx-min-width: 50; -fx-min-height: 50; -fx-background-radius: 25;");
                showSuccess("Micro coupé");
            } else {
                muteButton.setStyle("-fx-background-color: #334155; -fx-text-fill: white; -fx-font-size: 18px; -fx-min-width: 50; -fx-min-height: 50; -fx-background-radius: 25;");
                showSuccess("Micro activé");
            }
        });

        // Bouton fin d'appel
        Button endButton = new Button("🔴");
        endButton.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-size: 18px; -fx-min-width: 60; -fx-min-height: 60; -fx-background-radius: 30;");
        endButton.setOnAction(e -> endCall());

        // Bouton haut-parleur
        Button speakerButton = new Button("🔊");
        speakerButton.setStyle("-fx-background-color: #334155; -fx-text-fill: white; -fx-font-size: 18px; -fx-min-width: 50; -fx-min-height: 50; -fx-background-radius: 25;");

        buttonBox.getChildren().addAll(muteButton, endButton, speakerButton);
        root.setBottom(buttonBox);

        // Simulation d'appel pour la démo
        simulateCallConnection();

        Scene scene = new Scene(root, 400, 500);
        callStage.setScene(scene);
        callStage.show();
    }

    private void startCallTimer(Label timerLabel) {
        Thread timerThread = new Thread(() -> {
            int seconds = 0;
            while (isInCall) {
                try {
                    Thread.sleep(1000);
                    seconds++;
                    int minutes = seconds / 60;
                    int secs = seconds % 60;
                    String time = String.format("%02d:%02d", minutes, secs);
                    Platform.runLater(() -> timerLabel.setText(time));
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        timerThread.setDaemon(true);
        timerThread.start();
    }

    private void simulateCallConnection() {
        isInCall = true;

        // Simuler la connexion après 2 secondes
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                Platform.runLater(() -> {
                    stopRingtone();
                    showSuccess("✅ Appel connecté !");
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void endCall() {
        isInCall = false;
        stopRingtone();

        if (callStage != null) {
            callStage.close();
        }

        showError("🔴 Appel terminé");
    }

    // ==================== FIN MÉTHODES D'APPEL ====================

    private void setupMessageList() {
        listMessages.setCellFactory(new Callback<ListView<HBox>, ListCell<HBox>>() {
            @Override
            public ListCell<HBox> call(ListView<HBox> param) {
                return new ListCell<HBox>() {
                    @Override
                    protected void updateItem(HBox item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setGraphic(null);
                        } else {
                            setGraphic(item);
                            setStyle("-fx-background-color: transparent; -fx-padding: 5;");
                            setFont(Font.font("Segoe UI Emoji", 13));
                        }
                    }
                };
            }
        });
    }

    private void setupEmojiPicker() {
        emojiContainer.getChildren().clear();

        GridPane emojiGrid = new GridPane();
        emojiGrid.setHgap(5);
        emojiGrid.setVgap(5);
        emojiGrid.setPadding(new Insets(10));

        int row = 0;
        int col = 0;

        for (String emoji : EMOJIS) {
            Button btn = new Button(emoji);
            btn.setStyle("-fx-font-size: 24; -fx-min-width: 50; -fx-min-height: 50; " +
                    "-fx-background-color: #f3f4f6; -fx-background-radius: 10; " +
                    "-fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 2, 0, 0, 1);");

            btn.setOnAction(e -> {
                txtContenu.appendText(emoji);
                emojiPicker.setVisible(false);
                txtContenu.requestFocus();
            });

            btn.setOnMouseEntered(e ->
                    btn.setStyle("-fx-font-size: 24; -fx-min-width: 50; -fx-min-height: 50; " +
                            "-fx-background-color: #e5e7eb; -fx-background-radius: 10; " +
                            "-fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 4, 0, 0, 2);")
            );

            btn.setOnMouseExited(e ->
                    btn.setStyle("-fx-font-size: 24; -fx-min-width: 50; -fx-min-height: 50; " +
                            "-fx-background-color: #f3f4f6; -fx-background-radius: 10; " +
                            "-fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 2, 0, 0, 1);")
            );

            emojiGrid.add(btn, col, row);

            col++;
            if (col >= 6) {
                col = 0;
                row++;
            }
        }

        emojiContainer.getChildren().add(emojiGrid);
        emojiPicker.setVisible(false);
    }


    private void setupTypingIndicator() {
        txtContenu.textProperty().addListener((obs, old, newVal) -> {
            if (!newVal.isEmpty() && currentConversationId != -1) {
                showTypingIndicator();
                PauseTransition pause = new PauseTransition(Duration.seconds(2));
                pause.setOnFinished(e -> hideTypingIndicator());
                pause.play();
            }
        });
    }

    private void setupFilePreview() {
        filePreviewContainer.setVisible(false);
        filePreviewCancel.setOnAction(e -> {
            selectedFile = null;
            selectedImage = null;
            filePreviewContainer.setVisible(false);
        });
    }

    private void setupKeyboardShortcuts() {
        txtContenu.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER && !event.isShiftDown()) {
                event.consume();
                handleEnvoyer();
            }
        });
    }

    private void showTypingIndicator() {
        if (typingIndicator != null) {
            lblTyping.setText(currentUser + " écrit...");
            typingIndicator.setVisible(true);
        }
    }

    private void hideTypingIndicator() {
        if (typingIndicator != null) {
            typingIndicator.setVisible(false);
        }
    }

    @FXML
    private void handleEmojiPicker() {
        if (emojiPicker.isVisible()) {
            emojiPicker.setVisible(false);
        } else {
            emojiPicker.setVisible(true);
        }
    }

    @FXML
    private void handleFileAttachment() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Joindre un fichier");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*"),
                new FileChooser.ExtensionFilter("PDF", "*.pdf"),
                new FileChooser.ExtensionFilter("Documents", "*.doc", "*.docx", "*.txt"),
                new FileChooser.ExtensionFilter("Archives", "*.zip", "*.rar")
        );

        selectedFile = fileChooser.showOpenDialog(btnPieceJointe.getScene().getWindow());
        if (selectedFile != null) {
            String fileUrl = saveFile(selectedFile, "files");
            if (fileUrl != null) {
                sendMessageWithAttachment(selectedFile.getName(), fileUrl, "file");
            }
        }
    }

    @FXML
    private void handlePhotoAttachment() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une photo");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png", "*.gif")
        );

        selectedImage = fileChooser.showOpenDialog(btnPhoto.getScene().getWindow());
        if (selectedImage != null) {
            String fileUrl = saveFile(selectedImage, "images");
            if (fileUrl != null) {
                sendMessageWithAttachment(selectedImage.getName(), fileUrl, "image");
            }
        }
    }

    @FXML
    private void handleVoiceMessage() {
        if (!isRecording) {
            startRecording();
        } else {
            stopRecordingAndSend();
        }
    }

    private void startRecording() {
        try {
            AudioFormat format = new AudioFormat(16000, 16, 1, true, true);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

            if (!AudioSystem.isLineSupported(info)) {
                showError("Matériel audio non supporté");
                return;
            }

            microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(format);
            microphone.start();

            audioOutputStream = new ByteArrayOutputStream();

            recordingThread = new Thread(() -> {
                byte[] buffer = new byte[4096];
                int bytesRead;

                while (isRecording) {
                    bytesRead = microphone.read(buffer, 0, buffer.length);
                    if (bytesRead > 0) {
                        audioOutputStream.write(buffer, 0, bytesRead);
                    }
                }
            });

            isRecording = true;
            recordingThread.start();

            btnMicro.setStyle("-fx-background-color: #ef4444; -fx-background-radius: 50;");
            showSuccess("🎤 Enregistrement en cours...");

        } catch (LineUnavailableException e) {
            showError("Erreur microphone: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void stopRecordingAndSend() {
        if (!isRecording) return;

        isRecording = false;

        if (microphone != null) {
            microphone.stop();
            microphone.close();
        }

        try {
            if (recordingThread != null) {
                recordingThread.join(1000);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        byte[] audioData = audioOutputStream.toByteArray();

        if (audioData.length == 0) {
            showError("Aucun audio enregistré");
            resetMicrophoneButton();
            return;
        }

        saveAndSendAudio(audioData);
    }

    private void saveAndSendAudio(byte[] audioData) {
        try {
            File audioDir = new File(UPLOAD_DIR + "audio/");
            if (!audioDir.exists()) {
                audioDir.mkdirs();
            }

            String fileName = "voice_" + System.currentTimeMillis() + ".wav";
            File audioFile = new File(audioDir, fileName);

            AudioFormat format = new AudioFormat(16000, 16, 1, true, true);
            ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
            AudioInputStream audioStream = new AudioInputStream(bais, format, audioData.length / format.getFrameSize());

            AudioSystem.write(audioStream, AudioFileFormat.Type.WAVE, audioFile);

            double durationSeconds = audioData.length / (16000.0 * 2);
            String duration = String.format("%d:%02d", (int)durationSeconds / 60, (int)durationSeconds % 60);

            String fileUrl = audioFile.getAbsolutePath();

            UserModel user = App.getCurrentUser();
            String expediteur = userRole + ": " + user.getFirstName();

            String sql = "INSERT INTO message (contenu, expediteur, conversation_id, date_envoie, piece_jointe_url, type_message, duree_audio) VALUES (?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, "🎤 Message vocal (" + duration + ")");
            ps.setString(2, expediteur);
            ps.setInt(3, currentConversationId);
            ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(5, fileUrl);
            ps.setString(6, "AUDIO");
            ps.setString(7, duration);

            ps.executeUpdate();

            refreshChat();
            showSuccess("Message vocal envoyé !");

        } catch (Exception e) {
            showError("Erreur sauvegarde audio: " + e.getMessage());
            e.printStackTrace();
        } finally {
            resetMicrophoneButton();
        }
    }

    private void resetMicrophoneButton() {
        btnMicro.setStyle("-fx-background-color: #6b7280; -fx-background-radius: 50;");
    }

    private String saveFile(File file, String type) {
        try {
            String fileName = UUID.randomUUID().toString() + "_" + file.getName();
            Path destination = Path.of(UPLOAD_DIR + type + "/", fileName);
            Files.copy(file.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);
            return destination.toString();
        } catch (IOException e) {
            showError("Erreur sauvegarde fichier");
            return null;
        }
    }

    private void sendMessageWithAttachment(String text, String fileUrl, String fileType) {
        UserModel user = App.getCurrentUser();
        String expediteur = userRole + ": " + user.getFirstName();

        String contenu = text;
        String typeMessage = "FICHIER";

        if (fileType.equals("image")) {
            contenu = "📷 " + text;
            typeMessage = "IMAGE";
        } else if (fileType.equals("audio")) {
            contenu = "🎤 Message vocal";
            typeMessage = "AUDIO";
        } else {
            contenu = "📎 " + text;
            typeMessage = "FICHIER";
        }

        try {
            String req = "INSERT INTO message (contenu, expediteur, conversation_id, date_envoie, piece_jointe_url, type_message) VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement ps = connection.prepareStatement(req);
            ps.setString(1, contenu);
            ps.setString(2, expediteur);
            ps.setInt(3, currentConversationId);
            ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(5, fileUrl);
            ps.setString(6, typeMessage);

            ps.executeUpdate();

            filePreviewContainer.setVisible(false);
            selectedFile = null;
            selectedImage = null;
            refreshChat();

        } catch (SQLException e) {
            showError("Erreur envoi: " + e.getMessage());
        }
    }

    // ==================== WEBSOCKET ====================

    private void initWebSocket() {
        if (currentConversationId == -1 || connectedUser == null) return;

        // Fermer ancienne connexion
        if (webSocketClient != null) {
            webSocketClient.close();
        }

        String expediteur = userRole + ": " + connectedUser.getFirstName();

        webSocketClient = new ChatWebSocketClient(
                String.valueOf(currentConversationId),
                clientId,
                freelanceId,
                expediteur,
                new ChatWebSocketClient.MessageListener() {

                    @Override
                    public void onMessage(JsonNode message) {
                        Platform.runLater(() -> {
                            try {
                                // Vérifier si c'est un message système
                                if (message.has("type") && "SYSTEM".equals(message.get("type").asText())) {
                                    // Message système - on l'ignore ou on affiche une notification
                                    System.out.println("ℹ️ " + (message.has("content") ? message.get("content").asText() : ""));
                                    return;
                                }

                                // Message normal de conversation
                                int id = message.get("id").asInt();
                                String contenu = message.get("contenu").asText();
                                String expediteur = message.get("expediteur").asText();
                                String dateStr = message.get("date").asText();

                                LocalDateTime date = LocalDateTime.parse(dateStr);

                                MessageModel newMsg = new MessageModel();
                                newMsg.setId(id);
                                newMsg.setContenu(contenu);
                                newMsg.setDateEnvoi(date);
                                newMsg.setExpediteur(expediteur);
                                newMsg.setConversationId(currentConversationId);
                                newMsg.setTypeMessage("TEXTE");

                                addWebSocketMessage(newMsg);

                            } catch (Exception e) {
                                System.err.println("❌ Erreur parsing message: " + e.getMessage());
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        Platform.runLater(() -> showError("WebSocket: " + error));
                    }

                    @Override
                    public void onClose() {
                        System.out.println("WebSocket fermé");
                    }

                    @Override
                    public void onConnectionStatus(boolean connected) {
                        Platform.runLater(() -> {
                            if (connected) {
                                showSuccess("✅ Connecté (temps réel)");
                            } else {
                                showError("⚠️ Mode dégradé (polling)");
                            }
                        });
                    }
                }
        );
    }

    private void addWebSocketMessage(MessageModel newMsg) {
        if (messagesMap.containsKey(newMsg.getId())) return;

        boolean isMe = connectedUser != null &&
                newMsg.getExpediteur() != null &&
                newMsg.getExpediteur().contains(connectedUser.getFirstName());

        HBox messageBox = createMessageBubble(
                newMsg.getExpediteur(),
                newMsg.getContenu(),
                newMsg.getDateEnvoi().format(DateTimeFormatter.ofPattern("HH:mm")),
                newMsg.getPieceJointeUrl(),
                newMsg.getTypeMessage(),
                isMe
        );

        messageDisplayList.add(messageBox);
        messageIdMap.put(messageBox, newMsg.getId());
        messagesMap.put(newMsg.getId(), newMsg);

        listMessages.scrollTo(messageDisplayList.size() - 1);
    }

    @FXML
    private void handleEnvoyer() {
        String contenu = txtContenu.getText().trim();

        if (contenu.isEmpty()) {
            showError("Message vide");
            return;
        }

        if (webSocketClient != null && webSocketClient.isConnected()) {
            // Envoi via WebSocket
            webSocketClient.sendMessage(contenu);
            txtContenu.clear();
        } else {
            // Fallback: envoi HTTP classique
            envoyerMessageHttp(contenu);
        }
    }

    private void envoyerMessageHttp(String contenu) {
        UserModel user = App.getCurrentUser();
        String expediteur = userRole + ": " + user.getFirstName();

        try {
            String req = "INSERT INTO message (contenu, expediteur, conversation_id, date_envoie, type_message) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement ps = connection.prepareStatement(req);
            ps.setString(1, contenu);
            ps.setString(2, expediteur);
            ps.setInt(3, currentConversationId);
            ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(5, "TEXTE");

            ps.executeUpdate();
            txtContenu.clear();
            refreshChat();

        } catch (SQLException e) {
            showError("Erreur envoi: " + e.getMessage());
        }
    }

    // ==================== MÉTHODES EXISTANTES ====================

    private void generateChatSummary() {
        if (currentConversationId == -1) {
            showError("Aucune conversation sélectionnée");
            return;
        }

        List<String> messageTexts = new ArrayList<>();
        for (MessageModel msg : messagesMap.values()) {
            String sender = msg.getExpediteur();
            if (sender.contains(":")) {
                sender = sender.split(":")[1].trim();
            }
            String text = msg.getContenu();
            String time = msg.getDateEnvoi() != null ?
                    msg.getDateEnvoi().format(DateTimeFormatter.ofPattern("HH:mm")) : "";

            messageTexts.add(String.format("[%s] %s: %s", time, sender, text));
        }

        if (messageTexts.isEmpty()) {
            showError("Aucun message à résumer");
            return;
        }

        Stage summaryStage = new Stage();
        summaryStage.setTitle("🤖 Résumé IA - 5ademni.tn");
        summaryStage.initModality(Modality.APPLICATION_MODAL);
        summaryStage.setMinWidth(700);
        summaryStage.setMinHeight(600);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: white; -fx-background-radius: 10;");
        root.setPadding(new Insets(25));

        VBox centerContent = new VBox(20);
        centerContent.setAlignment(Pos.TOP_CENTER);

        HBox titleBox = new HBox(15);
        titleBox.setAlignment(Pos.CENTER);

        Label iconLabel = new Label("🤖");
        iconLabel.setStyle("-fx-font-size: 48px; -fx-font-family: 'Segoe UI Emoji';");

        Label titleLabel = new Label("Génération du résumé...");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2563eb;");

        titleBox.getChildren().addAll(iconLabel, titleLabel);

        VBox progressBox = new VBox(20);
        progressBox.setAlignment(Pos.CENTER);
        progressBox.setPadding(new Insets(40, 0, 0, 0));

        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setMaxSize(80, 80);
        progressIndicator.setStyle("-fx-progress-color: #2563eb;");

        Label statusLabel = new Label("Patience, l'IA analyse la conversation...");
        statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #4b5563; -fx-font-style: italic;");

        progressBox.getChildren().addAll(progressIndicator, statusLabel);

        centerContent.getChildren().addAll(titleBox, progressBox);
        root.setCenter(centerContent);

        Scene scene = new Scene(root);
        summaryStage.setScene(scene);
        summaryStage.initOwner(btnRetour.getScene().getWindow());
        summaryStage.show();

        Task<String> summaryTask = new Task<String>() {
            @Override
            protected String call() throws Exception {
                return IASummaryService.generateSummary(messageTexts);
            }
        };

        summaryTask.setOnSucceeded(event -> {
            String summary = summaryTask.getValue();
            displaySummaryResult(centerContent, summary, summaryStage);
        });

        summaryTask.setOnFailed(event -> {
            Throwable error = summaryTask.getException();
            displaySummaryError(centerContent, error, summaryStage);
        });

        new Thread(summaryTask).start();
    }

    private void displaySummaryResult(VBox centerContent, String summary, Stage stage) {
        centerContent.getChildren().clear();

        HBox successBox = new HBox(15);
        successBox.setAlignment(Pos.CENTER);

        Label successIcon = new Label("✅");
        successIcon.setStyle("-fx-font-size: 48px;");

        Label successTitle = new Label("Résumé de la conversation");
        successTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #10b981;");

        successBox.getChildren().addAll(successIcon, successTitle);

        TextArea summaryArea = new TextArea(summary);
        summaryArea.setWrapText(true);
        summaryArea.setEditable(false);
        summaryArea.setPrefRowCount(18);
        summaryArea.setStyle("-fx-font-size: 14px; -fx-font-family: 'Segoe UI'; -fx-background-color: #f9fafb; -fx-background-radius: 8; -fx-border-color: #e5e7eb; -fx-border-radius: 8; -fx-padding: 15;");
        summaryArea.setMaxWidth(650);

        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));

        Button copyButton = new Button("📋 Copier le résumé");
        copyButton.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 12 25; -fx-cursor: hand; -fx-background-radius: 8; -fx-font-size: 14px;");
        copyButton.setOnAction(copyEvent -> {
            javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent clipboardContent = new javafx.scene.input.ClipboardContent();
            clipboardContent.putString(summary);
            clipboard.setContent(clipboardContent);
            showSuccess("Résumé copié !");
        });

        Button closeButton = new Button("Fermer");
        closeButton.setStyle("-fx-background-color: #6b7280; -fx-text-fill: white; -fx-padding: 12 25; -fx-cursor: hand; -fx-background-radius: 8; -fx-font-size: 14px;");
        closeButton.setOnAction(e -> stage.close());

        buttonBox.getChildren().addAll(copyButton, closeButton);

        centerContent.getChildren().addAll(successBox, summaryArea, buttonBox);
    }

    private void displaySummaryError(VBox centerContent, Throwable error, Stage stage) {
        centerContent.getChildren().clear();

        HBox errorBox = new HBox(15);
        errorBox.setAlignment(Pos.CENTER);

        Label errorIcon = new Label("❌");
        errorIcon.setStyle("-fx-font-size: 48px;");

        Label errorTitle = new Label("Erreur");
        errorTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #dc2626;");

        errorBox.getChildren().addAll(errorIcon, errorTitle);

        TextArea errorArea = new TextArea("Erreur: " + error.getMessage() + "\n\nUtilisation du résumé local...");
        errorArea.setWrapText(true);
        errorArea.setEditable(false);
        errorArea.setPrefRowCount(5);
        errorArea.setStyle("-fx-font-size: 14px; -fx-background-color: #fee2e2; -fx-background-radius: 8; -fx-border-color: #fecaca; -fx-border-radius: 8; -fx-padding: 15;");
        errorArea.setMaxWidth(650);

        Button closeButton = new Button("Fermer");
        closeButton.setStyle("-fx-background-color: #6b7280; -fx-text-fill: white; -fx-padding: 12 25; -fx-cursor: hand; -fx-background-radius: 8; -fx-font-size: 14px;");
        closeButton.setOnAction(e -> stage.close());

        HBox buttonBox = new HBox();
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.getChildren().add(closeButton);

        centerContent.getChildren().addAll(errorBox, errorArea, buttonBox);
    }

    private void refreshChat() {
        if (currentConversationId == -1) return;

        messageDisplayList.clear();
        messagesMap.clear();
        messageIdMap.clear();

        try {
            String req = "SELECT id, expediteur, contenu, date_envoie, piece_jointe_url, type_message, duree_audio FROM message WHERE conversation_id = ? ORDER BY date_envoie ASC";
            PreparedStatement ps = connection.prepareStatement(req);
            ps.setInt(1, currentConversationId);
            ResultSet rs = ps.executeQuery();

            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            LocalDateTime lastDate = null;

            while (rs.next()) {
                int id = rs.getInt("id");
                String expediteur = rs.getString("expediteur");
                String contenu = rs.getString("contenu");
                Timestamp dateEnvoi = rs.getTimestamp("date_envoie");
                String pieceJointe = rs.getString("piece_jointe_url");
                String typeMessage = rs.getString("type_message");
                String dureeAudio = rs.getString("duree_audio");

                LocalDateTime messageDate = dateEnvoi != null ? dateEnvoi.toLocalDateTime() : null;

                if (messageDate != null && (lastDate == null || !messageDate.toLocalDate().equals(lastDate.toLocalDate()))) {
                    HBox dateSeparator = createDateSeparator(messageDate.format(dateFormatter));
                    messageDisplayList.add(dateSeparator);
                    lastDate = messageDate;
                }

                boolean isMe = connectedUser != null &&
                        expediteur != null &&
                        expediteur.contains(connectedUser.getFirstName());

                HBox messageBox = createMessageBubble(
                        expediteur,
                        contenu,
                        messageDate != null ? messageDate.format(timeFormatter) : "",
                        pieceJointe,
                        typeMessage,
                        isMe
                );

                messageDisplayList.add(messageBox);
                messageIdMap.put(messageBox, id);

                MessageModel msg = new MessageModel();
                msg.setId(id);
                msg.setContenu(contenu);
                msg.setDateEnvoi(messageDate);
                msg.setExpediteur(expediteur);
                msg.setPieceJointeUrl(pieceJointe);
                msg.setConversationId(currentConversationId);
                msg.setTypeMessage(typeMessage);
                msg.setDureeAudio(dureeAudio);

                messagesMap.put(id, msg);
            }

            listMessages.setItems(messageDisplayList);

            if (!messageDisplayList.isEmpty()) {
                listMessages.scrollTo(messageDisplayList.size() - 1);
            }

        } catch (SQLException e) {
            showError("Erreur chargement: " + e.getMessage());
        }
    }

    private HBox createDateSeparator(String date) {
        HBox container = new HBox();
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(10, 0, 10, 0));

        Label dateLabel = new Label("📅 " + date);
        dateLabel.setStyle("-fx-background-color: #e5e7eb; -fx-padding: 5 15; -fx-background-radius: 15; -fx-font-size: 12; -fx-text-fill: #4b5563;");

        container.getChildren().add(dateLabel);
        return container;
    }

    private HBox createMessageBubble(String expediteur, String contenu, String heure, String pieceJointe, String typeMessage, boolean isMe) {
        HBox container = new HBox(10);
        container.setPadding(new Insets(5, 10, 5, 10));

        VBox messageBox = new VBox(5);

        String senderName = expediteur;
        if (expediteur != null && expediteur.contains(":")) {
            String[] parts = expediteur.split(":");
            senderName = parts.length > 1 ? parts[1].trim() : expediteur;
        }

        final String finalSenderName = senderName;

        String senderColor = isMe ? "-fx-text-fill: #e0f2fe;" : "-fx-text-fill: #4b5563;";
        Label senderLabel = new Label(senderName);
        senderLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13; " + senderColor);

        Label timeLabel = new Label(heure);
        timeLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #9ca3af;");

        if (pieceJointe != null && !pieceJointe.isEmpty()) {
            HBox attachmentBox = createAttachmentPreview(pieceJointe, typeMessage, isMe);
            messageBox.getChildren().addAll(senderLabel, attachmentBox, timeLabel);
        } else {
            Label contentLabel = new Label(contenu);
            contentLabel.setWrapText(true);
            contentLabel.setMaxWidth(400);
            contentLabel.setFont(Font.font("Segoe UI Emoji", 13));

            if (isMe) {
                contentLabel.setStyle("-fx-text-fill: white;");
            } else {
                contentLabel.setStyle("-fx-text-fill: #1f2937;");
            }

            messageBox.getChildren().addAll(senderLabel, contentLabel, timeLabel);
        }

        if (isMe) {
            messageBox.setStyle("-fx-background-color: #2563eb; -fx-background-radius: 18 18 5 18; -fx-padding: 10;");
            timeLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #bfdbfe;");
            container.setAlignment(Pos.CENTER_RIGHT);
        } else {
            messageBox.setStyle("-fx-background-color: #f3f4f6; -fx-background-radius: 18 18 18 5; -fx-padding: 10;");
            container.setAlignment(Pos.CENTER_LEFT);
        }

        container.getChildren().add(messageBox);

        ContextMenu contextMenu = new ContextMenu();

        MenuItem reply = new MenuItem("Répondre");
        MenuItem copy = new MenuItem("Copier");
        MenuItem delete = new MenuItem("Supprimer");
        MenuItem forward = new MenuItem("Transférer");

        reply.setOnAction(e -> {
            if (finalSenderName != null && !finalSenderName.isEmpty()) {
                txtContenu.setText("@" + finalSenderName + " ");
            } else {
                txtContenu.setText("@");
            }
            txtContenu.requestFocus();
            txtContenu.positionCaret(txtContenu.getText().length());
        });

        copy.setOnAction(e -> {
            javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString(contenu);
            clipboard.setContent(content);
            showSuccess("Copié !");
        });

        delete.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.initOwner(btnRetour.getScene().getWindow());
            alert.setTitle("Confirmation");
            alert.setHeaderText("Supprimer ce message ?");
            alert.setContentText("Cette action est irréversible.");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                deleteMessage(container);
            }
        });

        forward.setOnAction(e -> showForwardDialog(container));

        contextMenu.getItems().addAll(reply, copy, forward, delete);
        container.setOnContextMenuRequested(event ->
                contextMenu.show(container, event.getScreenX(), event.getScreenY())
        );

        return container;
    }

    private HBox createAttachmentPreview(String fileUrl, String typeMessage, boolean isMe) {
        HBox preview = new HBox(10);
        preview.setAlignment(Pos.CENTER_LEFT);
        preview.setPadding(new Insets(5));

        File file = new File(fileUrl);
        String fileName = file.getName();

        if ("IMAGE".equals(typeMessage)) {
            VBox imageContainer = new VBox(5);

            ImageView imageView = new ImageView();
            imageView.setFitHeight(150);
            imageView.setFitWidth(150);
            imageView.setPreserveRatio(true);
            imageView.setStyle("-fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 5, 0, 0, 2);");

            try {
                if (file.exists()) {
                    Image img = new Image(file.toURI().toString());
                    imageView.setImage(img);

                    long fileSize = file.length() / 1024;
                    String textColor = isMe ? "-fx-text-fill: #e0f2fe;" : "-fx-text-fill: #6b7280;";
                    Label sizeLabel = new Label("📷 " + fileName + " (" + fileSize + " KB)");
                    sizeLabel.setStyle("-fx-font-size: 11; " + textColor);

                    imageContainer.getChildren().addAll(imageView, sizeLabel);
                    imageView.setOnMouseClicked(e -> showFullImage(fileUrl));
                } else {
                    Label errorLabel = new Label("📷 Image non disponible");
                    errorLabel.setStyle(isMe ? "-fx-text-fill: white;" : "-fx-text-fill: #6b7280;");
                    imageContainer.getChildren().add(errorLabel);
                }
            } catch (Exception e) {
                Label errorLabel = new Label("📷 Erreur chargement");
                errorLabel.setStyle(isMe ? "-fx-text-fill: white;" : "-fx-text-fill: #6b7280;");
                imageContainer.getChildren().add(errorLabel);
            }

            preview.getChildren().add(imageContainer);

        } else if ("AUDIO".equals(typeMessage)) {
            HBox audioBox = new HBox(10);
            audioBox.setAlignment(Pos.CENTER_LEFT);

            Button playBtn = new Button("🔊 Écouter");
            playBtn.setStyle("-fx-background-color: " + (isMe ? "#3b82f6" : "#2563eb") +
                    "; -fx-text-fill: white; -fx-font-size: 12; -fx-font-weight: bold; " +
                    "-fx-background-radius: 20; -fx-padding: 8 15; -fx-cursor: hand;");

            if (file.exists()) {
                playBtn.setOnAction(e -> {
                    try {
                        java.awt.Desktop.getDesktop().open(file);
                        showSuccess("🔊 Lecture démarrée");
                    } catch (Exception ex) {
                        showError("Erreur lecture: " + ex.getMessage());

                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.initOwner(playBtn.getScene().getWindow());
                        alert.setTitle("Fichier audio");
                        alert.setHeaderText("Chemin du fichier:");
                        alert.setContentText(file.getAbsolutePath());
                        alert.showAndWait();
                    }
                });
            } else {
                playBtn.setDisable(true);
                playBtn.setText("❌ Introuvable");
                playBtn.setStyle("-fx-background-color: #9ca3af; -fx-text-fill: white;");
            }

            String textColor = isMe ? "-fx-text-fill: white;" : "-fx-text-fill: #1f2937;";
            String secondaryColor = isMe ? "-fx-text-fill: #e0f2fe;" : "-fx-text-fill: #6b7280;";

            VBox infoBox = new VBox(2);

            Label audioLabel = new Label("🎤 Message vocal");
            audioLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13; " + textColor);

            long fileSize = file.exists() ? file.length() / 1024 : 0;
            Label sizeLabel = new Label(fileSize + " KB");
            sizeLabel.setStyle("-fx-font-size: 11; " + secondaryColor);

            infoBox.getChildren().addAll(audioLabel, sizeLabel);

            audioBox.getChildren().addAll(playBtn, infoBox);
            preview.getChildren().add(audioBox);

        } else {
            HBox fileBox = new HBox(10);
            fileBox.setAlignment(Pos.CENTER_LEFT);

            String linkColor = isMe ? "-fx-text-fill: white; -fx-underline: true;"
                    : "-fx-text-fill: #2563eb; -fx-underline: true;";
            String secondaryColor = isMe ? "-fx-text-fill: #e0f2fe;" : "-fx-text-fill: #6b7280;";

            Label fileLabel = new Label("📎 " + fileName);
            fileLabel.setStyle("-fx-cursor: hand; -fx-font-weight: bold; " + linkColor);

            if (file.exists()) {
                fileLabel.setOnMouseClicked(e -> openFile(fileUrl));

                long fileSize = file.length() / 1024;
                Label sizeLabel = new Label("(" + fileSize + " KB)");
                sizeLabel.setStyle("-fx-font-size: 11; " + secondaryColor);

                fileBox.getChildren().addAll(fileLabel, sizeLabel);
            } else {
                fileLabel.setStyle("-fx-text-fill: #9ca3af; -fx-underline: false;");
                fileLabel.setText("📎 " + fileName + " (introuvable)");
                fileBox.getChildren().add(fileLabel);
            }

            preview.getChildren().add(fileBox);
        }

        return preview;
    }

    private void showFullImage(String imageUrl) {
        try {
            Stage stage = new Stage();
            stage.setTitle("Aperçu image");

            ImageView imageView = new ImageView();
            Image image = new Image(new File(imageUrl).toURI().toString());
            imageView.setImage(image);
            imageView.setPreserveRatio(true);
            imageView.setFitWidth(600);
            imageView.setFitHeight(400);

            ScrollPane scrollPane = new ScrollPane(imageView);
            scrollPane.setFitToWidth(true);
            scrollPane.setFitToHeight(true);

            Scene scene = new Scene(scrollPane, 600, 400);
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            showError("Erreur affichage image");
        }
    }

    private void playAudio(String fileUrl) {
        try {
            File file = new File(fileUrl);
            if (!file.exists()) {
                showError("Fichier audio introuvable");
                return;
            }

            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
            } else {
                showError("Ouverture de fichiers non supportée");
            }
        } catch (Exception e) {
            showError("Erreur lecture audio: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void openFile(String fileUrl) {
        try {
            File file = new File(fileUrl);
            if (file.exists()) {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(file);
                } else {
                    showError("Ouverture de fichiers non supportée");
                }
            }
        } catch (Exception e) {
            showError("Erreur ouverture fichier");
        }
    }

    private void deleteMessage(HBox messageBox) {
        Integer messageId = messageIdMap.get(messageBox);
        if (messageId != null) {
            try {
                String req = "DELETE FROM message WHERE id = ?";
                PreparedStatement ps = connection.prepareStatement(req);
                ps.setInt(1, messageId);
                ps.executeUpdate();
                refreshChat();
                showSuccess("Message supprimé");
            } catch (SQLException e) {
                showError("Erreur suppression");
            }
        }
    }

    private void showForwardDialog(HBox messageBox) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Transférer");
        dialog.setHeaderText("ID de la conversation destination:");
        dialog.setContentText("Conversation ID:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                int destConvId = Integer.parseInt(result.get());
                Integer messageId = messageIdMap.get(messageBox);
                MessageModel original = messagesMap.get(messageId);

                String req = "INSERT INTO message (contenu, expediteur, conversation_id, date_envoie, piece_jointe_url, type_message) VALUES (?, ?, ?, ?, ?, ?)";
                PreparedStatement ps = connection.prepareStatement(req);
                ps.setString(1, "📨 Transféré: " + original.getContenu());
                ps.setString(2, userRole + ": " + connectedUser.getFirstName());
                ps.setInt(3, destConvId);
                ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
                ps.setString(5, original.getPieceJointeUrl());
                ps.setString(6, original.getTypeMessage());

                ps.executeUpdate();
                showSuccess("Message transféré !");

            } catch (NumberFormatException e) {
                showError("ID invalide");
            } catch (Exception e) {
                showError("Erreur transfert: " + e.getMessage());
            }
        }
    }
    @FXML
    private void scheduleMeeting() {
        if (currentConversationId != -1) {
            TextInputDialog dialog = new TextInputDialog("30");
            dialog.setTitle("Planifier une réunion");
            dialog.setHeaderText("Planifier une réunion avec " + getOtherParticipantName());
            dialog.setContentText("Durée (minutes):");

            Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                try {
                    int minutes = Integer.parseInt(result.get());
                    LocalDateTime meetingTime = LocalDateTime.now().plusMinutes(minutes);
                    showSuccess("Réunion planifiée pour " + meetingTime.format(DateTimeFormatter.ofPattern("HH:mm")));
                } catch (NumberFormatException e) {
                    showError("Durée invalide");
                }
            }
        }
    }

    @FXML
    private void showConversationInfo() {
        if (currentConversationId != -1) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.initOwner(btnInfo.getScene().getWindow());
            alert.setTitle("Informations conversation");
            alert.setHeaderText("Conversation #" + currentConversationId);

            String content = String.format(
                    "Client: ID %d\nFreelance: ID %d\nParticipant: %s\nMessages: %d\nDate: %s",
                    clientId, freelanceId, getOtherParticipantName(),
                    messagesMap.size(), LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
            );

            alert.setContentText(content);
            alert.showAndWait();
        }
    }

    private String getOtherParticipantName() {
        if (connectedUser != null) {
            if (connectedUser.getId() == clientId) {
                return "Freelance (ID: " + freelanceId + ")";
            } else {
                return "Client (ID: " + clientId + ")";
            }
        }
        return "Participant";
    }

    private void disableButtons(boolean disable) {
        btnVideoCall.setDisable(disable);
        btnAudioCall.setDisable(disable);
        btnReunion.setDisable(disable);
        btnInfo.setDisable(disable);
        btnEnvoyer.setDisable(disable);
        txtContenu.setDisable(disable);
    }

    public void setConversationId(int id) {
        this.currentConversationId = id;
        disableButtons(false);
        refreshChat();
        initWebSocket(); // Initialiser WebSocket
    }

    public void setConversationInfo(int clientId, int freelanceId) {
        this.clientId = clientId;
        this.freelanceId = freelanceId;

        if (connectedUser != null) {
            currentUser = connectedUser.getFirstName();

            if (connectedUser.getId() == clientId) {
                userRole = "CLIENT";
            } else if (connectedUser.getId() == freelanceId) {
                userRole = "FREELANCE";
            }
        }

        updateConversationInfo();
    }

    private void updateConversationInfo() {
        if (lblConversationInfo != null) {
            String role = (userRole != null) ? " (" + userRole + ")" : "";
            lblConversationInfo.setText("Conversation #" + currentConversationId +
                    " | Client: " + clientId +
                    " | Freelance: " + freelanceId +
                    " | Vous: " + (currentUser != null ? currentUser + role : "Non connecté"));
        }
    }

    @FXML
    private void handleRetour() {
        if (webSocketClient != null) {
            webSocketClient.close();
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/khademni/Conversation.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) btnRetour.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Mes Conversations");

        } catch (IOException e) {
            showError("Erreur retour");
        }
    }

    private void showError(String message) {
        errorLabel.setText("❌ " + message);
        errorLabel.setStyle("-fx-text-fill: #dc2626;");
        errorLabel.setVisible(true);

        new Thread(() -> {
            try { Thread.sleep(3000); } catch (InterruptedException e) {}
            javafx.application.Platform.runLater(() -> errorLabel.setVisible(false));
        }).start();
    }

    private void showSuccess(String message) {
        errorLabel.setText("✅ " + message);
        errorLabel.setStyle("-fx-text-fill: #10b981;");
        errorLabel.setVisible(true);

        new Thread(() -> {
            try { Thread.sleep(2000); } catch (InterruptedException e) {}
            javafx.application.Platform.runLater(() -> errorLabel.setVisible(false));
        }).start();
    }
}