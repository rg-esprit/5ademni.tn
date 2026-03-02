package com.khademni.controller;

import com.khademni.App;
import com.khademni.model.MessageModel;
import com.khademni.model.UserModel;
import com.khademni.service.CallClient;
import com.khademni.service.CallWindow;
import com.khademni.service.GoogleCalendarService;
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
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MessageController {

    @FXML private VBox mainContainer;
    @FXML private Label lblConversationInfo;
    @FXML private Label lblConversationTitle;
    @FXML private Button btnRetour;
    @FXML private Button btnVideoCall;
    @FXML private Button btnAudioCall;
    @FXML private Button btnInfo;
    @FXML private Button btnReunion;
    @FXML private ListView<HBox> listMessages;
    @FXML private TextArea txtContenu;
    @FXML private Button btnEnvoyer;
    @FXML private Button btnEmoji;
    @FXML private Button btnPieceJointe;
    @FXML private Button btnPhoto;
    @FXML private Button btnMicro;
    @FXML private Button btnSummary;
    @FXML private Label errorLabel;
    @FXML private HBox typingIndicator;
    @FXML private Label lblTyping;
    @FXML private ScrollPane emojiPicker;
    @FXML private VBox emojiContainer;
    @FXML private VBox filePreviewContainer;
    @FXML private ImageView filePreviewImage;
    @FXML private Label filePreviewName;
    @FXML private Button filePreviewCancel;
    @FXML private ProgressBar uploadProgress;
    @FXML private HBox contractBadge;
    @FXML private Label lblContractStatus;
    @FXML private Button btnCreateContract;
    @FXML private Button btnProcessPayment;

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
    private boolean isRinging = false;
    private AtomicBoolean callTimeoutActive = new AtomicBoolean(false);
    private Thread callTimeoutThread;

    // Client d'appel
    private CallClient callClient;

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

            // Créer la table missed_calls si elle n'existe pas
            createMissedCallsTable();

            // Créer la table voice_messages si elle n'existe pas
            createVoiceMessagesTable();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void createMissedCallsTable() {
        try {
            String sql = "CREATE TABLE IF NOT EXISTS missed_calls (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT, " +
                    "caller_id INT NOT NULL, " +
                    "user_id INT NOT NULL, " +
                    "call_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "seen BOOLEAN DEFAULT FALSE, " +
                    "FOREIGN KEY (caller_id) REFERENCES users(id), " +
                    "FOREIGN KEY (user_id) REFERENCES users(id))";
            Statement stmt = connection.createStatement();
            stmt.execute(sql);
            System.out.println("✅ Table missed_calls vérifiée/créée");
        } catch (SQLException e) {
            System.err.println("❌ Erreur création table missed_calls: " + e.getMessage());
        }
    }

    private void createVoiceMessagesTable() {
        try {
            String sql = "CREATE TABLE IF NOT EXISTS voice_messages (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT, " +
                    "sender_id INT NOT NULL, " +
                    "receiver_id INT NOT NULL, " +
                    "audio_data LONGTEXT, " +
                    "duration VARCHAR(10), " +
                    "conversation_id INT, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "played BOOLEAN DEFAULT FALSE, " +
                    "FOREIGN KEY (sender_id) REFERENCES users(id), " +
                    "FOREIGN KEY (receiver_id) REFERENCES users(id), " +
                    "FOREIGN KEY (conversation_id) REFERENCES conversation(id)" +
                    ")";
            Statement stmt = connection.createStatement();
            stmt.execute(sql);
            System.out.println("✅ Table voice_messages vérifiée/créée");
        } catch (SQLException e) {
            System.err.println("❌ Erreur création table voice_messages: " + e.getMessage());
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

        // Initialisation des boutons
        if (btnVideoCall != null) btnVideoCall.setOnAction(e -> startVideoCall());
        if (btnAudioCall != null) btnAudioCall.setOnAction(e -> startAudioCall());
        if (btnInfo != null) btnInfo.setOnAction(e -> showConversationInfo());
        if (btnReunion != null) btnReunion.setOnAction(e -> scheduleMeeting());
        if (btnEmoji != null) btnEmoji.setOnAction(e -> handleEmojiPicker());
        if (btnPieceJointe != null) btnPieceJointe.setOnAction(e -> handleFileAttachment());
        if (btnPhoto != null) btnPhoto.setOnAction(e -> handlePhotoAttachment());
        if (btnMicro != null) btnMicro.setOnAction(e -> handleVoiceMessage());
        if (btnSummary != null) btnSummary.setOnAction(e -> generateChatSummary());
        if (btnCreateContract != null) btnCreateContract.setOnAction(e -> showInfo("Création de contrat"));
        if (btnProcessPayment != null) btnProcessPayment.setOnAction(e -> showInfo("Paiement"));

        // Initialiser le client d'appel
        initCallClient();

        disableButtons(true);

        System.out.println("✅ MessageController initialisé");
    }

    private void showInfo(String message) {
        showSuccess(message);
    }

    // ==================== INITIALISATION CLIENT D'APPEL ====================

    private void initCallClient() {
        if (connectedUser == null) return;

        callClient = new CallClient(String.valueOf(connectedUser.getId()), new CallClient.CallListener() {
            @Override
            public void onIncomingCall(String callerId) {
                Platform.runLater(() -> {
                    String callerName = getUserNameById(Integer.parseInt(callerId));
                    showIncomingCallNotification(callerName, callerId);
                });
            }

            @Override
            public void onCallAccepted() {
                Platform.runLater(() -> {
                    showSuccess("✅ Appel accepté");
                    isInCall = true;
                    cancelCallTimeout();
                    addCallMessageToChat("Appel accepté", "CALL_ACCEPTED");
                });
            }

            @Override
            public void onCallRejected() {
                Platform.runLater(() -> {
                    showError("❌ Appel refusé");
                    isInCall = false;
                    cancelCallTimeout();
                });
            }

            @Override
            public void onCallEnded() {
                Platform.runLater(() -> {
                    showError("🔴 Appel terminé");
                    isInCall = false;
                    cancelCallTimeout();
                    addCallMessageToChat("Appel terminé", "CALL_ENDED");
                });
            }

            @Override
            public void onRemoteVideoFrame(String frameData) {
                // Pour la vidéo (optionnel)
            }

            @Override
            public void onConnectionError(String error) {
                Platform.runLater(() -> {
                    showError("📞 Erreur appel: " + error);
                    isInCall = false;
                    cancelCallTimeout();
                });
            }

            @Override
            public void onMissedCall(String callerId, String timestamp) {
                Platform.runLater(() -> {
                    String callerName = getUserNameById(Integer.parseInt(callerId));
                    showError("📞 Appel manqué de " + callerName + " à " + timestamp);
                    addCallMessageToChat("Appel manqué de " + callerName + " à " + timestamp, "MISSED_CALL");
                    cancelCallTimeout();
                });
            }

            @Override
            public void onMissedCallNotification(String callerId, String timestamp) {
                Platform.runLater(() -> {
                    String callerName = getUserNameById(Integer.parseInt(callerId));
                    showMissedCallNotification(callerName, timestamp);
                });
            }

            @Override
            public void onUserUnavailable(String message, String timestamp) {
                Platform.runLater(() -> {
                    showError("📞 " + message + " (" + timestamp + ")");
                    cancelCallTimeout();
                    isInCall = false;
                });
            }

            @Override
            public void onUserOffline(String message, String timestamp) {
                Platform.runLater(() -> {
                    showError("📞 " + message + " - utilisateur hors ligne (" + timestamp + ")");
                    cancelCallTimeout();
                    isInCall = false;

                    // Proposer de laisser un message vocal
                    showVoiceMessageOption();
                });
            }

            @Override
            public void onUserOnline(String userId) {
                Platform.runLater(() -> {
                    System.out.println("✅ Utilisateur " + userId + " est en ligne");
                });
            }

            @Override
            public void onVoiceMessageReceived(String fromUserId, String audioData, String duration) {
                Platform.runLater(() -> {
                    String callerName = getUserNameById(Integer.parseInt(fromUserId));
                    showSuccess("🎤 Message vocal reçu de " + callerName + " (durée: " + duration + ")");
                    addCallMessageToChat("Message vocal reçu de " + callerName, "VOICE_MESSAGE");

                    // Sauvegarder le message vocal dans la base de données
                    saveVoiceMessageToDatabase(Integer.parseInt(fromUserId), audioData, duration);
                });
            }
        });
    }

    private void showVoiceMessageOption() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Utilisateur hors ligne");
        alert.setHeaderText("👤 L'utilisateur n'est pas en ligne");
        alert.setContentText("Voulez-vous laisser un message vocal ?");

        ButtonType voiceMessageButton = new ButtonType("🎤 Laisser un message vocal", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(voiceMessageButton, cancelButton);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == voiceMessageButton) {
            startVoiceRecordingForOfflineUser();
        }
    }

    private void startVoiceRecordingForOfflineUser() {
        // Utiliser la même méthode que pour l'enregistrement vocal normal
        if (!isRecording) {
            startRecording();

            // Modifier le comportement pour l'envoyer comme message vocal hors ligne
            new Thread(() -> {
                try {
                    Thread.sleep(30000); // 30 secondes max
                    if (isRecording) {
                        Platform.runLater(() -> {
                            stopRecordingAndSendAsOfflineMessage();
                        });
                    }
                } catch (InterruptedException e) {}
            }).start();
        }
    }

    private void stopRecordingAndSendAsOfflineMessage() {
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

        // Envoyer comme message vocal hors ligne
        double durationSeconds = audioData.length / (16000.0 * 2);
        String duration = String.format("%d:%02d", (int)durationSeconds / 60, (int)durationSeconds % 60);

        String targetId = String.valueOf(connectedUser.getId() == clientId ? freelanceId : clientId);

        if (callClient != null) {
            callClient.sendVoiceMessage(targetId, audioData, duration);
            showSuccess("✅ Message vocal envoyé (sera délivré quand l'utilisateur sera en ligne)");
        }

        resetMicrophoneButton();
    }

    private String getUserNameById(int userId) {
        try {
            String sql = "SELECT first_name, last_name FROM users WHERE id = ?";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("first_name") + " " + rs.getString("last_name");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "Utilisateur " + userId;
    }

    private void addCallMessageToChat(String message, String type) {
        if (currentConversationId == -1) return;

        String contenu = "🔔 " + message;

        try {
            String sql = "INSERT INTO message (contenu, expediteur, conversation_id, date_envoie, type_message) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, contenu);
            ps.setString(2, "SYSTEM");
            ps.setInt(3, currentConversationId);
            ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(5, "SYSTEM");
            ps.executeUpdate();
            refreshChat();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ==================== GESTION DU TIMEOUT D'APPEL ====================

    private void startCallTimeout() {
        cancelCallTimeout();

        callTimeoutActive.set(true);
        callTimeoutThread = new Thread(() -> {
            try {
                Thread.sleep(30000);
                if (callTimeoutActive.get() && !isInCall) {
                    Platform.runLater(() -> {
                        showError("⏰ Personne ne répond");
                        if (currentConversationId != -1) {
                            addCallMessageToChat("Appel sans réponse", "MISSED_CALL");
                        }

                        String targetId = String.valueOf(connectedUser.getId() == clientId ? freelanceId : clientId);

                        saveMissedCallToDatabase(Integer.parseInt(targetId));

                        if (callClient != null) {
                            callClient.reportMissedCall(targetId);
                        }

                        isInCall = false;
                    });
                }
            } catch (InterruptedException e) {
                // Timeout annulé
            }
        });
        callTimeoutThread.setDaemon(true);
        callTimeoutThread.start();
    }

    private void cancelCallTimeout() {
        callTimeoutActive.set(false);
        if (callTimeoutThread != null) {
            callTimeoutThread.interrupt();
            callTimeoutThread = null;
        }
    }

    // ==================== GESTION DES APPELS MANQUÉS ====================

    private void saveMissedCallToDatabase(int callerId) {
        try {
            String sql = "INSERT INTO missed_calls (caller_id, user_id, call_time, seen) VALUES (?, ?, ?, 0)";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, callerId);
            ps.setInt(2, connectedUser.getId());
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
            System.out.println("✅ Appel manqué sauvegardé dans la base de données");
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la sauvegarde de l'appel manqué: " + e.getMessage());
        }
    }

    private void saveVoiceMessageToDatabase(int senderId, String audioData, String duration) {
        try {
            String sql = "INSERT INTO voice_messages (sender_id, receiver_id, audio_data, duration, conversation_id) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, senderId);
            ps.setInt(2, connectedUser.getId());
            ps.setString(3, audioData);
            ps.setString(4, duration);
            ps.setInt(5, currentConversationId);
            ps.executeUpdate();

            System.out.println("✅ Message vocal sauvegardé dans la base de données");

        } catch (SQLException e) {
            System.err.println("❌ Erreur sauvegarde message vocal: " + e.getMessage());
        }
    }

    private void checkMissedCalls() {
        if (connectedUser == null) return;

        try {
            String sql = "SELECT mc.*, u.first_name, u.last_name FROM missed_calls mc " +
                    "JOIN users u ON mc.caller_id = u.id " +
                    "WHERE mc.user_id = ? AND mc.seen = 0 ORDER BY mc.call_time DESC";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, connectedUser.getId());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                int callerId = rs.getInt("caller_id");
                String callerName = rs.getString("first_name") + " " + rs.getString("last_name");
                String timestamp = rs.getTimestamp("call_time").toString();

                showMissedCallNotification(callerName, timestamp);

                String updateSql = "UPDATE missed_calls SET seen = 1 WHERE id = ?";
                PreparedStatement updatePs = connection.prepareStatement(updateSql);
                updatePs.setInt(1, rs.getInt("id"));
                updatePs.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la vérification des appels manqués: " + e.getMessage());
        }

        // Vérifier aussi les messages vocaux
        checkVoiceMessages();
    }

    private void checkVoiceMessages() {
        if (connectedUser == null) return;

        try {
            String sql = "SELECT vm.*, u.first_name, u.last_name FROM voice_messages vm " +
                    "JOIN users u ON vm.sender_id = u.id " +
                    "WHERE vm.receiver_id = ? AND vm.played = FALSE ORDER BY vm.created_at DESC";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, connectedUser.getId());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                int senderId = rs.getInt("sender_id");
                String senderName = rs.getString("first_name") + " " + rs.getString("last_name");
                String duration = rs.getString("duration");
                String createdAt = rs.getTimestamp("created_at").toString();

                showVoiceMessageNotification(senderName, duration, createdAt, rs.getInt("id"));
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur vérification messages vocaux: " + e.getMessage());
        }
    }

    private void showVoiceMessageNotification(String senderName, String duration, String timestamp, int messageId) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Message vocal");
            alert.setHeaderText("🎤 Message vocal de " + senderName);
            alert.setContentText("Durée: " + duration + "\nDate: " + timestamp);

            ButtonType ecouterButton = new ButtonType("🔊 Écouter", ButtonBar.ButtonData.OK_DONE);
            ButtonType fermerButton = new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE);

            alert.getButtonTypes().setAll(ecouterButton, fermerButton);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ecouterButton) {
                // Marquer comme écouté
                try {
                    String updateSql = "UPDATE voice_messages SET played = TRUE WHERE id = ?";
                    PreparedStatement updatePs = connection.prepareStatement(updateSql);
                    updatePs.setInt(1, messageId);
                    updatePs.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                showSuccess("🔊 Lecture du message vocal...");
            }
        });
    }

    private void showMissedCallNotification(String callerName, String timestamp) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Appel manqué");
            alert.setHeaderText("📞 Appel manqué de " + callerName);
            alert.setContentText("Date: " + timestamp);

            ButtonType rappelerButton = new ButtonType("Rappeler", ButtonBar.ButtonData.OK_DONE);
            ButtonType fermerButton = new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE);

            alert.getButtonTypes().setAll(rappelerButton, fermerButton);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == rappelerButton) {
                startAudioCall();
            }
        });
    }

    // ==================== NOTIFICATION D'APPEL ENTRANT ====================

    private void showIncomingCallNotification(String callerName, String callerId) {
        playRingtone();

        Stage notificationStage = new Stage();
        notificationStage.initStyle(StageStyle.UNDECORATED);
        notificationStage.setTitle("Appel entrant");

        notificationStage.setX(javafx.stage.Screen.getPrimary().getVisualBounds().getMaxX() - 320);
        notificationStage.setY(20);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 2);");
        root.setPadding(new Insets(20));
        root.setPrefWidth(300);

        VBox content = new VBox(15);
        content.setAlignment(Pos.CENTER);

        Label iconLabel = new Label("📞");
        iconLabel.setStyle("-fx-font-size: 40px;");

        Label titleLabel = new Label("Appel entrant");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1f2937;");

        Label nameLabel = new Label(callerName);
        nameLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2563eb;");

        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);

        Button acceptBtn = new Button("✅ Accepter");
        acceptBtn.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-cursor: hand; -fx-background-radius: 5;");
        acceptBtn.setOnAction(e -> {
            stopRingtone();
            notificationStage.close();
            if (callClient != null) {
                callClient.acceptCall(callerId);
                CallWindow callWindow = new CallWindow(callerName, false, callClient);
                callWindow.show();
                cancelCallTimeout();
            }
        });

        Button rejectBtn = new Button("❌ Refuser");
        rejectBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-cursor: hand; -fx-background-radius: 5;");
        rejectBtn.setOnAction(e -> {
            stopRingtone();
            notificationStage.close();
            if (callClient != null) {
                callClient.rejectCall(callerId);
            }
        });

        buttonBox.getChildren().addAll(acceptBtn, rejectBtn);

        content.getChildren().addAll(iconLabel, titleLabel, nameLabel, buttonBox);
        root.setCenter(content);

        Scene scene = new Scene(root);
        notificationStage.setScene(scene);
        notificationStage.show();

        final String finalCallerId = callerId;
        new Thread(() -> {
            try {
                Thread.sleep(30000);
                Platform.runLater(() -> {
                    if (notificationStage.isShowing()) {
                        notificationStage.close();
                        stopRingtone();
                        showError("Appel non répondus");

                        saveMissedCallToDatabase(Integer.parseInt(finalCallerId));

                        if (callClient != null) {
                            callClient.reportMissedCall(finalCallerId);
                        }

                        if (currentConversationId != -1) {
                            String callerName2 = getUserNameById(Integer.parseInt(finalCallerId));
                            addCallMessageToChat("Appel manqué de " + callerName2, "MISSED_CALL");
                        }
                    }
                });
            } catch (InterruptedException ex) {}
        }).start();
    }

    // ==================== MÉTHODES D'APPEL ====================

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

        if (callClient == null) {
            showError("Client d'appel non initialisé");
            return;
        }

        String targetId = String.valueOf(connectedUser.getId() == clientId ? freelanceId : clientId);
        String targetName = getOtherParticipantName();

        callClient.startCall(targetId);
        showInfo("Appel vidéo en cours vers " + targetName + "...");
        addCallMessageToChat("Appel vidéo initié", "CALL_STARTED");

        isInCall = true;
        startCallTimeout();
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

        if (callClient == null) {
            showError("Client d'appel non initialisé");
            return;
        }

        String targetId = String.valueOf(connectedUser.getId() == clientId ? freelanceId : clientId);
        String targetName = getOtherParticipantName();

        callClient.startCall(targetId);
        showInfo("Appel en cours vers " + targetName + "...");
        addCallMessageToChat("Appel audio initié", "CALL_STARTED");

        isInCall = true;
        startCallTimeout();
    }

    private void playRingtone() {
        try {
            String ringtonePath = getClass().getResource("/sounds/ringtone.mp3").toExternalForm();
            System.out.println("🔍 Chemin sonnerie: " + ringtonePath);

            if (ringtonePath != null && !ringtonePath.isEmpty() && !ringtonePath.equals("null")) {
                Media ringtone = new Media(ringtonePath);
                ringtonePlayer = new MediaPlayer(ringtone);
                ringtonePlayer.setCycleCount(MediaPlayer.INDEFINITE);
                ringtonePlayer.setVolume(0.8);
                ringtonePlayer.play();
                System.out.println("🔊 Sonnerie jouée");
            } else {
                System.err.println("❌ Fichier sonnerie non trouvé");
                isRinging = true;
                new Thread(() -> {
                    while (isRinging) {
                        Toolkit.getDefaultToolkit().beep();
                        try { Thread.sleep(2000); } catch (InterruptedException ex) {}
                    }
                }).start();
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur sonnerie: " + e.getMessage());
            isRinging = true;
            new Thread(() -> {
                while (isRinging) {
                    Toolkit.getDefaultToolkit().beep();
                    try { Thread.sleep(2000); } catch (InterruptedException ex) {}
                }
            }).start();
        }
    }

    private void stopRingtone() {
        isRinging = false;
        if (ringtonePlayer != null) {
            ringtonePlayer.stop();
        }
    }

    // ==================== PLANIFICATION RÉUNION ====================

    @FXML
    private void scheduleMeeting() {
        if (currentConversationId == -1) return;

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("📅 Planifier une réunion");
        dialog.setHeaderText("Planifier une réunion avec " + getOtherParticipantName());

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setPrefWidth(400);

        Label titleLabel = new Label("Titre de la réunion :");
        titleLabel.setStyle("-fx-font-weight: bold;");
        TextField titleField = new TextField("Réunion " + getOtherParticipantName());

        Label dateLabel = new Label("Date :");
        dateLabel.setStyle("-fx-font-weight: bold;");
        DatePicker datePicker = new DatePicker(LocalDate.now());
        datePicker.setPrefWidth(300);

        Label timeLabel = new Label("Heure :");
        timeLabel.setStyle("-fx-font-weight: bold;");

        HBox timeBox = new HBox(10);
        Spinner<Integer> hourSpinner = new Spinner<>(0, 23, 14);
        hourSpinner.setPrefWidth(80);
        hourSpinner.setEditable(true);

        Spinner<Integer> minuteSpinner = new Spinner<>(0, 59, 0);
        minuteSpinner.setPrefWidth(80);
        minuteSpinner.setEditable(true);

        Label separator = new Label(":");
        separator.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");

        timeBox.getChildren().addAll(hourSpinner, separator, minuteSpinner);

        Label durationLabel = new Label("Durée (minutes) :");
        durationLabel.setStyle("-fx-font-weight: bold;");
        Spinner<Integer> durationSpinner = new Spinner<>(5, 180, 30, 5);
        durationSpinner.setPrefWidth(120);
        durationSpinner.setEditable(true);

        Label descLabel = new Label("Description :");
        descLabel.setStyle("-fx-font-weight: bold;");
        TextArea descArea = new TextArea();
        descArea.setPromptText("Objet de la réunion...");
        descArea.setPrefRowCount(3);

        content.getChildren().addAll(
                titleLabel, titleField,
                dateLabel, datePicker,
                timeLabel, timeBox,
                durationLabel, durationSpinner,
                descLabel, descArea
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                String titreSaisi = titleField.getText().trim();
                LocalDate date = datePicker.getValue();
                int hour = hourSpinner.getValue();
                int minute = minuteSpinner.getValue();
                int duration = durationSpinner.getValue();
                String description = descArea.getText().trim();

                final String titre = titreSaisi.isEmpty() ? "Réunion" : titreSaisi;
                final LocalDateTime meetingTime = LocalDateTime.of(date, LocalTime.of(hour, minute));
                final int finalDuration = duration;
                final String finalDescription = description;

                new Thread(() -> {
                    try {
                        GoogleCalendarService calendarService = new GoogleCalendarService();
                        String eventId = calendarService.createMeetingEvent(
                                titre,
                                finalDescription,
                                meetingTime,
                                finalDuration
                        );

                        Platform.runLater(() -> {
                            String message = String.format(
                                    "✅ Réunion planifiée dans votre calendrier !\n📅 %s à %02d:%02d\n⏱️ %d minutes",
                                    date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                                    hour, minute, finalDuration
                            );

                            envoyerMessageSysteme("🔔 Réunion planifiée: " + titre + " le " +
                                    date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " à " +
                                    String.format("%02d:%02d", hour, minute));

                            showSuccess(message);
                        });

                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            showError("Erreur Google Calendar: " + e.getMessage());
                            e.printStackTrace();
                        });
                    }
                }).start();
            }
        });
    }

    private void envoyerMessageSysteme(String contenu) {
        try {
            String sql = "INSERT INTO message (contenu, expediteur, conversation_id, date_envoie, type_message) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, contenu);
            ps.setString(2, "SYSTEM");
            ps.setInt(3, currentConversationId);
            ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(5, "SYSTEM");
            ps.executeUpdate();
            refreshChat();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ==================== MÉTHODES EXISTANTES ====================

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
        if (emojiPicker != null) {
            emojiPicker.setVisible(!emojiPicker.isVisible());
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

        System.out.println("🔄 Initialisation WebSocket - Conv: " + currentConversationId +
                ", Client: " + clientId + ", Freelance: " + freelanceId);

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
                                if (message.has("type") && "SYSTEM".equals(message.get("type").asText())) {
                                    System.out.println("ℹ️ Message système: " +
                                            (message.has("content") ? message.get("content").asText() : ""));
                                    return;
                                }

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
                                checkMissedCalls();
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
            webSocketClient.sendMessage(contenu);
            txtContenu.clear();
        } else {
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

    // ==================== RÉSUMÉ IA ====================

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
    private void showConversationInfo() {
        if (currentConversationId != -1) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Informations conversation");
            alert.setHeaderText("À propos de cette conversation");

            String clientName = getClientName();
            String freelanceName = getFreelanceName();
            String titre = getConversationTitle();

            String content = String.format(
                    "Titre: %s\nClient: %s\nFreelance: %s\nMessages: %d\nDate: %s",
                    (titre != null ? titre : "Conversation simple"),
                    clientName,
                    freelanceName,
                    messagesMap.size(),
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
            );

            alert.setContentText(content);
            alert.showAndWait();
        }
    }

    private String getOtherParticipantName() {
        if (connectedUser != null) {
            if (connectedUser.getId() == clientId) {
                return getFreelanceName();
            } else {
                return getClientName();
            }
        }
        return "Participant";
    }

    private String getClientName() {
        try {
            String sql = "SELECT first_name, last_name FROM users WHERE id = ?";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, clientId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("first_name") + " " + rs.getString("last_name");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "Client";
    }

    private String getFreelanceName() {
        try {
            String sql = "SELECT first_name, last_name FROM users WHERE id = ?";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, freelanceId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("first_name") + " " + rs.getString("last_name");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "Freelance";
    }

    private void disableButtons(boolean disable) {
        if (btnVideoCall != null) btnVideoCall.setDisable(disable);
        if (btnAudioCall != null) btnAudioCall.setDisable(disable);
        if (btnReunion != null) btnReunion.setDisable(disable);
        if (btnInfo != null) btnInfo.setDisable(disable);
        if (btnEnvoyer != null) btnEnvoyer.setDisable(disable);
        if (txtContenu != null) txtContenu.setDisable(disable);
    }

    public void setConversationId(int id) {
        this.currentConversationId = id;
        disableButtons(false);
        refreshChat();
        initWebSocket();
    }

    public void setConversationInfo(int clientId, int freelanceId) {
        this.clientId = clientId;
        this.freelanceId = freelanceId;

        System.out.println("📌 Conversation Info reçue - Client ID: " + clientId + ", Freelance ID: " + freelanceId);

        if (connectedUser != null) {
            currentUser = connectedUser.getFirstName();

            if (connectedUser.getId() == clientId) {
                userRole = "CLIENT";
                System.out.println("✅ Rôle défini: CLIENT pour " + currentUser);
            } else if (connectedUser.getId() == freelanceId) {
                userRole = "FREELANCE";
                System.out.println("✅ Rôle défini: FREELANCE pour " + currentUser);
            } else {
                userRole = "OBSERVATEUR";
                System.out.println("⚠️ Rôle: OBSERVATEUR");
            }
        }

        updateConversationInfo();
    }

    private void updateConversationInfo() {
        if (lblConversationInfo != null) {
            String role = (userRole != null) ? " (" + userRole + ")" : "";
            lblConversationInfo.setText("Conversation avec " + getOtherParticipantName() + role);
        }
        if (lblConversationTitle != null) {
            String titre = getConversationTitle();
            if (titre != null && !titre.isEmpty()) {
                lblConversationTitle.setText("📌 " + titre);
            } else {
                lblConversationTitle.setText("Conversation avec " + getOtherParticipantName());
            }
        }
    }

    private String getConversationTitle() {
        try {
            String sql = "SELECT titre FROM conversation WHERE id = ?";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, currentConversationId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("titre");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @FXML
    private void handleRetour() {
        cancelCallTimeout();
        if (isInCall && callClient != null) {
            callClient.endCall();
        }

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
        if (errorLabel == null) return;
        errorLabel.setText("❌ " + message);
        errorLabel.setStyle("-fx-text-fill: #dc2626;");
        errorLabel.setVisible(true);

        new Thread(() -> {
            try { Thread.sleep(3000); } catch (InterruptedException e) {}
            javafx.application.Platform.runLater(() -> errorLabel.setVisible(false));
        }).start();
    }

    private void showSuccess(String message) {
        if (errorLabel == null) return;
        errorLabel.setText("✅ " + message);
        errorLabel.setStyle("-fx-text-fill: #10b981;");
        errorLabel.setVisible(true);

        new Thread(() -> {
            try { Thread.sleep(2000); } catch (InterruptedException e) {}
            javafx.application.Platform.runLater(() -> errorLabel.setVisible(false));
        }).start();
    }
}