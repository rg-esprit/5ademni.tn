package com.khademni.service;

import com.khademni.model.BadWordResult;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI-powered Bad Word Detection Service using the DeepSeek API.
 *
 * Analyzes gig titles and descriptions for profanity, hate speech, slurs,
 * sexual content, threats, and other inappropriate language.
 *
 * Architecture:
 *   1. Primary:  DeepSeek Chat API (multilingual, context-aware)
 *   2. Fallback: Local keyword dictionary (offline, English/French/Arabic)
 *
 * The DeepSeek model returns a structured JSON verdict which is parsed
 * into a {@link BadWordResult} containing the flagged words, severity,
 * and a human-readable explanation.
 */
public class BadWordDetectionService {

    // ======================== Configuration ========================

    private static final String DEEPSEEK_API_URL = "https://api.deepseek.com/v1/chat/completions";
    private static final String DEEPSEEK_MODEL   = "deepseek-chat";
    private static final String API_KEY          = loadApiKey();

    private static String loadApiKey() {
        String key = System.getenv("DEEPSEEK_API_KEY");
        if (key != null && !key.isBlank()) {
            return key.trim();
        }
        try (InputStream input = BadWordDetectionService.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input != null) {
                Properties prop = new Properties();
                prop.load(input);
                String val = prop.getProperty("DEEPSEEK_API_KEY", "").trim();
                if (!val.isEmpty() && !val.contains("your_")) {
                    return val;
                }
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    /** Connection timeout (ms) — fail fast so the UI stays responsive */
    private static final int CONNECT_TIMEOUT = 8000;
    /** Read timeout (ms) */
    private static final int READ_TIMEOUT    = 15000;

    // ======================== Local Fallback Dictionary ========================

    /** Common profanity / slurs in English, French, and transliterated Arabic */
    private static final List<String> BAD_WORDS_DICTIONARY = List.of(
        // English profanity
        "fuck", "shit", "ass", "asshole", "bitch", "bastard", "damn", "dick",
        "pussy", "cock", "cunt", "whore", "slut", "nigger", "nigga", "faggot",
        "retard", "motherfucker", "bullshit", "crap",
        // English hate / threat
        "kill you", "i will kill", "death threat", "rape", "murder you",
        // French profanity
        "merde", "putain", "connard", "connasse", "enculé", "salaud", "salope",
        "bordel", "nique", "foutre", "ta gueule", "fils de pute",
        // Transliterated Arabic profanity
        "kuss", "sharmouta", "sharmout", "zamel", "manyak", "kol khara",
        "ibn el sharmouta", "nik", "nikomok", "zebi", "kahba", "nayek",
        "tfo", "kol ayre"
    );

    // ======================== System Prompt ========================

    private static final String SYSTEM_PROMPT = """
        You are a professional content moderation AI. Your task is to analyze text
        submitted for a freelance gig listing and detect any inappropriate language
        including profanity, slurs, hate speech, sexual content, threats, or
        discriminatory language in ANY language (English, French, Arabic, etc.).

        You MUST respond with ONLY a valid JSON object — no markdown, no explanation
        outside the JSON. Use this exact schema:

        {
          "hasBadWords": true or false,
          "detectedWords": ["word1", "word2"],
          "severity": "NONE" | "LOW" | "MEDIUM" | "HIGH",
          "explanation": "Brief reason in English"
        }

        Severity guide:
        - NONE:   Content is clean.
        - LOW:    Mildly inappropriate (e.g., "damn", "crap") — warn but allow.
        - MEDIUM: Clearly offensive profanity or insults — block submission.
        - HIGH:   Hate speech, slurs, threats, or sexual content — block immediately.

        Be strict but fair. Do not flag normal professional language.
        """;

    // ======================== Public API ========================

    /**
     * Analyzes the given title and description for bad words.
     * Tries DeepSeek API first, falls back to local dictionary on failure.
     *
     * @param title       the gig title
     * @param description the gig description
     * @return BadWordResult with the analysis verdict
     */
    public BadWordResult analyze(String title, String description) {
        String safeTitle = title != null ? title : "";
        String safeDesc  = description != null ? description : "";
        String combined  = safeTitle + " " + safeDesc;

        if (combined.trim().isEmpty()) {
            return new BadWordResult(); // nothing to analyze
        }

        // Try AI-powered detection first
        try {
            System.out.println("🔍 [BadWordDetection] Calling DeepSeek API...");
            BadWordResult aiResult = analyzeWithDeepSeek(safeTitle, safeDesc);
            System.out.println("✅ [BadWordDetection] AI result: " + aiResult);
            return aiResult;
        } catch (Exception e) {
            System.out.println("⚠️  [BadWordDetection] DeepSeek API failed: " + e.getMessage());
            System.out.println("🔄 [BadWordDetection] Falling back to local dictionary...");
        }

        // Fallback: local dictionary scan
        return analyzeWithLocalDictionary(safeTitle, safeDesc);
    }

    // ======================== DeepSeek API ========================

    private BadWordResult analyzeWithDeepSeek(String title, String description) throws Exception {
        String userMessage = "Analyze this gig listing for inappropriate language:\n\n"
                + "Title: " + title + "\n"
                + "Description: " + description;

        // Build the chat completion request JSON
        String requestJson = buildChatRequest(userMessage);

        // Call the API
        String responseBody = callDeepSeekAPI(requestJson);

        // Extract the assistant's message content
        String content = extractMessageContent(responseBody);

        // Parse the structured JSON from the AI response
        return parseAIResponse(content);
    }

    private String buildChatRequest(String userMessage) {
        // Escape strings for safe JSON embedding
        String escapedSystem = escapeJson(SYSTEM_PROMPT);
        String escapedUser   = escapeJson(userMessage);

        return "{"
                + "\"model\": \"" + DEEPSEEK_MODEL + "\","
                + "\"messages\": ["
                + "  {\"role\": \"system\", \"content\": \"" + escapedSystem + "\"},"
                + "  {\"role\": \"user\",   \"content\": \"" + escapedUser   + "\"}"
                + "],"
                + "\"temperature\": 0.1,"
                + "\"max_tokens\": 300"
                + "}";
    }

    private String callDeepSeekAPI(String requestJson) throws Exception {
        if (API_KEY == null || API_KEY.isBlank()) {
            throw new IllegalStateException("DeepSeek API key is not configured.");
        }
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

            // Send request body
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = requestJson.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            System.out.println("   [DeepSeek] Response code: " + responseCode);

            BufferedReader reader;
            if (responseCode >= 200 && responseCode < 300) {
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            } else {
                reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
                StringBuilder errBody = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) errBody.append(line);
                reader.close();
                throw new Exception("DeepSeek API error " + responseCode + ": " + errBody);
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

    /**
     * Extracts the assistant's message content from the DeepSeek chat completion response.
     * Expected structure: { "choices": [ { "message": { "content": "..." } } ] }
     */
    private String extractMessageContent(String responseBody) throws Exception {
        // Find "content" inside the first "message" object
        int msgIdx = responseBody.indexOf("\"message\"");
        if (msgIdx == -1) throw new Exception("No 'message' field in response");

        int contentIdx = responseBody.indexOf("\"content\"", msgIdx);
        if (contentIdx == -1) throw new Exception("No 'content' field in message");

        // Find the value after "content":
        int colonIdx = responseBody.indexOf(':', contentIdx + 9);
        if (colonIdx == -1) throw new Exception("Malformed content field");

        // Find the opening quote of the value
        int openQuote = responseBody.indexOf('"', colonIdx + 1);
        if (openQuote == -1) throw new Exception("No opening quote for content value");

        // Find the closing quote (handle escaped quotes)
        int cursor = openQuote + 1;
        StringBuilder content = new StringBuilder();
        while (cursor < responseBody.length()) {
            char c = responseBody.charAt(cursor);
            if (c == '\\' && cursor + 1 < responseBody.length()) {
                char next = responseBody.charAt(cursor + 1);
                if (next == '"') {
                    content.append('"');
                    cursor += 2;
                } else if (next == 'n') {
                    content.append('\n');
                    cursor += 2;
                } else if (next == '\\') {
                    content.append('\\');
                    cursor += 2;
                } else if (next == 't') {
                    content.append('\t');
                    cursor += 2;
                } else {
                    content.append(c);
                    cursor++;
                }
            } else if (c == '"') {
                break; // end of string
            } else {
                content.append(c);
                cursor++;
            }
        }

        return content.toString().trim();
    }

    // ======================== AI Response Parsing ========================

    /**
     * Parses the structured JSON returned by the AI into a BadWordResult.
     * Expected format:
     * {
     *   "hasBadWords": true/false,
     *   "detectedWords": ["word1", "word2"],
     *   "severity": "NONE|LOW|MEDIUM|HIGH",
     *   "explanation": "..."
     * }
     */
    private BadWordResult parseAIResponse(String content) {
        BadWordResult result = new BadWordResult();

        try {
            // Strip any markdown code fences the model might add
            String json = content;
            if (json.contains("```json")) {
                json = json.substring(json.indexOf("```json") + 7);
                json = json.substring(0, json.indexOf("```"));
            } else if (json.contains("```")) {
                json = json.substring(json.indexOf("```") + 3);
                json = json.substring(0, json.indexOf("```"));
            }
            json = json.trim();

            // Parse hasBadWords
            result.setHasBadWords(extractBoolean(json, "hasBadWords"));

            // Parse severity
            result.setSeverity(extractString(json, "severity"));

            // Parse explanation
            result.setExplanation(extractString(json, "explanation"));

            // Parse detectedWords array
            result.setDetectedWords(extractStringArray(json, "detectedWords"));

        } catch (Exception e) {
            System.out.println("⚠️  [BadWordDetection] Failed to parse AI response: " + e.getMessage());
            System.out.println("   Raw content: " + content);
            // If parsing fails but the response text itself mentions bad words, flag it
            if (content.toLowerCase().contains("\"hasb" + "adwords\": true")
                    || content.toLowerCase().contains("\"hasb" + "adwords\":true")) {
                result.setHasBadWords(true);
                result.setSeverity("MEDIUM");
                result.setExplanation("Content flagged by AI (response parse partial).");
            }
        }

        return result;
    }

    private boolean extractBoolean(String json, String key) {
        Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*(true|false)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(json);
        return m.find() && m.group(1).equalsIgnoreCase("true");
    }

    private String extractString(String json, String key) {
        Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]*?)\"");
        Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : "";
    }

