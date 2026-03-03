package com.khademni.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;
import org.bytedeco.javacv.*;
import org.bytedeco.javacv.Frame;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;

public class CallClient {

    private WebSocket webSocket;
    private String userId;
    private String targetUserId;
    private CallListener listener;
    private ObjectMapper mapper = new ObjectMapper();

    // Pour la vidéo
    private OpenCVFrameGrabber grabber;
    private CanvasFrame canvasFrame;
    private boolean isCallActive = false;

    public interface CallListener {
        void onIncomingCall(String callerId);
        void onCallAccepted();
        void onCallRejected();
        void onCallEnded();
        void onRemoteVideoFrame(Frame frame);
    }

    public CallClient(String userId, CallListener listener) {
        this.userId = userId;
        this.listener = listener;
        connect();
    }

    private void connect() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            String wsUrl = "ws://localhost:8081/ws/call/" + userId;

            client.newWebSocketBuilder()
                    .buildAsync(URI.create(wsUrl), new WebSocketListener())
                    .thenAccept(ws -> this.webSocket = ws);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startCall(String targetUserId) {
        this.targetUserId = targetUserId;

        // Envoyer une demande d'appel
        ObjectNode callRequest = mapper.createObjectNode();
        callRequest.put("type", "CALL_REQUEST");
        callRequest.put("from", userId);
        callRequest.put("target", targetUserId);

        webSocket.sendText(callRequest.toString(), true);
    }

    public void acceptCall(String callerId) {
        ObjectNode acceptResponse = mapper.createObjectNode();
        acceptResponse.put("type", "CALL_ACCEPT");
        acceptResponse.put("from", userId);
        acceptResponse.put("target", callerId);

        webSocket.sendText(acceptResponse.toString(), true);
        startVideoStream();
    }

    public void rejectCall(String callerId) {
        ObjectNode rejectResponse = mapper.createObjectNode();
        rejectResponse.put("type", "CALL_REJECT");
        rejectResponse.put("from", userId);
        rejectResponse.put("target", callerId);

        webSocket.sendText(rejectResponse.toString(), true);
    }

    public void endCall() {
        if (targetUserId != null) {
            ObjectNode endMessage = mapper.createObjectNode();
            endMessage.put("type", "CALL_END");
            endMessage.put("from", userId);
            endMessage.put("target", targetUserId);

            webSocket.sendText(endMessage.toString(), true);
        }
        stopVideoStream();
    }

    private void startVideoStream() {
        isCallActive = true;

        // Démarrer la capture vidéo
        new Thread(() -> {
            try {
                grabber = new OpenCVFrameGrabber(0); // 0 = caméra par défaut
                grabber.start();

                canvasFrame = new CanvasFrame("Appel avec " + targetUserId);
                canvasFrame.setDefaultCloseOperation(javax.swing.JFrame.DO_NOTHING_ON_CLOSE);

                Frame frame;
                while (isCallActive && (frame = grabber.grab()) != null) {
                    canvasFrame.showImage(frame);

                    // Envoyer le frame compressé à l'autre utilisateur
                    sendVideoFrame(frame);

                    Thread.sleep(33); // ~30 FPS
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void sendVideoFrame(Frame frame) {
        // Compresser et envoyer le frame via WebSocket
        // Implémentation simplifiée
    }

    private void stopVideoStream() {
        isCallActive = false;
        try {
            if (grabber != null) grabber.stop();
            if (canvasFrame != null) canvasFrame.dispose();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class WebSocketListener implements WebSocket.Listener {

        @Override
        public void onOpen(WebSocket webSocket) {
            System.out.println("✅ Connecté au serveur d'appels");
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            String message = data.toString();

            try {
                JsonNode json = mapper.readTree(message);
                String type = json.get("type").asText();
                String from = json.get("from").asText();

                switch (type) {
                    case "CALL_REQUEST":
                        Platform.runLater(() -> listener.onIncomingCall(from));
                        break;
                    case "CALL_ACCEPT":
                        Platform.runLater(() -> {
                            listener.onCallAccepted();
                            startVideoStream();
                        });
                        break;
                    case "CALL_REJECT":
                        Platform.runLater(() -> listener.onCallRejected());
                        break;
                    case "CALL_END":
                        Platform.runLater(() -> {
                            listener.onCallEnded();
                            stopVideoStream();
                        });
                        break;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            return WebSocket.Listener.super.onText(webSocket, data, last);
        }
    }
}