package com.khademni.utils;

import java.util.*;

/**
 * 🧪 Test de classification avec 200 exemples réels du dataset
 *
 * Ce test vérifie la précision de FreeAIService sur 200 exemples
 * répartis en 5 catégories principales.
 */
public class TestDataset200Classification {

    public static void main(String[] args) {
        System.out.println("═══════════════════════════════════════════════════════════════════");
        System.out.println("  🧪 TEST DE CLASSIFICATION - 200 EXEMPLES DU DATASET");
        System.out.println("═══════════════════════════════════════════════════════════════════\n");

        FreeAIService aiService = new FreeAIService();

        // Compteurs
        int total = 0;
        int correct = 0;
        int incorrect = 0;

        Map<String, Integer> correctByCategory = new HashMap<>();
        Map<String, Integer> totalByCategory = new HashMap<>();

        String[] categories = {"Design", "Development", "Marketing", "Writing", "Video"};
        for (String cat : categories) {
            correctByCategory.put(cat, 0);
            totalByCategory.put(cat, 0);
        }

        // ═══════════════════════════════════════════════════════════════
        // DESIGN (70 exemples)
        // ═══════════════════════════════════════════════════════════════

        String[][] designExamples = {
            // Logo & Branding (20)
            {"Professional Logo Design", "Design"},
            {"Modern Minimalist Logo", "Design"},
            {"Brand Identity Package", "Design"},
            {"Vintage Logo Design", "Design"},
            {"3D Logo Animation", "Design"},
            {"Mascot Logo Design", "Design"},
            {"Luxury Brand Identity", "Design"},
            {"Signature Logo", "Design"},
            {"Geometric Logo", "Design"},
            {"Monogram Logo", "Design"},
            {"Badge Logo Design", "Design"},
            {"Abstract Logo", "Design"},
            {"Typography Logo", "Design"},
            {"Sports Team Logo", "Design"},
            {"Tech Startup Logo", "Design"},
            {"Restaurant Logo", "Design"},
            {"Real Estate Logo", "Design"},
            {"Fashion Brand Logo", "Design"},
            {"Medical Logo Design", "Design"},
            {"App Icon Design", "Design"},

            // UI/UX (15)
            {"Website UI Design", "Design"},
            {"Mobile App UI Design", "Design"},
            {"Dashboard Design", "Design"},
            {"Landing Page Design", "Design"},
            {"E-commerce Website Design", "Design"},
            {"SaaS Dashboard Design", "Design"},
            {"Mobile Game UI", "Design"},
            {"Wireframe Design", "Design"},
            {"Prototype Design", "Design"},
            {"UX Research", "Design"},
            {"Responsive Web Design", "Design"},
            {"Portfolio Website Design", "Design"},
            {"Admin Panel Design", "Design"},
            {"User Flow Design", "Design"},
            {"Design System", "Design"},

            // Print & Marketing (15)
            {"Flyer Design", "Design"},
            {"Business Card Design", "Design"},
            {"Brochure Design", "Design"},
            {"Poster Design", "Design"},
            {"Menu Design", "Design"},
            {"Banner Ad Design", "Design"},
            {"Social Media Banner", "Design"},
            {"Instagram Post Design", "Design"},
            {"Facebook Ad Design", "Design"},
            {"YouTube Thumbnail", "Design"},
            {"Packaging Design", "Design"},
            {"Label Design", "Design"},
            {"Book Cover Design", "Design"},
            {"Catalog Design", "Design"},
            {"Magazine Layout", "Design"},

            // Illustration (10)
            {"Custom Illustration", "Design"},
            {"Character Design", "Design"},
            {"Infographic Design", "Design"},
            {"Icon Set Design", "Design"},
            {"Vector Illustration", "Design"},
            {"Portrait Illustration", "Design"},
            {"Comic Style Illustration", "Design"},
            {"Sticker Design", "Design"},
            {"Pattern Design", "Design"},
            {"Children Book Illustration", "Design"},

            // Other (10)
            {"T-shirt Design", "Design"},
            {"Twitch Overlay Design", "Design"},
            {"Discord Server Design", "Design"},
            {"Email Newsletter Design", "Design"},
            {"PowerPoint Template", "Design"},
            {"Resume Design", "Design"},
            {"Mug Design", "Design"},
            {"Vehicle Wrap Design", "Design"},
            {"Signage Design", "Design"},
            {"Trade Show Booth Design", "Design"}
        };

        // ═══════════════════════════════════════════════════════════════
        // DEVELOPMENT (60 exemples)
        // ═══════════════════════════════════════════════════════════════

        String[][] devExamples = {
            // Web (25)
            {"React Website Development", "Development"},
            {"WordPress Website", "Development"},
            {"E-commerce Store", "Development"},
            {"Landing Page Development", "Development"},
            {"Portfolio Website", "Development"},
            {"Business Website", "Development"},
            {"Blog Website", "Development"},
            {"Next.js Application", "Development"},
            {"Vue.js Website", "Development"},
            {"Angular Application", "Development"},
            {"HTML CSS Website", "Development"},
            {"Laravel Website", "Development"},
            {"Django Web App", "Development"},
            {"Shopify Store", "Development"},
            {"WooCommerce Store", "Development"},
            {"Restaurant Website", "Development"},
            {"Real Estate Website", "Development"},
            {"Booking Website", "Development"},
            {"Membership Website", "Development"},
            {"Custom CMS", "Development"},
            {"Static Website", "Development"},
            {"PHP Website", "Development"},
            {"Node.js Backend", "Development"},
            {"API Development", "Development"},
            {"Website Redesign", "Development"},

            // Mobile (15)
            {"React Native App", "Development"},
            {"Flutter App Development", "Development"},
            {"iOS App Development", "Development"},
            {"Android App Development", "Development"},
            {"Mobile Game Development", "Development"},
            {"Hybrid App Development", "Development"},
            {"Food Delivery App", "Development"},
            {"Taxi Booking App", "Development"},
            {"E-commerce Mobile App", "Development"},
            {"Social Media App", "Development"},
            {"Fitness Tracking App", "Development"},
            {"Dating App", "Development"},
            {"Chat Application", "Development"},
            {"Educational App", "Development"},
            {"Music Streaming App", "Development"},

            // Backend (10)
            {"Database Design", "Development"},
            {"REST API Development", "Development"},
            {"GraphQL API", "Development"},
            {"Python Script", "Development"},
            {"Node.js Microservices", "Development"},
            {"Firebase Integration", "Development"},
            {"MongoDB Setup", "Development"},
            {"PostgreSQL Database", "Development"},
            {"MySQL Database", "Development"},
            {"Server Configuration", "Development"},

            // Other (10)
            {"Chrome Extension", "Development"},
            {"Browser Plugin", "Development"},
            {"WordPress Plugin", "Development"},
            {"Shopify App", "Development"},
            {"Discord Bot", "Development"},
            {"Telegram Bot", "Development"},
            {"Desktop Application", "Development"},
            {"Web Scraping Script", "Development"},
            {"Automation Tool", "Development"},
            {"Code Review", "Development"}
        };

        // ═══════════════════════════════════════════════════════════════
        // MARKETING (30 exemples)
        // ═══════════════════════════════════════════════════════════════

        String[][] marketingExamples = {
            // SEO (10)
            {"SEO Optimization", "Marketing"},
            {"Keyword Research", "Marketing"},
            {"Local SEO", "Marketing"},
            {"Backlink Building", "Marketing"},
            {"Technical SEO Audit", "Marketing"},
            {"On-Page SEO", "Marketing"},
            {"SEO Content Writing", "Marketing"},
            {"Google My Business", "Marketing"},
            {"Competitor Analysis", "Marketing"},
            {"SEO Strategy", "Marketing"},

            // Social Media (10)
            {"Social Media Strategy", "Marketing"},
            {"Facebook Ads Campaign", "Marketing"},
            {"Instagram Marketing", "Marketing"},
            {"TikTok Marketing", "Marketing"},
            {"LinkedIn Marketing", "Marketing"},
            {"Twitter Marketing", "Marketing"},
            {"Social Media Content", "Marketing"},
            {"Community Management", "Marketing"},
            {"Influencer Marketing", "Marketing"},
            {"Social Media Audit", "Marketing"},

            // Paid Ads (5)
            {"Google Ads Campaign", "Marketing"},
            {"PPC Management", "Marketing"},
            {"Display Ads", "Marketing"},
            {"Retargeting Campaign", "Marketing"},
            {"YouTube Ads", "Marketing"},

            // Email & Content (5)
            {"Email Marketing Campaign", "Marketing"},
            {"Newsletter Design", "Marketing"},
            {"Email Automation", "Marketing"},
            {"Content Marketing Strategy", "Marketing"},
            {"Marketing Analytics", "Marketing"}
        };

        // ═══════════════════════════════════════════════════════════════
        // WRITING (20 exemples)
        // ═══════════════════════════════════════════════════════════════

        String[][] writingExamples = {
            {"Blog Post Writing", "Writing"},
            {"Article Writing", "Writing"},
            {"Copywriting Services", "Writing"},
            {"Technical Writing", "Writing"},
            {"Product Description", "Writing"},
            {"Ghostwriting", "Writing"},
            {"Script Writing", "Writing"},
            {"Website Content", "Writing"},
            {"Press Release", "Writing"},
            {"White Paper", "Writing"},
            {"Case Study", "Writing"},
            {"Proofreading", "Writing"},
            {"Resume Writing", "Writing"},
            {"Business Plan", "Writing"},
            {"Grant Proposal", "Writing"},
            {"Social Media Copy", "Writing"},
            {"Email Copy", "Writing"},
            {"Speech Writing", "Writing"},
            {"Poetry Writing", "Writing"},
            {"Translation Services", "Writing"}
        };

        // ═══════════════════════════════════════════════════════════════
        // VIDEO (20 exemples)
        // ═══════════════════════════════════════════════════════════════

        String[][] videoExamples = {
            {"Video Editing", "Video"},
            {"YouTube Video Editing", "Video"},
            {"Explainer Video", "Video"},
            {"Promo Video", "Video"},
            {"Whiteboard Animation", "Video"},
            {"Motion Graphics", "Video"},
            {"2D Animation", "Video"},
            {"3D Animation", "Video"},
            {"Video Ads", "Video"},
            {"Short Film Production", "Video"},
            {"Music Video", "Video"},
            {"Slideshow Video", "Video"},
            {"Corporate Video", "Video"},
            {"Video Color Grading", "Video"},
            {"Subtitle Creation", "Video"},
            {"Green Screen Editing", "Video"},
            {"Drone Video Editing", "Video"},
            {"Wedding Video Editing", "Video"},
            {"Instagram Reels", "Video"},
            {"TikTok Video Editing", "Video"}
        };

        // ═══════════════════════════════════════════════════════════════
        // EXÉCUTION DES TESTS
        // ═══════════════════════════════════════════════════════════════

        System.out.println("🎨 Testing DESIGN (70 examples)...\n");
        int[] designResults = testCategory(aiService, designExamples, "Design");
        total += designResults[0];
        correct += designResults[1];
        incorrect += designResults[2];

        System.out.println("\n💻 Testing DEVELOPMENT (60 examples)...\n");
        int[] devResults = testCategory(aiService, devExamples, "Development");
        total += devResults[0];
        correct += devResults[1];
        incorrect += devResults[2];

        System.out.println("\n📈 Testing MARKETING (30 examples)...\n");
        int[] marketingResults = testCategory(aiService, marketingExamples, "Marketing");
        total += marketingResults[0];
        correct += marketingResults[1];
        incorrect += marketingResults[2];

        System.out.println("\n✍️ Testing WRITING (20 examples)...\n");
        int[] writingResults = testCategory(aiService, writingExamples, "Writing");
        total += writingResults[0];
        correct += writingResults[1];
        incorrect += writingResults[2];

        System.out.println("\n🎥 Testing VIDEO (20 examples)...\n");
        int[] videoResults = testCategory(aiService, videoExamples, "Video");
        total += videoResults[0];
        correct += videoResults[1];
        incorrect += videoResults[2];

        // ═══════════════════════════════════════════════════════════════
        // RÉSULTATS FINAUX
        // ═══════════════════════════════════════════════════════════════

        System.out.println("\n═══════════════════════════════════════════════════════════════════");
        System.out.println("  📊 RÉSULTATS FINAUX");
        System.out.println("═══════════════════════════════════════════════════════════════════\n");

        double accuracy = (correct * 100.0) / total;

        System.out.println("Total exemples testés:  " + total);
        System.out.println("✅ Corrects:             " + correct + " (" + String.format("%.1f%%", accuracy) + ")");
        System.out.println("❌ Incorrects:           " + incorrect + " (" + String.format("%.1f%%", (incorrect * 100.0) / total) + ")");

        System.out.println("\n📊 Précision par catégorie:");
        System.out.println("─────────────────────────────────────────────────────────────────");

        printCategoryStats("Design", designResults);
        printCategoryStats("Development", devResults);
        printCategoryStats("Marketing", marketingResults);
        printCategoryStats("Writing", writingResults);
        printCategoryStats("Video", videoResults);

        System.out.println("\n═══════════════════════════════════════════════════════════════════");

        if (accuracy >= 95) {
            System.out.println("  🎉 EXCELLENT! Précision supérieure à 95%");
        } else if (accuracy >= 90) {
            System.out.println("  ✅ TRÈS BON! Précision supérieure à 90%");
        } else if (accuracy >= 80) {
            System.out.println("  👍 BON! Précision supérieure à 80%");
        } else {
            System.out.println("  ⚠️ Précision inférieure à 80%, amélioration nécessaire");
        }

        System.out.println("═══════════════════════════════════════════════════════════════════\n");
    }

