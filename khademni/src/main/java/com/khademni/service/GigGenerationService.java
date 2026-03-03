package com.khademni.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI-powered Gig Generation Engine using the DeepSeek API.
 *
 * Takes a simple user prompt (e.g. "I'm good at making logos") and generates
 * a fully structured, marketplace-ready gig including:
 *   - Professional title
 *   - Detailed SEO-friendly description
 *   - Suggested price (TND)
 *   - Recommended category
 *   - Delivery days estimate
 *
 * Falls back to intelligent local template generation when the API is unavailable.
 */
public class GigGenerationService {

    // ======================== Configuration ========================

    private static final String DEEPSEEK_API_URL = "https://api.deepseek.com/v1/chat/completions";
    private static final String DEEPSEEK_MODEL   = "deepseek-chat";
    private static final String API_KEY          = "your_deepseek_api_key_here";

    private static final int CONNECT_TIMEOUT = 10000;
    private static final int READ_TIMEOUT    = 30000;

    // ======================== Result DTO ========================

    /**
     * Holds the full AI-generated gig structure.
     */
    public static class GeneratedGig {
        private String title;
        private String description;
        private double price;
        private String category;
        private int deliveryDays;

        public GeneratedGig() {}

        public String getTitle()        { return title; }
        public String getDescription()  { return description; }
        public double getPrice()        { return price; }
        public String getCategory()     { return category; }
        public int getDeliveryDays()    { return deliveryDays; }

        public void setTitle(String title)              { this.title = title; }
        public void setDescription(String description)  { this.description = description; }
        public void setPrice(double price)              { this.price = price; }
        public void setCategory(String category)        { this.category = category; }
        public void setDeliveryDays(int deliveryDays)   { this.deliveryDays = deliveryDays; }

        @Override
        public String toString() {
            return "GeneratedGig{title='" + title + "', category='" + category
                    + "', price=" + price + ", deliveryDays=" + deliveryDays + "}";
        }
    }

    // ======================== System Prompt ========================

    private static final String SYSTEM_PROMPT = """
        You are an expert freelance marketplace consultant. Given a simple user prompt
        describing their skills or what they want to offer, generate a complete,
        professional, marketplace-ready gig listing.

        You MUST respond with ONLY a valid JSON object — no markdown fences, no extra text.
        Use this exact schema:

        {
          "title": "A compelling professional gig title (max 80 chars, start with 'I will')",
          "description": "A detailed 4-6 sentence professional description. Include: what you deliver, your expertise, what makes you stand out, and a call to action. Use professional tone.",
          "price": 150.00,
          "category": "One of: Development, Design, Marketing, Writing, Video, Data, Music, Business",
          "deliveryDays": 7
        }

        Rules:
        - Title MUST start with "I will" and be compelling
        - Description must be 4-6 sentences, professional, SEO-friendly
        - Price must be in TND (Tunisian Dinar), minimum 10, realistic for the Tunisian freelance market
        - Category must be one of the 8 listed above
        - Delivery days: realistic estimate (1-30 days)
        - Respond in English
        """;

    // ======================== Public API ========================

    /**
     * Generates a complete gig from a simple user prompt.
     *
     * @param userPrompt e.g. "I'm good at making logos" or "build React websites"
     * @return a fully structured GeneratedGig
     */
    public GeneratedGig generate(String userPrompt) {
        if (userPrompt == null || userPrompt.trim().isEmpty()) {
            throw new IllegalArgumentException("Prompt cannot be empty");
        }

        // Try DeepSeek API first
        try {
            System.out.println("🚀 [GigGenerator] Calling DeepSeek API...");
            System.out.println("   Prompt: " + userPrompt);
            GeneratedGig gig = generateWithDeepSeek(userPrompt.trim());
            System.out.println("✅ [GigGenerator] AI generated: " + gig);
            return gig;
        } catch (Exception e) {
            System.out.println("⚠️  [GigGenerator] DeepSeek API failed: " + e.getMessage());
            System.out.println("🔄 [GigGenerator] Using local template generation...");
        }

        // Fallback: local template-based generation
        return generateLocally(userPrompt.trim());
    }

