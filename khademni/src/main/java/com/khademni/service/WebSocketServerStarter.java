package com.khademni.service;

import org.glassfish.tyrus.server.Server;
import java.util.Scanner;

public class WebSocketServerStarter {

    private static Server server;

    public static void start() {
        new Thread(() -> {
            try {
                server = new Server("localhost", 8081, "/ws", null, ChatWebSocketServer.class);
                server.start();
                System.out.println("✅ Serveur WebSocket démarré sur ws://localhost:8080/ws/chat/");
                System.out.println("⏳ En attente de connexions...");
            } catch (Exception e) {
                System.err.println("❌ Erreur démarrage serveur: " + e.getMessage());
            }
        }).start();
    }

    public static void stop() {
        if (server != null) {
            server.stop();
            System.out.println("🛑 Serveur WebSocket arrêté");
        }
    }

    // Pour tester en standalone
    public static void main(String[] args) {
        start();

        // Attendre que l'utilisateur appuie sur Entrée pour arrêter
        System.out.println("Appuyez sur Entrée pour arrêter le serveur...");
        new Scanner(System.in).nextLine();
        stop();
    }
}