package com.khademni.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.application.Platform;

import javax.sound.sampled.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletionStage;

public class CallClient {

    private WebSocket webSocket;
    private String userId;
    private String targetUserId;
    private CallListener listener;
    private ObjectMapper mapper = new ObjectMapper();
    private boolean isConnected = false;
    private int reconnectAttempts = 0;
    private static final int MAX_RECONNECT_ATTEMPTS = 5;
    private boolean isCallRequested = false;
    private long lastCallTime = 0;
    private static final long MIN_CALL_INTERVAL = 5000;

    // Attributs audio
    private TargetDataLine microphone;
    private boolean isMicroMuted = false;
    private boolean isSpeakerOn = true;
    private Thread audioCaptureThread;

    public interface CallListener {
        void onIncomingCall(String callerId);
        void onCallAccepted();
        void onCallRejected();
        void onCallEnded();
        void onRemoteVideoFrame(String frameData);
        void onConnectionError(String error);
        void onMissedCall(String callerId, String timestamp);
        void onMissedCallNotification(String callerId, String timestamp);
        void onUserUnavailable(String message, String timestamp);
        void onUserOffline(String message, String timestamp);
        void onUserOnline(String userId);
        void onVoiceMessageReceived(String fromUserId, String audioData, String duration);
    }

    public CallClient(String userId, CallListener listener) {
        this.userId = userId;
        this.listener = listener;
        connect();
        initMicrophone();
    }

    private void connect() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            String wsUrl = "ws://localhost:8081/call/" + userId;

            System.out.println("🔌 Tentative de connexion à: " + wsUrl);

