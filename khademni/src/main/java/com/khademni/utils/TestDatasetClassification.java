package com.khademni.utils;

/**
 * 🧪 TEST DE CLASSIFICATION - 50 EXEMPLES DU DATASET RÉEL
 *
 * Ce test valide que FreeAIService classifie correctement
 * les 50 exemples réels du dataset fourni
 */
public class TestDatasetClassification {

    public static void main(String[] args) {
        System.out.println("═══════════════════════════════════════════════════════════════════");
        System.out.println("  🧪 TEST DE CLASSIFICATION - 50 EXEMPLES DU DATASET");
        System.out.println("═══════════════════════════════════════════════════════════════════\n");

        FreeAIService ai = new FreeAIService();

        // Dataset réel: 50 exemples avec leur catégorie attendue
        String[][] dataset = {
            // DESIGN (18 exemples - 36%)
            {"Logo design", "Design"},
            {"Flyer design", "Design"},
            {"Product photography", "Design"},
            {"Brand identity", "Design"},
            {"UI/UX design", "Design"},
            {"Business card design", "Design"},
            {"Banner design", "Design"},
            {"T-shirt design", "Design"},
            {"Illustration", "Design"},
            {"Packaging design", "Design"},
            {"Website redesign", "Design"},

            // DEVELOPMENT (15 exemples - 30%)
            {"React website", "Development"},
            {"WordPress site", "Development"},
            {"Mobile app development", "Development"},
            {"E-commerce website", "Development"},
            {"JavaScript development", "Development"},
            {"Python programming", "Development"},
            {"Node.js API", "Development"},
            {"Front-end web development", "Development"},
            {"React Native app", "Development"},
            {"Backend development", "Development"},
            {"Full-stack development", "Development"},

            // MARKETING (9 exemples - 18%)
            {"SEO optimization", "Marketing"},
            {"Social media strategy", "Marketing"},
            {"Email marketing", "Marketing"},
            {"Content marketing", "Marketing"},
            {"Social media ads", "Marketing"},
            {"Google Ads campaign", "Marketing"},
            {"Facebook marketing", "Marketing"},
            {"Instagram strategy", "Marketing"},
            {"Influencer marketing", "Marketing"},

            // WRITING (7 exemples - 14%)
            {"Blog writing", "Writing"},
            {"Technical writing", "Writing"},
            {"Copywriting", "Writing"},
            {"Ghostwriting", "Writing"},
            {"Script writing", "Writing"},
            {"Technical documentation", "Writing"},
            {"Article writing", "Writing"},
            {"Proofreading", "Writing"},
            {"Novel writing", "Writing"},

            // VIDEO (9 exemples - 18%)
            {"Video editing", "Video"},
            {"Animation", "Video"},
            {"Explainer video", "Video"},
            {"Video production", "Video"},
            {"Motion graphics", "Video"},
            {"Promo video", "Video"},
            {"Video ads", "Video"},
            {"Video shooting", "Video"},
            {"Short film", "Video"}
        };

        int correct = 0;
        int total = dataset.length;

        System.out.println("Testing " + total + " examples...\n");

        for (String[] example : dataset) {
            String title = example[0];
            String expectedCategory = example[1];

            String predicted = ai.predictCategory(title, "");

            boolean isCorrect = predicted != null && predicted.equalsIgnoreCase(expectedCategory);

            if (isCorrect) {
                correct++;
                System.out.println("✅ CORRECT: \"" + title + "\" → " + predicted);
            } else {
                System.out.println("❌ ERREUR:  \"" + title + "\" → " + predicted + " (attendu: " + expectedCategory + ")");
            }
        }

        System.out.println("\n═══════════════════════════════════════════════════════════════════");
        System.out.println("  📊 RÉSULTATS");
        System.out.println("═══════════════════════════════════════════════════════════════════");
        System.out.println("Total testé:  " + total + " exemples");
        System.out.println("Correct:      " + correct + " (" + (correct * 100 / total) + "%)");
        System.out.println("Erreurs:      " + (total - correct) + " (" + ((total - correct) * 100 / total) + "%)");
        System.out.println("═══════════════════════════════════════════════════════════════════");

        if (correct == total) {
            System.out.println("\n🎉 PARFAIT! Classification à 100%!");
        } else if (correct >= total * 0.9) {
            System.out.println("\n✅ EXCELLENT! Classification > 90%");
        } else if (correct >= total * 0.8) {
            System.out.println("\n✅ BON! Classification > 80%");
        } else {
            System.out.println("\n⚠️  À améliorer (< 80%)");
        }

        System.out.println("\n═══════════════════════════════════════════════════════════════════\n");
    }
}