    private List<String> extractStringArray(String json, String key) {
        List<String> list = new ArrayList<>();
        Pattern arrayPattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\\[([^\\]]*)]");
        Matcher arrayMatcher = arrayPattern.matcher(json);
        if (arrayMatcher.find()) {
            String arrayContent = arrayMatcher.group(1);
            Pattern itemPattern = Pattern.compile("\"([^\"]+?)\"");
            Matcher itemMatcher = itemPattern.matcher(arrayContent);
            while (itemMatcher.find()) {
                list.add(itemMatcher.group(1));
            }
        }
        return list;
    }

    // ======================== Local Fallback ========================

    /**
     * Scans text against a local dictionary of known bad words.
     * Used when the DeepSeek API is unavailable.
     */
    private BadWordResult analyzeWithLocalDictionary(String title, String description) {
        BadWordResult result = new BadWordResult();
        String combined = (title + " " + description).toLowerCase();

        List<String> found = new ArrayList<>();
        for (String badWord : BAD_WORDS_DICTIONARY) {
            // Use word boundary matching to avoid false positives (e.g., "class" inside "classification")
            Pattern p = Pattern.compile("\\b" + Pattern.quote(badWord) + "\\b", Pattern.CASE_INSENSITIVE);
            if (p.matcher(combined).find()) {
                found.add(badWord);
            }
        }

        if (!found.isEmpty()) {
            result.setHasBadWords(true);
            result.setDetectedWords(found);

            // Determine severity based on word count and categories
            if (found.size() >= 3) {
                result.setSeverity("HIGH");
            } else if (found.size() == 2) {
                result.setSeverity("MEDIUM");
            } else {
                result.setSeverity("MEDIUM");
            }

            result.setExplanation("Detected " + found.size() + " inappropriate word(s) via local dictionary scan: "
                    + String.join(", ", found) + ".");
            System.out.println("🚫 [BadWordDetection] Local scan found: " + found);
        } else {
            result.setSeverity("NONE");
            result.setExplanation("No inappropriate content detected (local scan).");
            System.out.println("✅ [BadWordDetection] Local scan: content is clean.");
        }

        return result;
    }

    // ======================== Utilities ========================

    /** Escapes a string for safe inclusion inside a JSON string value */
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
