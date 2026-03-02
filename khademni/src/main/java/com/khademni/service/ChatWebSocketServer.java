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

@ServerEndpoint("/ws/chat/{conversationId}")
public class ChatWebSocketServer {

    private static ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();
    private static ObjectMapper mapper = new ObjectMapper();

    @OnOpen
    public void onOpen(Session session, @PathParam("conversationId") String conversationId) {
        sessions.put(conversationId + "_" + session.getId(), session);
        System.out.println("🔵 Nouvelle connexion - Conversation: " + conversationId);

        // Envoyer un message de confirmation
        try {
            ObjectNode welcomeMsg = mapper.createObjectNode();
            welcomeMsg.put("type", "SYSTEM");
            welcomeMsg.put("content", "Connecté à la conversation " + conversationId);
            session.getBasicRemote().sendText(welcomeMsg.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        try {
            JsonNode json = mapper.readTree(message);

            // Vérifier si c'est une requête GET_MISSED_CALLS
            if (json.has("type") && "GET_MISSED_CALLS".equals(json.get("type").asText())) {
                // Traiter la requête d'appels manqués
                int userId = json.get("userId").asInt();
                System.out.println("📞 Requête d'appels manqués pour l'utilisateur: " + userId);

                // Répondre avec les appels manqués (à implémenter selon votre logique)
                ObjectNode response = mapper.createObjectNode();
                response.put("type", "MISSED_CALLS_RESPONSE");
                response.put("userId", userId);
                response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                session.getBasicRemote().sendText(response.toString());
                return;
            }

            // Vérifier si c'est un message de chat normal
            if (json.has("contenu") && json.has("expediteur")) {
                String contenu = json.get("contenu").asText();
                String expediteur = json.get("expediteur").asText();

                ObjectNode chatMsg = mapper.createObjectNode();
                chatMsg.put("id", System.currentTimeMillis());
                chatMsg.put("contenu", contenu);
                chatMsg.put("expediteur", expediteur);
                chatMsg.put("date", LocalDateTime.now().toString());

                // Diffuser à toutes les sessions de la même conversation
                String targetConversationId = null;
                for (String key : sessions.keySet()) {
                    if (key.startsWith(targetConversationId)) {
                        Session s = sessions.get(key);
                        if (s.isOpen()) {
                            s.getBasicRemote().sendText(chatMsg.toString());
                        }
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur traitement message: " + e.getMessage());
            e.printStackTrace();

            // Envoyer une erreur au client
            try {
                ObjectNode errorMsg = mapper.createObjectNode();
                errorMsg.put("type", "ERROR");
                errorMsg.put("message", "Erreur de traitement: " + e.getMessage());
                session.getBasicRemote().sendText(errorMsg.toString());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    @OnClose
    public void onClose(Session session, @PathParam("conversationId") String conversationId) {
        String keyToRemove = null;
        for (String key : sessions.keySet()) {
            if (key.startsWith(conversationId) && sessions.get(key).equals(session)) {
                keyToRemove = key;
                break;
            }
        }
        if (keyToRemove != null) {
            sessions.remove(keyToRemove);
        }
        System.out.println("🔴 Déconnexion - Conversation: " + conversationId);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        System.err.println("❌ Erreur WebSocket: " + error.getMessage());
    }
}