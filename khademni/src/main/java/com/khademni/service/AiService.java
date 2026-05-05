package com.khademni.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class AiService {

    private static AiService instance;
    private final HttpClient httpClient;
    private final String geminiKey = ""; // Key would normally go here
    private final String geminiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent";

    private AiService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public static synchronized AiService getInstance() {
        if (instance == null) {
            instance = new AiService();
        }
        return instance;
    }

    public Map<String, Object> analyzePaymentRisk(double amount, double platformAvg, double platformMax, String title) {
        Map<String, Object> result = new HashMap<>();
        
        if (amount <= 0) {
            result.put("suspicious", true);
            result.put("reason", "Montant invalide.");
            return result;
        }

        // Heuristic checks (matching Symfony)
        if (amount > 1000000 || (platformMax > 0 && amount > platformMax * 10)) {
            result.put("suspicious", true);
            result.put("reason", "Montant anormalement élevé.");
            result.put("suggestion", suggestCorrectAmount(amount));
            return result;
        }

        if (platformAvg > 0 && amount > platformAvg * 20) {
            result.put("suspicious", true);
            result.put("reason", "Le montant depasse largement la moyenne de la plateforme.");
            result.put("suggestion", suggestCorrectAmount(amount));
            return result;
        }

        result.put("suspicious", false);
        return result;
    }

    public String generateDescription(String hint, String title, double price) {
        if (hint == null || hint.isBlank()) return "Veuillez fournir quelques indications pour l'IA.";
        
        // Mocking a professional response as if it were from Gemini
        // In a real scenario, this would call the API.
        return "CONTRAT DE PRESTATION : " + (title != null ? title.toUpperCase() : "SERVICE") + "\n\n" +
               "1. PÉRIMÈTRE DU PROJET\n" +
               "Le prestataire s'engage à réaliser : " + hint + "\n\n" +
               "2. LIVRABLES\n" +
               "- Code source complet et documenté.\n" +
               "- Tests unitaires et d'intégration.\n\n" +
               "3. DÉLAIS ET BUDGET\n" +
               "Le budget total est fixé à " + String.format("%.2f", price) + " TND.\n" +
               "Le délai sera convenu mutuellement après signature.\n\n" +
               "4. CONDITIONS GÉNÉRALES\n" +
               "Propriété intellectuelle transférée au client après paiement final.";
    }

    private String suggestCorrectAmount(double amount) {
        String s = String.valueOf((long) amount);
        if (s.length() > 6) {
            String suggested = s.substring(0, (int) Math.ceil(s.length() / 2.0));
            return "Vouliez-vous dire " + suggested + " TND ?";
        }
        return null;
    }
}
