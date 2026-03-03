package com.khademni.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.khademni.utils.MyDataBase;

import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/chat/{conversationId}")
public class ChatWebSocketServer {

    // Stocke les sessions par conversation
    private static ConcurrentHashMap<String, ConcurrentHashMap<Session, Boolean>> rooms
            = new ConcurrentHashMap<>();

    private ObjectMapper mapper = new ObjectMapper();

    @OnOpen
    public void onOpen(Session session, @PathParam("conversationId") String convId) {
        System.out.println("🔵 Nouvelle connexion - Conversation: " + convId);

        // Ajouter la session à la room
        rooms.putIfAbsent(convId, new ConcurrentHashMap<>());
        rooms.get(convId).put(session, true);

        // Configurer la session
        session.setMaxIdleTimeout(0); // Pas de timeout

        // Envoyer confirmation
        sendToClient(session, "SYSTEM", "Connecté à la conversation " + convId);
    }

    @OnMessage
    public void onMessage(String message, Session session, @PathParam("conversationId") String convId) {
        System.out.println("📩 Message reçu: " + message);

        try {
            // Parser le message JSON
            JsonNode json = mapper.readTree(message);
            String contenu = json.get("contenu").asText();
            String expediteur = json.get("expediteur").asText();
            int clientId = json.get("clientId").asInt();
            int freelanceId = json.get("freelanceId").asInt();

            // Sauvegarder dans MySQL
            int messageId = saveToDatabase(convId, contenu, expediteur, clientId, freelanceId);

            // Préparer la réponse
            ObjectNode response = mapper.createObjectNode();
            response.put("id", messageId);
            response.put("contenu", contenu);
            response.put("expediteur", expediteur);
            response.put("date", LocalDateTime.now().toString());
            response.put("type", "TEXTE");

            String responseStr = mapper.writeValueAsString(response);

            // Diffuser à tous les clients de la conversation
            broadcastToRoom(convId, session, responseStr);

        } catch (Exception e) {
            e.printStackTrace();
            sendToClient(session, "SYSTEM", "Erreur: " + e.getMessage());
        }
    }

    private int saveToDatabase(String convId, String contenu, String expediteur,
                               int clientId, int freelanceId) throws SQLException {
        String sql = "INSERT INTO message (contenu, expediteur, conversation_id, date_envoie, type_message) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = MyDataBase.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, contenu);
            ps.setString(2, expediteur);
            ps.setInt(3, Integer.parseInt(convId));
            ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(5, "TEXTE");

            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return -1;
    }

    private void broadcastToRoom(String convId, Session sender, String message) {
        ConcurrentHashMap<Session, Boolean> room = rooms.get(convId);
        if (room != null) {
            for (Session s : room.keySet()) {
                if (s.isOpen() && !s.equals(sender)) {
                    try {
                        s.getBasicRemote().sendText(message);
                    } catch (IOException e) {
                        System.err.println("Erreur broadcast: " + e.getMessage());
                    }
                }
            }
        }
    }

    private void sendToClient(Session session, String type, String content) {
        try {
            ObjectNode msg = mapper.createObjectNode();
            msg.put("type", type);
            msg.put("content", content);
            session.getBasicRemote().sendText(mapper.writeValueAsString(msg));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @OnClose
    public void onClose(Session session, @PathParam("conversationId") String convId) {
        System.out.println("🔴 Connexion fermée - Conversation: " + convId);

        ConcurrentHashMap<Session, Boolean> room = rooms.get(convId);
        if (room != null) {
            room.remove(session);
            if (room.isEmpty()) {
                rooms.remove(convId);
            }
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        System.err.println("❌ Erreur WebSocket: " + error.getMessage());
    }
}