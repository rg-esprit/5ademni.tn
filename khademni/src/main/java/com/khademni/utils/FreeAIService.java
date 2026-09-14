package com.khademni.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Free AI Service - Alternative gratuite à OpenAI
 * Utilise l'API Hugging Face (GRATUITE et sans quota)
 */
public class FreeAIService {

    // API Hugging Face GRATUITE - pas besoin de carte bancaire
    private static final String HUGGING_FACE_API = "https://api-inference.huggingface.co/models/mistralai/Mistral-7B-Instruct-v0.2";
    private static final String API_KEY = loadApiKey();

    private static String loadApiKey() {
        String key = System.getenv("HUGGINGFACE_API_KEY");
        if (key != null && !key.isBlank()) {
            return key.trim();
        }
        try (java.io.InputStream input = FreeAIService.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input != null) {
                java.util.Properties prop = new java.util.Properties();
                prop.load(input);
                String val = prop.getProperty("HUGGINGFACE_API_KEY", "").trim();
                if (!val.isEmpty() && !val.contains("your_") && !val.contains("xxxx")) {
                    return val;
                }
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    /**
     * Génère une description pour une catégorie
     */
    public String generateCategoryDescription(String categoryName) {
        System.out.println("🚀 Génération avec AI GRATUITE (Hugging Face)...");
        System.out.println("   Catégorie: " + categoryName);

        String prompt = String.format(
            "Generate a professional and concise description (2-3 sentences) for a freelance service category called '%s'. " +
            "Focus on what services this category includes and who might need them.",
            categoryName
        );

        // Fallback: Si API échoue, génération locale
        try {
            return generateWithHuggingFace(prompt);
        } catch (Exception e) {
            System.out.println("⚠️  API Hugging Face non disponible, utilisation génération locale");
            return generateLocalCategoryDescription(categoryName);
        }
    }

    /**
     * Génère une description pour un gig
     */
    public String generateGigDescription(String gigTitle, String categoryName) {
        System.out.println("🚀 Génération avec AI GRATUITE (Hugging Face)...");
        System.out.println("   Titre: " + gigTitle);
        System.out.println("   Catégorie: " + categoryName);

        String prompt = String.format(
            "Generate a professional gig description (3-4 sentences) for a freelance service titled '%s' in the category '%s'. " +
            "Include what the seller will deliver, the quality, and why clients should choose this service.",
            gigTitle, categoryName
        );

        // Fallback: Si API échoue, génération locale
        try {
            return generateWithHuggingFace(prompt);
        } catch (Exception e) {
            System.out.println("⚠️  API Hugging Face non disponible, utilisation génération locale");
            return generateLocalGigDescription(gigTitle, categoryName);
        }
    }

    /**
     * Appelle l'API Hugging Face (GRATUITE)
     */
    private String generateWithHuggingFace(String prompt) throws Exception {
        System.out.println("🤖 Appel API Hugging Face (gratuite)...");

        URL url = new URL(HUGGING_FACE_API);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(30000);

        // Request body
        String jsonRequest = String.format(
            "{\"inputs\": \"%s\", \"parameters\": {\"max_new_tokens\": 150, \"temperature\": 0.7}}",
            prompt.replace("\"", "\\\"")
        );

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonRequest.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();
        System.out.println("📥 Response code: " + responseCode);

        if (responseCode == 200) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            // Parse response (format simple)
            String fullResponse = response.toString();

            // Extract generated text (basic parsing)
            if (fullResponse.contains("generated_text")) {
                int start = fullResponse.indexOf("\"generated_text\":\"") + 18;
                int end = fullResponse.indexOf("\"", start);
                if (start > 18 && end > start) {
                    String generated = fullResponse.substring(start, end);
                    System.out.println("✅ Description générée avec succès!");
                    return generated;
                }
            }

            throw new Exception("Format de réponse inattendu");
        } else {
            throw new Exception("Erreur API: " + responseCode);
        }
    }

    /**
     * Génération locale SANS API (100% gratuit et hors ligne)
     * Basée sur des templates intelligents
     */
    private String generateLocalCategoryDescription(String categoryName) {
        System.out.println("💡 Génération locale pour catégorie: " + categoryName);

        String nameLower = categoryName.toLowerCase();

        // Templates basés sur mots-clés
        if (nameLower.contains("web") || nameLower.contains("site") || nameLower.contains("development")) {
            return "This category includes professional web development services such as creating responsive websites, " +
                   "web applications, e-commerce platforms, and custom web solutions. Perfect for businesses and individuals " +
                   "looking to establish or enhance their online presence with modern, high-quality web technologies.";
        }

        if (nameLower.contains("design") || nameLower.contains("graphic") || nameLower.contains("logo")) {
            return "This category offers creative design services including logo design, branding, graphic design, " +
                   "UI/UX design, and visual identity creation. Ideal for businesses seeking professional visual content " +
                   "and memorable brand identities that stand out in the marketplace.";
        }

        if (nameLower.contains("writing") || nameLower.contains("content") || nameLower.contains("copywriting")) {
            return "This category provides professional writing and content creation services including copywriting, " +
                   "blog posts, articles, product descriptions, and content marketing. Essential for businesses looking " +
                   "to engage their audience with compelling and SEO-optimized written content.";
        }

        if (nameLower.contains("video") || nameLower.contains("animation") || nameLower.contains("editing")) {
            return "This category encompasses video production and editing services including video editing, animation, " +
                   "motion graphics, and visual effects. Perfect for content creators and businesses wanting to create " +
                   "engaging video content for social media, marketing, or entertainment purposes.";
        }

        if (nameLower.contains("marketing") || nameLower.contains("seo") || nameLower.contains("social")) {
            return "This category includes digital marketing services such as SEO optimization, social media marketing, " +
                   "content marketing, and online advertising campaigns. Ideal for businesses aiming to increase their " +
                   "online visibility, attract more customers, and grow their digital presence effectively.";
        }

        if (nameLower.contains("mobile") || nameLower.contains("app") || nameLower.contains("android") || nameLower.contains("ios")) {
            return "This category offers mobile application development services for iOS, Android, and cross-platform apps. " +
                   "Perfect for businesses and entrepreneurs looking to create user-friendly mobile applications that deliver " +
                   "exceptional user experiences and meet modern mobile standards.";
        }

        if (nameLower.contains("data") || nameLower.contains("analysis") || nameLower.contains("analytics")) {
            return "This category provides data analysis and business intelligence services including data visualization, " +
                   "statistical analysis, reporting, and insights generation. Essential for businesses seeking to make " +
                   "data-driven decisions and understand their business metrics better.";
        }

        if (nameLower.contains("automation") || nameLower.contains("workflow") || nameLower.contains("n8n")) {
            return "This category includes workflow automation and process optimization services using tools like n8n, " +
                   "Zapier, and custom scripts. Perfect for businesses looking to automate repetitive tasks, improve " +
                   "efficiency, and streamline their operational workflows.";
        }

        // Template générique
        return String.format(
            "This category offers professional %s services delivered by experienced freelancers. " +
            "Whether you're a business or individual, you'll find quality services tailored to your specific needs. " +
            "Browse through available gigs to find the perfect match for your project requirements.",
            categoryName
        );
    }

    /**
     * Génération locale pour gig
     */
    private String generateLocalGigDescription(String gigTitle, String categoryName) {
        System.out.println("💡 Génération locale pour gig: " + gigTitle);

        String titleLower = gigTitle.toLowerCase();

        // Templates basés sur le titre
        if (titleLower.contains("website") || titleLower.contains("site web")) {
            return "I will create a professional, responsive website tailored to your specific requirements. " +
                   "Using modern technologies and best practices, I'll deliver a high-quality website with clean code, " +
                   "fast loading times, and excellent user experience. The final product will be fully functional, " +
                   "mobile-friendly, and ready to launch. Perfect for businesses, portfolios, or personal projects.";
        }

        if (titleLower.contains("logo") || titleLower.contains("brand")) {
            return "I will design a unique and memorable logo that perfectly represents your brand identity. " +
                   "You'll receive multiple concepts to choose from, unlimited revisions until you're satisfied, " +
                   "and final files in all formats (PNG, SVG, PDF). The design will be modern, professional, " +
                   "and suitable for all your branding needs including business cards, websites, and social media.";
        }

        if (titleLower.contains("app") || titleLower.contains("mobile")) {
            return "I will develop a professional mobile application with intuitive design and smooth functionality. " +
                   "The app will be built using industry-standard technologies, ensuring optimal performance and " +
                   "user experience. You'll receive a fully tested application with documentation, ready for deployment " +
                   "to app stores. Includes post-delivery support and bug fixes.";
        }

        if (titleLower.contains("seo") || titleLower.contains("optimize")) {
            return "I will optimize your website or content for search engines to improve your online visibility. " +
                   "Using proven SEO strategies and white-hat techniques, I'll help you rank higher in search results. " +
                   "Services include keyword research, on-page optimization, meta tags, and detailed reports. " +
                   "Expect measurable improvements in your search engine rankings and organic traffic.";
        }

        if (titleLower.contains("write") || titleLower.contains("content") || titleLower.contains("article")) {
            return "I will write high-quality, engaging content perfectly tailored to your needs and target audience. " +
                   "Each piece is thoroughly researched, SEO-optimized, and crafted to deliver your message effectively. " +
                   "You'll receive original, plagiarism-free content with proper grammar and structure. " +
                   "Unlimited revisions included to ensure complete satisfaction.";
        }

        if (titleLower.contains("video") || titleLower.contains("edit")) {
            return "I will edit your video footage into a polished, professional final product. " +
                   "Services include cutting, transitions, color correction, audio enhancement, and special effects. " +
                   "Using professional editing software, I'll create engaging content perfect for YouTube, social media, " +
                   "or marketing campaigns. Fast turnaround with revisions included.";
        }

        if (titleLower.contains("data") || titleLower.contains("analysis")) {
            return "I will analyze your data and provide actionable insights through comprehensive reports and visualizations. " +
                   "Using advanced analytics tools and statistical methods, I'll help you understand your data better " +
                   "and make informed business decisions. Deliverables include clear charts, detailed reports, " +
                   "and recommendations based on the findings.";
        }

        if (titleLower.contains("automat") || titleLower.contains("workflow")) {
            return "I will automate your repetitive tasks and workflows to save you time and reduce errors. " +
                   "Using modern automation tools and custom scripts, I'll create efficient solutions that work 24/7. " +
                   "You'll receive fully documented automation workflows that are easy to maintain and modify. " +
                   "Increase productivity and focus on what matters most to your business.";
        }

        // Template générique basé sur le titre et la catégorie
        return String.format(
            "I will deliver high-quality %s services with professionalism and attention to detail. " +
            "As an experienced freelancer in %s, I understand client expectations and always strive to exceed them. " +
            "You'll receive excellent communication throughout the project, timely delivery, and the flexibility " +
            "to request revisions until you're completely satisfied. Let's work together to bring your vision to life!",
            categoryName.toLowerCase(),
            categoryName.toLowerCase()
        );
    }

    /**
     * 🎯 CLASSIFICATION AUTOMATIQUE DE CATÉGORIE
     * Prédit automatiquement la catégorie basée sur titre + description
     * Utilise 50 exemples réels de dataset pour meilleure précision
     *
     * @param title Titre du gig
     * @param description Description du gig
     * @return Nom de la catégorie prédite
     */
    public String predictCategory(String title, String description) {
        System.out.println("🤖 Classification automatique de catégorie...");
        System.out.println("   Titre: " + title);

        // Combine titre + description pour analyse
        String fullText = (title + " " + description).toLowerCase();

        // 🎯 SCORES PAR CATÉGORIE (basé sur dataset réel de 50 exemples)

        // DESIGN (18 exemples dans dataset)
        // Logo, Flyer, Product photo, Brand identity, UI/UX, Business card, Banner, T-shirt, Illustration, Packaging, Website redesign
        int designScore = countKeywords(fullText,
            // Termes généraux
            "design", "graphic", "visual", "créatif", "creative",
            // Spécifique logo/branding
            "logo", "brand", "branding", "identity", "brand identity", "charte graphique",
            // Spécifique print
            "flyer", "business card", "banner", "packaging", "t-shirt", "t shirt", "tshirt",
            // Spécifique digital
            "ui", "ux", "ui/ux", "interface", "wireframe", "mockup", "prototype",
            // Outils
            "photoshop", "illustrator", "figma", "sketch", "adobe", "canva",
            // Autres
            "illustration", "product photo", "photography", "redesign"
        );

        // DEVELOPMENT (15 exemples dans dataset)
        // React, WordPress, Mobile app, E-commerce, JavaScript, Python, Node.js, React Native, Backend, Full-stack, Front-end
        int developmentScore = countKeywords(fullText,
            // Générique
            "development", "dev", "développement", "programming", "code", "coding",
            // Web
            "web", "site", "website", "front-end", "frontend", "back-end", "backend", "fullstack", "full-stack",
            // Frameworks/Tech web
            "react", "angular", "vue", "next", "nuxt", "svelte", "html", "css", "javascript", "typescript", "js", "ts",
            "wordpress", "drupal", "joomla", "wix", "shopify",
            // E-commerce
            "e-commerce", "ecommerce", "boutique", "magasin", "shop", "store",
            // Backend/API
            "node", "nodejs", "node.js", "api", "rest", "graphql", "express",
            "php", "laravel", "symfony", "django", "flask", "spring",
            // Mobile
            "mobile", "app", "application", "android", "ios", "react native", "flutter", "swift", "kotlin",
            // Langages
            "python", "java", "c#", "ruby", "go", "rust"
        );

        // MARKETING (9 exemples dans dataset)
        // SEO, Social media strategy, Email marketing, Content marketing, Social media ads, Google Ads, Facebook, Instagram, Influencer
        int marketingScore = countKeywords(fullText,
            // Générique
            "marketing", "promotion", "publicité", "advertising", "campaign", "campagne",
            // SEO
            "seo", "optimization", "optimisation", "référencement", "google ranking",
            // Social Media
            "social media", "réseaux sociaux", "facebook", "instagram", "twitter", "linkedin", "tiktok",
            "social strategy", "community", "engagement",
            // Ads
            "ads", "google ads", "facebook ads", "instagram ads", "adwords", "sponsored",
            // Email
            "email", "newsletter", "mailing", "emailing", "email campaign",
            // Autre
            "influencer", "content marketing", "brand awareness", "traffic"
        );

        // WRITING (7 exemples dans dataset)
        // Blog, Technical, Copywriting, Ghostwriting, Script, Technical documentation, Article, Proofreading, Novel
        int writingScore = countKeywords(fullText,
            // Générique
            "writing", "write", "content", "rédaction", "écriture", "texte", "writer", "rédacteur",
            // Types
            "blog", "article", "copywriting", "copy", "ghostwriting", "script", "screenplay",
            "technical writing", "technical documentation", "documentation", "manuel",
            "proofreading", "editing", "correction", "relecture",
            "novel", "book", "ebook", "fiction", "story",
            // SEO/Marketing
            "seo content", "content marketing", "web content"
        );

        // VIDEO (9 exemples dans dataset)
        // Video editing, Animation, Explainer video, Video production, Promo video, Video ads, Short film, Video shooting, Motion graphics
        int videoScore = countKeywords(fullText,
            // Générique
            "video", "vidéo", "film", "movie", "cinéma", "cinema",
            // Editing
            "editing", "montage", "edit", "cut", "trim",
            // Production
            "production", "shooting", "filming", "tournage", "camera",
            // Animation
            "animation", "animated", "motion", "motion graphics", "after effects",
            "explainer", "promo", "promotional",
            // Types
            "ads", "advertising video", "short film", "youtube", "social video",
            // Outils
            "premiere", "final cut", "davinci", "ae", "vegas"
        );

        // Catégories et leurs scores
        String[] categories = {
            "Design",           // 18 exemples
            "Development",      // 15 exemples
            "Marketing",        // 9 exemples
            "Writing",          // 7 exemples
            "Video"             // 9 exemples
        };

        int[] scores = {
            designScore,
            developmentScore,
            marketingScore,
            writingScore,
            videoScore
        };

        // Trouve la catégorie avec le score le plus élevé
        int maxScore = 0;
        String predictedCategory = null;

        for (int i = 0; i < scores.length; i++) {
            System.out.println("   📊 " + categories[i] + ": " + scores[i] + " points");
            if (scores[i] > maxScore) {
                maxScore = scores[i];
                predictedCategory = categories[i];
            }
        }

        // Si aucun mot-clé trouvé, retourne null
        if (maxScore == 0) {
            System.out.println("⚠️  Aucun mot-clé trouvé, pas de prédiction");
            return null;
        }

        System.out.println("🎯 Catégorie prédite: " + predictedCategory + " (score: " + maxScore + ")");
        return predictedCategory;
    }

    /**
     * Compte le nombre de mots-clés trouvés dans le texte
     *
     * @param text Texte à analyser
     * @param keywords Mots-clés à rechercher
     * @return Score (nombre de mots-clés trouvés × 2)
     */
    private int countKeywords(String text, String... keywords) {
        int count = 0;
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                count += 2; // Poids de 2 par mot-clé trouvé
                System.out.println("   ✓ Mot-clé trouvé: " + keyword);
            }
        }
        return count;
    }
}

