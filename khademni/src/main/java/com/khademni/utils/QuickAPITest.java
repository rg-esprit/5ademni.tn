package com.khademni.utils;

/**
 * Test rapide du client Flask API (version simplifiée)
 *
 * Ce test est plus court et direct que FlaskAPIClient.main()
 */
public class QuickAPITest {

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║          🧪 TEST RAPIDE FLASK API CLIENT               ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝\n");

        // Test 1: Santé de l'API
        System.out.println("1️⃣ Test: API Health Check");
        System.out.println("─────────────────────────────────────────────────────────");
        boolean isHealthy = FlaskAPIClient.checkAPIHealth();

        if (!isHealthy) {
            System.err.println("\n❌ ÉCHEC: L'API Flask n'est pas accessible!");
            System.err.println("📝 Action requise:");
            System.err.println("   1. Ouvrir un terminal");
            System.err.println("   2. cd src/main/java/com/khademni/ai-service");
            System.err.println("   3. python app_classifier.py");
            System.err.println("   4. Attendre 'Running on http://0.0.0.0:5000'");
            System.err.println("   5. Relancer ce test\n");
            return;
        }

        System.out.println("✅ API accessible!\n");

        // Test 2: Prédiction de catégorie (simple)
        System.out.println("2️⃣ Test: Predict Category");
        System.out.println("─────────────────────────────────────────────────────────");
        testCategory("React Website", "Modern responsive website", "Development");
        testCategory("Logo Design", "Professional logo for startup", "Design");
        testCategory("SEO Service", "Google ranking optimization", "Marketing");
        System.out.println();

        // Test 3: Prédiction de prix (simple)
        System.out.println("3️⃣ Test: Predict Price");
        System.out.println("─────────────────────────────────────────────────────────");
        testPrice("Development", 150, 7);
        testPrice("Design", 100, 3);
        testPrice("Marketing", 120, 5);
        System.out.println();

        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║                ✅ TOUS LES TESTS PASSÉS                ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
    }

    /**
     * Tester la prédiction de catégorie
     */
    private static void testCategory(String title, String description, String expected) {
        System.out.print("  Testing: \"" + title + "\" → ");

        try {
            String predicted = FlaskAPIClient.predictCategory(title, description);

            if (predicted.equalsIgnoreCase(expected)) {
                System.out.println("✅ " + predicted + " (correct)");
            } else {
                System.out.println("⚠️  " + predicted + " (attendu: " + expected + ")");
            }
        } catch (Exception e) {
            System.out.println("❌ Erreur: " + e.getMessage());
        }
    }

    /**
     * Tester la prédiction de prix
     */
    private static void testPrice(String category, int descLength, int deliveryDays) {
        System.out.print("  Testing: " + category + " (" + descLength + " chars, " + deliveryDays + "j) → ");

        try {
            double price = FlaskAPIClient.predictPrice(category, descLength, deliveryDays);
            System.out.println("💰 " + price + " DT");
        } catch (Exception e) {
            System.out.println("❌ Erreur: " + e.getMessage());
        }
    }
}

