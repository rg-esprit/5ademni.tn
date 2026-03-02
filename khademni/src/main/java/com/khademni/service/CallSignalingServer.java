package com.khademni.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.List;

@ServerEndpoint("/call/{userId}")
public class CallSignalingServer {

    private static ConcurrentHashMap<String, Session> users = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, List<ObjectNode>> missedCalls = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, List<ObjectNode>> voiceMessages = new ConcurrentHashMap<>();
    private static ObjectMapper mapper = new ObjectMapper();

    @OnOpen
    public void onOpen(Session session, @PathParam("userId") String userId) {
        users.put(userId, session);
        System.out.println("🔵 Utilisateur connecté pour les appels: " + userId);

        // Vérifier les appels manqués
        checkMissedCalls(userId, session);

        // Vérifier les messages vocaux
        checkVoiceMessages(userId, session);
    }

    private void checkMissedCalls(String userId, Session session) {
        List<ObjectNode> missed = missedCalls.remove(userId);
        if (missed != null && !missed.isEmpty()) {
            try {
                for (ObjectNode missedCall : missed) {
                    session.getBasicRemote().sendText(missedCall.toString());
                }
                System.out.println("📞 " + missed.size() + " appel(s) manqué(s) notifié(s) à " + userId);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void checkVoiceMessages(String userId, Session session) {
        List<ObjectNode> messages = voiceMessages.remove(userId);
        if (messages != null && !messages.isEmpty()) {
            try {
                for (ObjectNode voiceMsg : messages) {
                    session.getBasicRemote().sendText(voiceMsg.toString());
                }
                System.out.println("🎤 " + messages.size() + " message(s) vocal(aux) notifié(s) à " + userId);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        try {
            JsonNode json = mapper.readTree(message);
            String type = json.get("type").asText();
            String fromUserId = json.get("from").asText();
            String targetUserId = json.get("target").asText();
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

            Session targetSession = users.get(targetUserId);

            if (targetSession != null && targetSession.isOpen()) {
                // Utilisateur en ligne - transfert direct
                targetSession.getBasicRemote().sendText(message);
                System.out.println("📤 Message " + type + " de " + fromUserId + " vers " + targetUserId);

                // Répondre à l'appelant que l'utilisateur est en ligne
                Session senderSession = users.get(fromUserId);
                if (senderSession != null && senderSession.isOpen()) {
                    ObjectNode response = mapper.createObjectNode();
                    response.put("type", "USER_ONLINE");
                    response.put("userId", targetUserId);
                    senderSession.getBasicRemote().sendText(response.toString());
                }

            } else {
                // Utilisateur hors ligne
                System.out.println("⚠️ Utilisateur " + targetUserId + " hors ligne");

                if ("CALL_REQUEST".equals(type)) {
                    // Sauvegarder l'appel manqué
                    ObjectNode missedCall = mapper.createObjectNode();
                    missedCall.put("type", "MISSED_CALL");
                    missedCall.put("from", fromUserId);
                    missedCall.put("timestamp", timestamp);
                    missedCall.put("message", "Appel manqué de " + fromUserId);

                    missedCalls.computeIfAbsent(targetUserId, k -> new ArrayList<>()).add(missedCall);
                    System.out.println("📞 Appel manqué enregistré pour " + targetUserId);

                    // Répondre à l'appelant
                    Session senderSession = users.get(fromUserId);
                    if (senderSession != null && senderSession.isOpen()) {
                        ObjectNode response = mapper.createObjectNode();
                        response.put("type", "USER_OFFLINE");
                        response.put("message", "L'utilisateur n'est pas en ligne");
                        response.put("timestamp", timestamp);
                        response.put("userId", targetUserId);
                        senderSession.getBasicRemote().sendText(response.toString());
                    }

                } else if ("VOICE_MESSAGE".equals(type)) {
                    // Sauvegarder le message vocal
                    String audioData = json.get("audioData").asText();
                    String duration = json.get("duration").asText();

                    ObjectNode voiceMsg = mapper.createObjectNode();
                    voiceMsg.put("type", "VOICE_MESSAGE");
                    voiceMsg.put("from", fromUserId);
                    voiceMsg.put("timestamp", timestamp);
                    voiceMsg.put("duration", duration);
                    voiceMsg.put("audioData", audioData);
                    voiceMsg.put("message", "Message vocal de " + fromUserId);

                    voiceMessages.computeIfAbsent(targetUserId, k -> new ArrayList<>()).add(voiceMsg);
                    System.out.println("🎤 Message vocal enregistré pour " + targetUserId + " (durée: " + duration + ")");

                    // Confirmation à l'expéditeur
                    Session senderSession = users.get(fromUserId);
                    if (senderSession != null && senderSession.isOpen()) {
                        ObjectNode response = mapper.createObjectNode();
                        response.put("type", "VOICE_MESSAGE_SENT");
                        response.put("message", "Message vocal envoyé à " + targetUserId);
                        response.put("timestamp", timestamp);
                        senderSession.getBasicRemote().sendText(response.toString());
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @OnClose
    public void onClose(Session session, @PathParam("userId") String userId) {
        users.remove(userId);
        System.out.println("🔴 Utilisateur déconnecté: " + userId);
        notifyUserDisconnected(userId);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        System.err.println("❌ Erreur WebSocket: " + error.getMessage());
        for (var entry : users.entrySet()) {
            if (entry.getValue().equals(session)) {
                users.remove(entry.getKey());
                notifyUserDisconnected(entry.getKey());
                break;
            }
        }
    }

    private void notifyUserDisconnected(String userId) {
        ObjectNode disconnectMsg = mapper.createObjectNode();
        disconnectMsg.put("type", "USER_DISCONNECTED");
        disconnectMsg.put("userId", userId);
        String message = disconnectMsg.toString();

        users.forEach((otherUserId, session) -> {
            if (!otherUserId.equals(userId) && session.isOpen()) {
                try {
                    session.getBasicRemote().sendText(message);
                } catch (IOException e) {
                    System.err.println("❌ Erreur notification déconnexion à " + otherUserId);
                }
            }
        });
    }
}