    // ======================== DeepSeek API ========================

    private GeneratedGig generateWithDeepSeek(String userPrompt) throws Exception {
        String userMessage = "Generate a complete freelance gig listing based on this prompt:\n\n\"" + userPrompt + "\"";

        String requestJson = buildChatRequest(userMessage);
        String responseBody = callAPI(requestJson);
        String content = extractMessageContent(responseBody);

        return parseGeneratedGig(content);
    }

    private String buildChatRequest(String userMessage) {
        String escapedSystem = escapeJson(SYSTEM_PROMPT);
        String escapedUser   = escapeJson(userMessage);

        return "{"
                + "\"model\": \"" + DEEPSEEK_MODEL + "\","
                + "\"messages\": ["
                + "  {\"role\": \"system\", \"content\": \"" + escapedSystem + "\"},"
                + "  {\"role\": \"user\",   \"content\": \"" + escapedUser   + "\"}"
                + "],"
                + "\"temperature\": 0.7,"
                + "\"max_tokens\": 500"
                + "}";
    }

    private String callAPI(String requestJson) throws Exception {
        URL url = new URL(DEEPSEEK_API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        try {
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
            conn.setDoOutput(true);
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(requestJson.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            System.out.println("   [DeepSeek] Response code: " + code);

            BufferedReader reader;
            if (code >= 200 && code < 300) {
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            } else {
                reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
                StringBuilder err = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) err.append(line);
                reader.close();
                throw new Exception("DeepSeek API error " + code + ": " + err);
            }

            StringBuilder body = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) body.append(line);
            reader.close();
            return body.toString();
        } finally {
            conn.disconnect();
        }
    }

    private String extractMessageContent(String responseBody) throws Exception {
        int msgIdx = responseBody.indexOf("\"message\"");
        if (msgIdx == -1) throw new Exception("No 'message' field in response");

        int contentIdx = responseBody.indexOf("\"content\"", msgIdx);
        if (contentIdx == -1) throw new Exception("No 'content' field in message");

        int colonIdx = responseBody.indexOf(':', contentIdx + 9);
        if (colonIdx == -1) throw new Exception("Malformed content field");

        int openQuote = responseBody.indexOf('"', colonIdx + 1);
        if (openQuote == -1) throw new Exception("No opening quote for content value");

        int cursor = openQuote + 1;
        StringBuilder content = new StringBuilder();
        while (cursor < responseBody.length()) {
            char c = responseBody.charAt(cursor);
            if (c == '\\' && cursor + 1 < responseBody.length()) {
                char next = responseBody.charAt(cursor + 1);
                switch (next) {
                    case '"':  content.append('"');  cursor += 2; break;
                    case 'n':  content.append('\n'); cursor += 2; break;
                    case '\\': content.append('\\'); cursor += 2; break;
                    case 't':  content.append('\t'); cursor += 2; break;
                    default:   content.append(c);    cursor++;    break;
                }
            } else if (c == '"') {
                break;
            } else {
                content.append(c);
                cursor++;
            }
        }
        return content.toString().trim();
    }

    // ======================== JSON Parsing ========================

    private GeneratedGig parseGeneratedGig(String content) {
        GeneratedGig gig = new GeneratedGig();

        // Strip markdown code fences if present
        String json = content;
        if (json.contains("```json")) {
            json = json.substring(json.indexOf("```json") + 7);
            json = json.substring(0, json.indexOf("```"));
        } else if (json.contains("```")) {
            json = json.substring(json.indexOf("```") + 3);
            if (json.contains("```")) {
                json = json.substring(0, json.indexOf("```"));
            }
        }
        json = json.trim();

        gig.setTitle(extractString(json, "title"));
        gig.setDescription(extractString(json, "description"));
        gig.setCategory(extractString(json, "category"));
        gig.setPrice(extractDouble(json, "price"));
        gig.setDeliveryDays(extractInt(json, "deliveryDays"));

        // Validate / sanitize
        if (gig.getTitle() == null || gig.getTitle().isEmpty()) {
            gig.setTitle("I will provide a professional service");
        }
        if (gig.getDescription() == null || gig.getDescription().isEmpty()) {
            gig.setDescription("Professional service tailored to your needs. Contact me for details.");
        }
        if (gig.getPrice() < 10) gig.setPrice(50.0);
        if (gig.getDeliveryDays() < 1 || gig.getDeliveryDays() > 60) gig.setDeliveryDays(7);
        if (gig.getCategory() == null || gig.getCategory().isEmpty()) gig.setCategory("Development");

        return gig;
    }