    private static int[] testCategory(FreeAIService aiService, String[][] examples, String categoryName) {
        int total = 0;
        int correct = 0;
        int incorrect = 0;

        List<String> errors = new ArrayList<>();

        for (String[] example : examples) {
            String title = example[0];
            String expectedCategory = example[1];

            // predictCategory nécessite title ET description
            String predicted = aiService.predictCategory(title, "");
            total++;

            if (predicted.equalsIgnoreCase(expectedCategory)) {
                correct++;
                System.out.print("✅ ");
            } else {
                incorrect++;
                errors.add(title + " → " + predicted + " (attendu: " + expectedCategory + ")");
                System.out.print("❌ ");
            }

            System.out.println(title);
        }

        double accuracy = (correct * 100.0) / total;
        System.out.println("\n  📊 " + categoryName + ": " + correct + "/" + total +
                         " (" + String.format("%.1f%%", accuracy) + ")");

        if (!errors.isEmpty() && errors.size() <= 5) {
            System.out.println("\n  ❌ Erreurs:");
            for (String error : errors) {
                System.out.println("     • " + error);
            }
        }

        return new int[]{total, correct, incorrect};
    }

    private static void printCategoryStats(String category, int[] results) {
        int total = results[0];
        int correct = results[1];
        double accuracy = (correct * 100.0) / total;

        System.out.printf("  %-15s %3d/%3d  (%5.1f%%)  ", category, correct, total, accuracy);

        // Barre de progression
        int bars = (int)(accuracy / 5);
        System.out.print("[");
        for (int i = 0; i < 20; i++) {
            if (i < bars) {
                System.out.print("█");
            } else {
                System.out.print("░");
            }
        }
        System.out.println("]");
    }
}


