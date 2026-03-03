package com.khademni.utils;

/**
 * Test rapide de la classification automatique de catégorie
 */
public class TestCategoryPrediction {

    public static void main(String[] args) {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  🤖 TEST CLASSIFICATION AUTOMATIQUE");
        System.out.println("═══════════════════════════════════════════════════\n");

        FreeAIService ai = new FreeAIService();

        // Test 1: Web Development
        System.out.println("Test 1: Web Development");
        System.out.println("───────────────────────────────────────────────────");
        String result1 = ai.predictCategory(
            "Je vais créer un site e-commerce en React",
            "Site moderne avec panier, paiement Stripe, interface responsive"
        );
        System.out.println("✓ Résultat: " + result1);
        System.out.println("✓ Attendu: Web Development");
        System.out.println("✓ Correct: " + (result1 != null && result1.contains("Web")) + "\n");

        // Test 2: Graphic Design
        System.out.println("Test 2: Graphic Design");
        System.out.println("───────────────────────────────────────────────────");
        String result2 = ai.predictCategory(
            "Je vais créer votre logo professionnel",
            "Design unique avec Photoshop et Illustrator, fichiers PNG et SVG"
        );
        System.out.println("✓ Résultat: " + result2);
        System.out.println("✓ Attendu: Graphic Design");
        System.out.println("✓ Correct: " + (result2 != null && result2.contains("Design")) + "\n");

        // Test 3: Video Editing
        System.out.println("Test 3: Video Editing");
        System.out.println("───────────────────────────────────────────────────");
        String result3 = ai.predictCategory(
            "Montage vidéo YouTube professionnel",
            "Editing avec Adobe Premiere Pro, animations, transitions, musique"
        );
        System.out.println("✓ Résultat: " + result3);
        System.out.println("✓ Attendu: Video Editing");
        System.out.println("✓ Correct: " + (result3 != null && result3.contains("Video")) + "\n");

        // Test 4: Mobile Development
        System.out.println("Test 4: Mobile Development");
        System.out.println("───────────────────────────────────────────────────");
        String result4 = ai.predictCategory(
            "Développement application mobile Flutter",
            "App cross-platform Android et iOS avec Flutter et Firebase"
        );
        System.out.println("✓ Résultat: " + result4);
        System.out.println("✓ Attendu: Mobile Development");
        System.out.println("✓ Correct: " + (result4 != null && result4.contains("Mobile")) + "\n");

        // Test 5: Data Analysis
        System.out.println("Test 5: Data Analysis");
        System.out.println("───────────────────────────────────────────────────");
        String result5 = ai.predictCategory(
            "Analyse de données avec Python",
            "Analyse statistique avec Pandas, visualisation, dashboard Excel"
        );
        System.out.println("✓ Résultat: " + result5);
        System.out.println("✓ Attendu: Data Analysis");
        System.out.println("✓ Correct: " + (result5 != null && result5.contains("Data")) + "\n");

        // Test 6: Digital Marketing
        System.out.println("Test 6: Digital Marketing");
        System.out.println("───────────────────────────────────────────────────");
        String result6 = ai.predictCategory(
            "Gestion campagne Google Ads",
            "Publicité Google Ads, Facebook Ads, optimisation SEO"
        );
        System.out.println("✓ Résultat: " + result6);
        System.out.println("✓ Attendu: Digital Marketing");
        System.out.println("✓ Correct: " + (result6 != null && result6.contains("Marketing")) + "\n");

        // Test 7: Workflow Automation
        System.out.println("Test 7: Workflow Automation");
        System.out.println("───────────────────────────────────────────────────");
        String result7 = ai.predictCategory(
            "Automatisation workflows avec n8n",
            "Je vais automatiser vos processus avec n8n et Zapier"
        );
        System.out.println("✓ Résultat: " + result7);
        System.out.println("✓ Attendu: Workflow Automation");
        System.out.println("✓ Correct: " + (result7 != null && result7.contains("Automation")) + "\n");

        // Test 8: Texte ambigu (aucune prédiction)
        System.out.println("Test 8: Texte Ambigu");
        System.out.println("───────────────────────────────────────────────────");
        String result8 = ai.predictCategory(
            "Service professionnel",
            "Je vais vous aider avec votre projet"
        );
        System.out.println("✓ Résultat: " + result8);
        System.out.println("✓ Attendu: null (aucun mot-clé)");
        System.out.println("✓ Correct: " + (result8 == null) + "\n");

        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  ✅ TESTS TERMINÉS!");
        System.out.println("═══════════════════════════════════════════════════");
    }
}