            client.newWebSocketBuilder()
                    .buildAsync(URI.create(wsUrl), new WebSocketListener())
                    .thenAccept(ws -> {
                        this.webSocket = ws;
                        this.isConnected = true;
                        this.reconnectAttempts = 0;
                        System.out.println("✅ Connecté au serveur d'appels");
                    })
                    .exceptionally(e -> {
                        System.err.println("❌ Erreur connexion: " + e.getMessage());
                        this.isConnected = false;

                        Platform.runLater(() -> {
                            if (listener != null) {
                                listener.onConnectionError("Impossible de se connecter au serveur d'appels");
                            }
                        });

                        if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
                            reconnectAttempts++;
                            int delay = 2000 * reconnectAttempts;
                            System.out.println("⏳ Nouvelle tentative dans " + delay/1000 + "s... (tentative " + reconnectAttempts + "/" + MAX_RECONNECT_ATTEMPTS + ")");

                            new Thread(() -> {
                                try {
                                    Thread.sleep(delay);
                                    connect();
                                } catch (InterruptedException ex) {}
                            }).start();
                        } else {
                            System.err.println("❌ Nombre maximum de tentatives atteint");
                        }

                        return null;
                    });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initMicrophone() {
        try {
            AudioFormat format = new AudioFormat(16000, 16, 1, true, true);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(format);
            microphone.start();
            System.out.println("✅ Microphone initialisé");

            startAudioCapture();

        } catch (LineUnavailableException e) {
            System.err.println("❌ Erreur initialisation microphone: " + e.getMessage());
            Platform.runLater(() -> {
                if (listener != null) {
                    listener.onConnectionError("Erreur microphone: " + e.getMessage());
                }
            });
        }
    }

    private void startAudioCapture() {
        audioCaptureThread = new Thread(() -> {
            byte[] buffer = new byte[1024];
            while (!Thread.currentThread().isInterrupted()) {
                if (microphone != null && microphone.isOpen() && !isMicroMuted && isConnected) {
                    try {
                        int bytesRead = microphone.read(buffer, 0, buffer.length);
                        if (bytesRead > 0) {
                            sendAudioData(buffer, bytesRead);
                        }
                    } catch (Exception e) {
                        // Ignorer
                    }
                } else {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        });
        audioCaptureThread.setDaemon(true);
        audioCaptureThread.start();
    }

    private void sendAudioData(byte[] data, int length) {
        if (webSocket != null && targetUserId != null && isConnected) {
            try {
                ObjectNode audioMessage = mapper.createObjectNode();
                audioMessage.put("type", "AUDIO");
                audioMessage.put("from", userId);
                audioMessage.put("target", targetUserId);
                audioMessage.put("data", java.util.Base64.getEncoder().encodeToString(java.util.Arrays.copyOf(data, length)));

                webSocket.sendText(audioMessage.toString(), true);
            } catch (Exception e) {
                // Ignorer
            }
        }
    }

    public void startCall(String targetUserId) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCallTime < MIN_CALL_INTERVAL) {
            System.err.println("⚠️ Trop d'appels, attendez quelques secondes");
            return;
        }

        if (targetUserId == null || targetUserId.isEmpty()) {
            System.err.println("❌ ID cible invalide");
            return;
        }

        if (webSocket == null || !isConnected) {
            System.err.println("❌ WebSocket non connecté");
            Platform.runLater(() -> {
                if (listener != null) {
                    listener.onConnectionError("Serveur d'appels non connecté. Tentative de reconnexion...");
                }
            });
            connect();
            return;
        }

        this.targetUserId = targetUserId;
        this.isCallRequested = true;
        this.lastCallTime = currentTime;

        try {
            ObjectNode callRequest = mapper.createObjectNode();
            callRequest.put("type", "CALL_REQUEST");
            callRequest.put("from", userId);
            callRequest.put("target", targetUserId);

            String jsonRequest = callRequest.toString();
            webSocket.sendText(jsonRequest, true);
            System.out.println("📞 Appel lancé vers " + targetUserId);

            // Démarrer un timer pour détecter si l'utilisateur est hors ligne
            new Thread(() -> {
                try {
                    Thread.sleep(5000);
                    if (isCallRequested && listener != null) {
                        Platform.runLater(() -> {
                            listener.onUserUnavailable("L'utilisateur ne répond pas",
                                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                        });
                    }
                    isCallRequested = false;
                } catch (InterruptedException e) {}
            }).start();

        } catch (Exception e) {
            System.err.println("❌ Erreur envoi appel: " + e.getMessage());
            e.printStackTrace();
            isCallRequested = false;
            isConnected = false;
            connect();
        }
    }

    public void acceptCall(String callerId) {
        if (webSocket == null || !isConnected) {
            System.err.println("❌ WebSocket non connecté");
            return;
        }

        this.targetUserId = callerId;

        try {
            ObjectNode acceptResponse = mapper.createObjectNode();
            acceptResponse.put("type", "CALL_ACCEPT");
            acceptResponse.put("from", userId);
            acceptResponse.put("target", callerId);

            webSocket.sendText(acceptResponse.toString(), true);
            System.out.println("✅ Appel accepté");
        } catch (Exception e) {
            System.err.println("❌ Erreur acceptation: " + e.getMessage());
        }
    }

    public void rejectCall(String callerId) {
        if (webSocket == null || !isConnected) {
            System.err.println("❌ WebSocket non connecté");
            return;
        }

        try {
            ObjectNode rejectResponse = mapper.createObjectNode();
            rejectResponse.put("type", "CALL_REJECT");
            rejectResponse.put("from", userId);
            rejectResponse.put("target", callerId);

            webSocket.sendText(rejectResponse.toString(), true);
            System.out.println("❌ Appel refusé");
        } catch (Exception e) {
            System.err.println("❌ Erreur refus: " + e.getMessage());
        }
    }

    public void endCall() {
        if (targetUserId != null && webSocket != null && isConnected) {
            try {
                ObjectNode endMessage = mapper.createObjectNode();
                endMessage.put("type", "CALL_END");
                endMessage.put("from", userId);
                endMessage.put("target", targetUserId);

                webSocket.sendText(endMessage.toString(), true);
                System.out.println("🔴 Appel terminé");
            } catch (Exception e) {
                System.err.println("❌ Erreur fin d'appel: " + e.getMessage());
            }
        }

        cleanup();
    }

    public void reportMissedCall(String callerId) {
        if (webSocket == null || !isConnected) {
            System.err.println("❌ WebSocket non connecté pour signaler appel manqué");
            return;
        }

        try {
            ObjectNode missedCallMsg = mapper.createObjectNode();
            missedCallMsg.put("type", "MISSED_CALL");
            missedCallMsg.put("from", userId);
            missedCallMsg.put("target", callerId);
            missedCallMsg.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));

            webSocket.sendText(missedCallMsg.toString(), true);
            System.out.println("📞 Appel manqué signalé pour " + callerId);
        } catch (Exception e) {
            System.err.println("❌ Erreur signalement appel manqué: " + e.getMessage());
        }
    }

    public void sendVoiceMessage(String targetUserId, byte[] audioData, String duration) {
        if (webSocket == null || !isConnected) {
            System.err.println("❌ WebSocket non connecté");
            return;
        }

        try {
            ObjectNode voiceMsg = mapper.createObjectNode();
            voiceMsg.put("type", "VOICE_MESSAGE");
            voiceMsg.put("from", userId);
            voiceMsg.put("target", targetUserId);
            voiceMsg.put("audioData", java.util.Base64.getEncoder().encodeToString(audioData));
            voiceMsg.put("duration", duration);

            webSocket.sendText(voiceMsg.toString(), true);
            System.out.println("🎤 Message vocal envoyé à " + targetUserId + " (durée: " + duration + ")");

        } catch (Exception e) {
            System.err.println("❌ Erreur envoi message vocal: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void toggleMicrophone() {
        isMicroMuted = !isMicroMuted;
        System.out.println("🎤 Micro " + (isMicroMuted ? "coupé" : "activé"));
    }

    public void toggleSpeaker() {
        isSpeakerOn = !isSpeakerOn;
        System.out.println("🔊 Haut-parleur " + (isSpeakerOn ? "activé" : "désactivé"));
    }

    public boolean isMicroMuted() {
        return isMicroMuted;
    }

    public boolean isSpeakerOn() {
        return isSpeakerOn;
    }

    public boolean isCallActive() {
        return targetUserId != null && isConnected;
    }

    public String getCurrentCallUser() {
        return targetUserId;
    }

    public void resetCallState() {
        isCallRequested = false;
    }

    private void cleanup() {
        targetUserId = null;
        if (audioCaptureThread != null) {
            audioCaptureThread.interrupt();
        }
        if (microphone != null && microphone.isOpen()) {
            microphone.stop();
            microphone.close();
        }
    }

    public void disconnect() {
        if (webSocket != null && isConnected) {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Fermeture normale");
        }
        cleanup();
        isConnected = false;
    }

    private class WebSocketListener implements WebSocket.Listener {

        @Override
        public void onOpen(WebSocket webSocket) {
            System.out.println("🔵 WebSocket ouvert pour les appels");
            isConnected = true;
            reconnectAttempts = 0;
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            String message = data.toString();
            System.out.println("📩 Message reçu (appel): " + message);

            try {
                JsonNode json = mapper.readTree(message);
                String type = json.get("type").asText();
                String from = json.get("from").asText();

                switch (type) {
                    case "CALL_REQUEST":
                        Platform.runLater(() -> {
                            if (listener != null) {
                                listener.onIncomingCall(from);
                            }
                        });
                        break;

                    case "CALL_ACCEPT":
                        Platform.runLater(() -> {
                            if (listener != null) {
                                listener.onCallAccepted();
                            }
                        });
                        break;

                    case "CALL_REJECT":
                        Platform.runLater(() -> {
                            if (listener != null) {
                                listener.onCallRejected();
                            }
                        });
                        break;

                    case "CALL_END":
                        Platform.runLater(() -> {
                            if (listener != null) {
                                listener.onCallEnded();
                            }
                            cleanup();
                        });
                        break;

                    case "AUDIO":
                        // Traitement audio entrant (à implémenter)
                        break;

                    case "USER_DISCONNECTED":
                        System.out.println("👤 Utilisateur " + from + " déconnecté");
                        if (targetUserId != null && targetUserId.equals(from)) {
                            Platform.runLater(() -> {
                                if (listener != null) {
                                    listener.onCallEnded();
                                }
                                cleanup();
                            });
                        }
                        break;

                    case "ERROR":
                        System.err.println("❌ Erreur serveur: " + json.get("message").asText());
                        Platform.runLater(() -> {
                            if (listener != null) {
                                listener.onConnectionError("Erreur serveur: " + json.get("message").asText());
                            }
                        });
                        break;

                    case "USER_UNAVAILABLE":
                        Platform.runLater(() -> {
                            String msg = json.get("message").asText();
                            String timestamp = json.get("timestamp").asText();
                            if (listener != null) {
                                listener.onUserUnavailable(msg, timestamp);
                                listener.onMissedCall(from, timestamp);
                            }
                        });
                        break;

                    case "USER_OFFLINE":
                        Platform.runLater(() -> {
                            String msg = json.get("message").asText();
                            String timestamp = json.get("timestamp").asText();
                            if (listener != null) {
                                listener.onUserOffline(msg, timestamp);
                                listener.onMissedCall(from, timestamp);
                            }
                        });
                        break;

                    case "USER_ONLINE":
                        Platform.runLater(() -> {
                            String userId = json.get("userId").asText();
                            if (listener != null) {
                                listener.onUserOnline(userId);
                            }
                        });
                        break;

                    case "MISSED_CALL_NOTIFICATION":
                        Platform.runLater(() -> {
                            String callerId = json.get("callerId").asText();
                            String timestamp = json.get("timestamp").asText();
                            if (listener != null) {
                                listener.onMissedCallNotification(callerId, timestamp);
                            }
                        });
                        break;

                    case "MISSED_CALL":
                        Platform.runLater(() -> {
                            String callerId = json.get("from").asText();
                            String timestamp = json.get("timestamp").asText();
                            if (listener != null) {
                                listener.onMissedCall(callerId, timestamp);
                            }
                        });
                        break;

                    case "VOICE_MESSAGE":
                        Platform.runLater(() -> {
                            String fromUserId = json.get("from").asText();
                            String audioData = json.get("audioData").asText();
                            String duration = json.get("duration").asText();
                            if (listener != null) {
                                listener.onVoiceMessageReceived(fromUserId, audioData, duration);
                            }
                        });
                        break;

                    case "VOICE_MESSAGE_SENT":
                        Platform.runLater(() -> {
                            String msg = json.get("message").asText();
                            System.out.println("✅ " + msg);
                        });
                        break;
                }

            } catch (Exception e) {
                System.err.println("❌ Erreur parsing message: " + e.getMessage());
                e.printStackTrace();
            }

            return WebSocket.Listener.super.onText(webSocket, data, last);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            System.err.println("❌ Erreur WebSocket: " + error.getMessage());
            isConnected = false;

            Platform.runLater(() -> {
                if (listener != null) {
                    listener.onConnectionError("Erreur de connexion: " + error.getMessage());
                }
            });

            if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
                new Thread(() -> {
                    try {
                        Thread.sleep(3000);
                        connect();
                    } catch (InterruptedException ex) {}
                }).start();
            }
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            System.out.println("🔴 WebSocket fermé: " + reason);
            isConnected = false;
            cleanup();

            Platform.runLater(() -> {
                if (listener != null && targetUserId != null) {
                    listener.onCallEnded();
                }
            });

            return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
        }
    }
}