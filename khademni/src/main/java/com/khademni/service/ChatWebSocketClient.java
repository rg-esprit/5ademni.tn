package com.khademni.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;

public class ChatWebSocketClient {

    private WebSocket webSocket;
    private MessageListener listener;
    private String conversationId;
    private int clientId;
    private int freelanceId;
    private String expediteur;
    private boolean connected = false;
    private ObjectMapper mapper = new ObjectMapper();

    public interface MessageListener {
        void onMessage(JsonNode message);
        void onError(String error);
        void onClose();
        void onConnectionStatus(boolean connected);
    }

    public ChatWebSocketClient(String conversationId, int clientId, int freelanceId,
                               String expediteur, MessageListener listener) {
        this.conversationId = conversationId;
        this.clientId = clientId;
        this.freelanceId = freelanceId;
        this.expediteur = expediteur;
        this.listener = listener;
        connect();
    }

    private void connect() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            String wsUrl = "ws://localhost:8081/ws/chat/" + conversationId;

            client.newWebSocketBuilder()
                    .buildAsync(URI.create(wsUrl), new WebSocketListener())
                    .thenAccept(ws -> {
                        this.webSocket = ws;
                        this.connected = true;
                        System.out.println("✅ WebSocket connecté à: " + wsUrl);
                        if (listener != null) {
                            Platform.runLater(() -> listener.onConnectionStatus(true));
                        }
                    })
                    .exceptionally(e -> {
                        Platform.runLater(() -> {
                            if (listener != null) {
                                listener.onError("Erreur connexion: " + e.getMessage());
                                listener.onConnectionStatus(false);
                            }
                        });
                        return null;
                    });

        } catch (Exception e) {
            e.printStackTrace();
            if (listener != null) {
                listener.onError("Exception: " + e.getMessage());
                listener.onConnectionStatus(false);
            }
        }
    }

    public void sendMessage(String contenu) {
        if (!connected || webSocket == null) {
            System.err.println("WebSocket non connecté");
            return;
        }

        try {
            // Créer le message au format attendu par le serveur
            String message = "{\"contenu\":\"" + contenu + "\",\"expediteur\":\"" + expediteur +
                    "\",\"clientId\":" + clientId + ",\"freelanceId\":" + freelanceId + "}";

            webSocket.sendText(message, true);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void close() {
        if (webSocket != null) {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Fermeture normale");
        }
        connected = false;
        if (listener != null) {
            Platform.runLater(() -> listener.onConnectionStatus(false));
        }
    }

    public boolean isConnected() {
        return connected;
    }

    private class WebSocketListener implements WebSocket.Listener {

        @Override
        public void onOpen(WebSocket webSocket) {
            System.out.println("🔵 Connexion WebSocket ouverte");
            WebSocket.Listener.super.onOpen(webSocket);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            String message = data.toString();
            System.out.println("📩 Message reçu: " + message);

            try {
                JsonNode json = mapper.readTree(message);

                // Vérifier si c'est un message système
                if (json.has("type") && "SYSTEM".equals(json.get("type").asText())) {
                    // C'est un message système, on l'ignore ou on le log
                    System.out.println("🔔 Message système: " +
                            (json.has("content") ? json.get("content").asText() : ""));

                    // Optionnel: notifier le listener
                    if (listener != null) {
                        Platform.runLater(() -> listener.onMessage(json));
                    }
                } else {
                    // C'est un message normal de conversation
                    if (listener != null) {
                        Platform.runLater(() -> listener.onMessage(json));
                    }
                }

            } catch (Exception e) {
                System.err.println("❌ Erreur parsing: " + e.getMessage());
                e.printStackTrace();
            }

            return WebSocket.Listener.super.onText(webSocket, data, last);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            System.err.println("❌ Erreur WebSocket: " + error.getMessage());
            connected = false;

            if (listener != null) {
                Platform.runLater(() -> {
                    listener.onError("Erreur: " + error.getMessage());
                    listener.onConnectionStatus(false);
                });
            }
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            System.out.println("🔴 WebSocket fermé: " + reason);
            connected = false;

            if (listener != null) {
                Platform.runLater(() -> {
                    listener.onClose();
                    listener.onConnectionStatus(false);
                });
            }

            return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
        }
    }
}