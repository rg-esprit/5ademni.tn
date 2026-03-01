package com.khademni.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Client Java pour communiquer avec les API Flask locales
 *
 * Endpoints disponibles:
 * 1. POST /predict_category - Prédire la catégorie d'un gig
 * 2. POST /predict_price - Prédire le prix recommandé
 */
public class FlaskAPIClient {

    // URLs des API Flask
    private static final String CATEGORY_API_URL = "http://localhost:5000";
    private static final String PRICE_API_URL = "http://localhost:5001";
    private static final String PREDICT_CATEGORY_URL = CATEGORY_API_URL + "/predict_category";
    private static final String PREDICT_PRICE_URL = PRICE_API_URL + "/predict_price";

    // Configuration
    private static final int TIMEOUT_MS = 2000; // 2 secondes (fallback rapide)

    /**
     * Fonction générique pour appeler une API
     *
     * @param urlString URL de l'API
     * @param jsonInput JSON à envoyer
     * @return Réponse JSON de l'API
     * @throws Exception Si erreur réseau ou serveur
     */
    public static String callAPI(String urlString, String jsonInput) throws Exception {
        System.out.println("\n🌐 Appel API: " + urlString);
        System.out.println("📤 Données envoyées: " + jsonInput);

        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        try {
            // Configuration de la connexion
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);

            // Envoi des données JSON
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInput.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Lecture de la réponse
            int responseCode = conn.getResponseCode();
            System.out.println("📥 Code de réponse: " + responseCode);

            BufferedReader br;
            if (responseCode >= 200 && responseCode < 300) {
                // Succès
                br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            } else {
                // Erreur
                br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
            }

            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            br.close();

            String responseBody = response.toString();
            System.out.println("✅ Réponse reçue: " + responseBody);

            return responseBody;

        } finally {
            conn.disconnect();
        }
    }

    /**
     * Prédire la catégorie d'un gig
     *
     * @param title Titre du gig
     * @param description Description du gig
     * @return Catégorie prédite (ex: "Development", "Design", etc.)
     */
    public static String predictCategory(String title, String description) {
        try {
            // Combiner titre et description
            String text = title + " " + description;

            // Construire le JSON
            String jsonInput = String.format(
                "{\"text\": \"%s\"}",
                escapeJson(text)
            );

            // Appeler l'API
            String response = callAPI(PREDICT_CATEGORY_URL, jsonInput);

            // Parser la réponse (simple parsing)
            String category = extractJsonValue(response, "category");

            if (category != null) {
                System.out.println("🎯 Catégorie prédite: " + category);
                return category;
            } else {
                System.out.println("⚠️ Aucune catégorie trouvée dans la réponse");
                return "Unknown";
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la prédiction de catégorie: " + e.getMessage());
            e.printStackTrace();
            return "Error";
        }
    }

    /**
     * Prédire le prix recommandé pour un gig
     *
     * @param category Catégorie du gig
     * @param descriptionLength Longueur de la description
     * @param deliveryTime Temps de livraison en jours
     * @return Prix recommandé en DT
     * @throws Exception si l'API Flask est inaccessible
     */
    public static double predictPrice(String category, int descriptionLength, int deliveryTime) throws Exception {
        // Construire le JSON
        String jsonInput = String.format(
            "{\"category\": \"%s\", \"description_length\": %d, \"delivery_time\": %d}",
            escapeJson(category),
            descriptionLength,
            deliveryTime
        );

        // Appeler l'API (lance une exception si indisponible)
        String response = callAPI(PREDICT_PRICE_URL, jsonInput);

        // Parser la réponse
        String priceStr = extractJsonValue(response, "recommended_price");

        if (priceStr != null) {
            double price = Double.parseDouble(priceStr);
            System.out.println("💰 Prix recommandé: " + price + " DT");
            return price;
        } else {
            throw new Exception("Aucun prix trouvé dans la réponse: " + response);
        }
    }

    /**
     * Échapper les caractères spéciaux pour JSON
     */
    private static String escapeJson(String text) {
        if (text == null) return "";
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }

    /**
     * Extraire une valeur d'un JSON simple (parsing basique)
     *
     * @param json JSON complet
     * @param key Clé à extraire
     * @return Valeur de la clé
     */
    private static String extractJsonValue(String json, String key) {
        if (json == null || key == null) return null;

        // Pattern: "key": "value" ou "key": value
        String pattern1 = "\"" + key + "\":\\s*\"([^\"]+)\"";
        String pattern2 = "\"" + key + "\":\\s*([^,}]+)";

        // Essayer pattern1 (valeur entre guillemets)
        java.util.regex.Pattern p1 = java.util.regex.Pattern.compile(pattern1);
        java.util.regex.Matcher m1 = p1.matcher(json);
        if (m1.find()) {
            return m1.group(1);
        }

        // Essayer pattern2 (valeur numérique)
        java.util.regex.Pattern p2 = java.util.regex.Pattern.compile(pattern2);
        java.util.regex.Matcher m2 = p2.matcher(json);
        if (m2.find()) {
            return m2.group(1).trim();
        }

        return null;
    }

    /**
     * Vérifier si les API Flask sont accessibles
     */
    public static boolean checkAPIHealth() {
        try {
            System.out.println("\n🏥 Vérification de la santé des API...");

            URL url = new URL(CATEGORY_API_URL + "/health");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);

            int responseCode = conn.getResponseCode();
            conn.disconnect();

            if (responseCode == 200) {
                System.out.println("✅ API Flask opérationnelle (code: " + responseCode + ")");
                return true;
            } else {
                System.out.println("⚠️ API Flask répond mais avec erreur (code: " + responseCode + ")");
                return false;
            }

        } catch (Exception e) {
            System.err.println("❌ API Flask non accessible: " + e.getMessage());
            System.err.println("💡 Assurez-vous que l'API Flask est démarrée (python app_classifier.py)");
            return false;
        }
    }

    /**
     * Main de test
     */
    public static void main(String[] args) {
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("  🧪 TEST DU CLIENT API FLASK");
        System.out.println("═══════════════════════════════════════════════════════════════");

        // Vérifier la santé de l'API
        if (!checkAPIHealth()) {
            System.err.println("\n❌ ERREUR: L'API Flask n'est pas accessible!");
            System.err.println("Démarrez l'API avec: python app_classifier.py");
            return;
        }

        System.out.println("\n═══════════════════════════════════════════════════════════════");
        System.out.println("  TEST 1: Prédiction de catégorie");
        System.out.println("═══════════════════════════════════════════════════════════════");

        // Test 1: Prédire la catégorie d'un gig de développement
        String title1 = "React Website Development";
        String description1 = "I will create a modern and responsive website using React.js";
        String category1 = predictCategory(title1, description1);

        // Test 2: Prédire la catégorie d'un gig de design
        String title2 = "Professional Logo Design";
        String description2 = "I will design a unique and modern logo for your brand";
        String category2 = predictCategory(title2, description2);

        // Test 3: Prédire la catégorie d'un gig de marketing
        String title3 = "SEO Optimization";
        String description3 = "I will optimize your website for search engines";
        String category3 = predictCategory(title3, description3);

        System.out.println("\n═══════════════════════════════════════════════════════════════");
        System.out.println("  TEST 2: Prédiction de prix");
        System.out.println("═══════════════════════════════════════════════════════════════");

        // Test 4: Prédire le prix pour un gig de développement
        double price1 = 0, price2 = 0, price3 = 0;
        try { price1 = predictPrice("Development", 150, 7); } catch (Exception e) { System.err.println("Prix Development: " + e.getMessage()); }
        try { price2 = predictPrice("Design", 100, 3); } catch (Exception e) { System.err.println("Prix Design: " + e.getMessage()); }
        try { price3 = predictPrice("Marketing", 120, 5); } catch (Exception e) { System.err.println("Prix Marketing: " + e.getMessage()); }

        System.out.println("\n═══════════════════════════════════════════════════════════════");
        System.out.println("  📊 RÉSUMÉ DES TESTS");
        System.out.println("═══════════════════════════════════════════════════════════════");

        System.out.println("\n🎯 Catégories prédites:");
        System.out.println("  • \"" + title1 + "\" → " + category1);
        System.out.println("  • \"" + title2 + "\" → " + category2);
        System.out.println("  • \"" + title3 + "\" → " + category3);

        System.out.println("\n💰 Prix prédits:");
        System.out.println("  • Development (150 chars, 7j) → " + price1 + " DT");
        System.out.println("  • Design (100 chars, 3j) → " + price2 + " DT");
        System.out.println("  • Marketing (120 chars, 5j) → " + price3 + " DT");

        System.out.println("\n═══════════════════════════════════════════════════════════════");
        System.out.println("  ✅ TESTS TERMINÉS");
        System.out.println("═══════════════════════════════════════════════════════════════\n");
    }
}

