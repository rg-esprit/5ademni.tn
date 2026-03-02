package com.khademni.service;

import org.glassfish.tyrus.server.Server;
import java.util.Scanner;

public class WebSocketServerStarter {

    private static Server chatServer;
    private static Server callServer;

    public static void start() {
        // Serveur de chat (port 8082)
        new Thread(() -> {
            try {
                chatServer = new Server("localhost", 8082, "/ws", null, ChatWebSocketServer.class);
                chatServer.start();
                System.out.println("✅ Serveur CHAT démarré sur ws://localhost:8082/ws/chat/");
            } catch (Exception e) {
                System.err.println("❌ Erreur CHAT: " + e.getMessage());
            }
        }).start();

        // Serveur d'appel (port 8081) - CORRIGÉ !
        new Thread(() -> {
            try {
                // Essayer avec différents chemins
                callServer = new Server("localhost", 8081, "/", null, CallSignalingServer.class);
                callServer.start();
                System.out.println("✅ Serveur APPEL démarré sur ws://localhost:8081/call/");
            } catch (Exception e) {
                System.err.println("❌ Erreur APPEL: " + e.getMessage());
            }
        }).start();
    }

    public static void stop() {
        if (chatServer != null) chatServer.stop();
        if (callServer != null) callServer.stop();
        System.out.println("🛑 Serveurs arrêtés");
    }
}