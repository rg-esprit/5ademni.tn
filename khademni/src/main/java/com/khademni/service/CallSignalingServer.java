package com.khademni.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/ws/call/{userId}")
public class CallSignalingServer {

    private static ConcurrentHashMap<String, Session> users = new ConcurrentHashMap<>();
    private static ObjectMapper mapper = new ObjectMapper();

    @OnOpen
    public void onOpen(Session session, @PathParam("userId") String userId) {
        users.put(userId, session);
        System.out.println("🔵 Utilisateur connecté pour les appels: " + userId);
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        try {
            JsonNode json = mapper.readTree(message);
            String type = json.get("type").asText();
            String targetUserId = json.get("target").asText();

            Session targetSession = users.get(targetUserId);
            if (targetSession != null && targetSession.isOpen()) {
                // Transférer le message à l'utilisateur cible
                targetSession.getBasicRemote().sendText(message);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @OnClose
    public void onClose(Session session, @PathParam("userId") String userId) {
        users.remove(userId);
        System.out.println("🔴 Utilisateur déconnecté: " + userId);
    }
}