    private String extractString(String json, String key) {
        // Match "key": "value" handling escaped quotes inside value
        Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
        Matcher m = p.matcher(json);
        if (m.find()) {
            return m.group(1).replace("\\\"", "\"").replace("\\n", "\n");
        }
        return "";
    }

    private double extractDouble(String json, String key) {
        Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*([\\d.]+)");
        Matcher m = p.matcher(json);
        if (m.find()) {
            try { return Double.parseDouble(m.group(1)); } catch (NumberFormatException e) { /* fall through */ }
        }
        return 0;
    }

    private int extractInt(String json, String key) {
        Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*(\\d+)");
        Matcher m = p.matcher(json);
        if (m.find()) {
            try { return Integer.parseInt(m.group(1)); } catch (NumberFormatException e) { /* fall through */ }
        }
        return 0;
    }

    // ======================== Local Fallback ========================

    /**
     * Generates a gig locally using keyword-based templates when the API is unavailable.
     */
    private GeneratedGig generateLocally(String prompt) {
        System.out.println("💡 [GigGenerator] Local generation for: " + prompt);
        GeneratedGig gig = new GeneratedGig();

        String lower = prompt.toLowerCase();

        // Detect category from keywords
        String category;
        String title;
        String description;
        double price;
        int days;

        if (matchesAny(lower, "logo", "design", "graphic", "banner", "flyer", "branding", "ui", "ux", "poster")) {
            category = "Design";
            title = "I will create a stunning " + extractMainKeyword(lower, "logo", "design", "banner", "flyer", "poster", "branding") + " for your brand";
            description = "I will design a professional, eye-catching " + extractMainKeyword(lower, "logo", "design", "banner", "flyer", "poster", "branding")
                    + " that perfectly represents your brand identity. With years of experience in graphic design, "
                    + "I deliver high-quality visuals that make your business stand out. "
                    + "You'll receive multiple concepts, unlimited revisions, and all source files. "
                    + "Fast turnaround and 100% satisfaction guaranteed. Let's bring your vision to life!";
            price = 80.0;
            days = 3;

        } else if (matchesAny(lower, "website", "web", "react", "angular", "vue", "frontend", "backend", "fullstack",
                "api", "app", "mobile", "android", "ios", "java", "python", "node", "php", "laravel", "spring")) {
            category = "Development";
            title = "I will develop a professional " + extractMainKeyword(lower, "website", "web app", "mobile app", "API", "application") + " for you";
            description = "I will build a modern, responsive, and scalable " + extractMainKeyword(lower, "website", "web application", "mobile app", "API", "software solution")
                    + " using the latest technologies and best practices. "
                    + "My development process includes clean code architecture, thorough testing, and performance optimization. "
                    + "You'll get a fully functional product with documentation and post-delivery support. "
                    + "Whether it's a startup MVP or an enterprise solution, I deliver on time and beyond expectations.";
            price = 300.0;
            days = 14;

        } else if (matchesAny(lower, "video", "edit", "animation", "motion", "youtube", "reel", "tiktok", "premiere", "after effects")) {
            category = "Video";
            title = "I will produce professional " + extractMainKeyword(lower, "video editing", "animation", "motion graphics", "video content") + " for you";
            description = "I will create high-quality video content that captivates your audience and delivers your message effectively. "
                    + "From concept to final cut, I handle editing, color grading, sound design, and motion graphics. "
                    + "Perfect for YouTube channels, social media reels, corporate videos, and promotional content. "
                    + "Expect cinematic quality with quick turnaround. Let's make your content go viral!";
            price = 120.0;
            days = 5;

        } else if (matchesAny(lower, "seo", "marketing", "social media", "ads", "facebook", "instagram", "google ads", "campaign", "growth")) {
            category = "Marketing";
            title = "I will create a results-driven " + extractMainKeyword(lower, "SEO strategy", "marketing campaign", "social media strategy", "ad campaign") + " for your business";
            description = "I will develop and execute a data-driven digital marketing strategy that grows your online presence and drives real results. "
                    + "My approach includes audience research, competitor analysis, content planning, and performance tracking. "
                    + "Whether you need SEO optimization, social media management, or paid advertising, I deliver measurable ROI. "
                    + "Get a complete strategy report with actionable insights. Let's scale your business together!";
            price = 150.0;
            days = 7;

        } else if (matchesAny(lower, "write", "article", "blog", "content", "copywriting", "copy", "ebook", "script", "translation")) {
            category = "Writing";
            title = "I will write compelling " + extractMainKeyword(lower, "articles", "blog posts", "copy", "content", "ebook") + " for you";
            description = "I will craft engaging, SEO-optimized written content that resonates with your target audience and drives action. "
                    + "With expertise in persuasive writing, I deliver well-researched, original content tailored to your brand voice. "
                    + "Each piece includes proper formatting, keyword integration, and a compelling call-to-action. "
                    + "Unlimited revisions until you're 100% satisfied. Let's tell your story the right way!";
            price = 50.0;
            days = 3;

        } else if (matchesAny(lower, "data", "analysis", "excel", "dashboard", "power bi", "tableau", "database", "sql", "scraping")) {
            category = "Data";
            title = "I will provide expert " + extractMainKeyword(lower, "data analysis", "dashboard", "database", "data scraping") + " services";
            description = "I will transform your raw data into actionable insights using advanced analytics tools and techniques. "
                    + "My services include data cleaning, visualization, statistical analysis, and interactive dashboard creation. "
                    + "I work with Excel, Python, SQL, Power BI, and Tableau to deliver comprehensive reports. "
                    + "Get clear, data-driven recommendations that help you make better business decisions.";
            price = 100.0;
            days = 5;

        } else if (matchesAny(lower, "music", "audio", "song", "beat", "mix", "master", "voiceover", "podcast", "sound")) {
            category = "Music";
            title = "I will produce professional " + extractMainKeyword(lower, "music", "audio", "beats", "voiceover", "podcast editing") + " for you";
            description = "I will deliver studio-quality audio production tailored to your project needs. "
                    + "Whether it's music production, mixing, mastering, voiceover, or podcast editing, I bring professional expertise. "
                    + "Using industry-standard tools, I ensure crystal-clear sound and creative excellence. "
                    + "Fast delivery with revisions included. Let's create something that sounds amazing!";
            price = 80.0;
            days = 4;

        } else {
            // Generic business / catch-all
            category = "Business";
            title = "I will provide professional " + prompt.substring(0, Math.min(prompt.length(), 50)).trim() + " services";
            description = "I will deliver a high-quality professional service based on your specific requirements. "
                    + "With a focus on excellence and client satisfaction, I bring expertise and dedication to every project. "
                    + "You'll receive timely delivery, clear communication throughout the process, and results that exceed expectations. "
                    + "Let's discuss your needs and get started on achieving your goals!";
            price = 100.0;
            days = 7;
        }

        // Ensure title length
        if (title.length() > 80) {
            title = title.substring(0, 77) + "...";
        }

        gig.setTitle(title);
        gig.setDescription(description);
        gig.setCategory(category);
        gig.setPrice(price);
        gig.setDeliveryDays(days);

        System.out.println("✅ [GigGenerator] Local result: " + gig);
        return gig;
    }

    private boolean matchesAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }

    private String extractMainKeyword(String text, String... candidates) {
        for (String candidate : candidates) {
            if (text.contains(candidate.toLowerCase())) return candidate;
        }
        return candidates[0]; // default to first
    }

    // ======================== Utilities ========================

    private String escapeJson(String text) {
        if (text == null) return "";
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
