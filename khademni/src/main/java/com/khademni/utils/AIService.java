package com.khademni.utils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import org.json.JSONObject; // Note: I might need to add a JSON library to pom.xml

public class AIService {
    private static String API_KEY = loadApiKey();
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";

    private static String loadApiKey() {
        // 1. Try environment variable
        String key = System.getenv("GEMINI_API_KEY");

        // 2. Try loading from properties file
        if (key == null || key.isEmpty()) {
            try (java.io.InputStream input = AIService.class.getClassLoader()
                    .getResourceAsStream("config.properties")) {
                if (input != null) {
                    java.util.Properties prop = new java.util.Properties();
                    prop.load(input);
                    String fileKey = prop.getProperty("GEMINI_API_KEY");
                    if (fileKey != null && !fileKey.trim().isEmpty()
                            && !fileKey.contains("YOUR_API_KEY_HERE")) {
                        key = fileKey.trim();
                    }
                }
            } catch (Exception e) {
                System.err.println("Note: Could not load config.properties: " + e.getMessage());
            }
        }
        return key;
    }

    private static String getApiUrl() {
        return API_URL + API_KEY;
    }

    public static CompletableFuture<String> rewriteProfessionally(String text, java.util.Map<String, String> context) {
        if (API_KEY == null || API_KEY.isEmpty()) {
            return CompletableFuture.completedFuture(
                    "Error: Gemini API Key is missing. Please add it to config.properties to enable real AI generation.");
        }

        HttpClient client = HttpClient.newHttpClient();
        boolean isJobPost = context != null && "Job Description".equals(context.get("contextType"));

        // Create a creative prompt for 100% AI generation
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("Act as a creative professional recruiter and writer.\n");

        if (isJobPost) {
            promptBuilder.append(
                    "Generate a professional Job Description AND a list of specific Requirements based on the input.\n");
            promptBuilder.append(
                    "MANDATORY: You MUST return a valid JSON object only. Do NOT include any markdown formatting like ```json. \n");
            promptBuilder.append("JSON Structure:\n");
            promptBuilder.append(
                    "{ \"description\": \"Put the full professional description here\", \"requirements\": \"List requirements here, separated by newlines\" }\n");
            promptBuilder.append(
                    "The requirements should be a newline-separated string without any symbols (no - or *).\n\n");
        } else {
            promptBuilder.append("Generate a 100% original and structured description based on the input below.\n\n");
        }

        if (context != null && !context.isEmpty()) {
            promptBuilder.append("Role Context:\n");
            context.forEach((key, value) -> {
                if (value != null && !value.isEmpty()) {
                    promptBuilder.append("- ").append(key).append(": ").append(value).append("\n");
                }
            });
            promptBuilder.append("\n");
        }

        promptBuilder.append("Instructions:\n");
        promptBuilder.append("1. Analyze the 'Original Text' and expand it into a full, professional description.\n");
        promptBuilder.append(
                "2. Structure with clear headers (e.g., About the Role, Key Responsibilities, Requirements).\n");
        promptBuilder.append("3. Avoid generic templates.\n");
        promptBuilder.append("4. Tone: Catchy, professional, and authoritative.\n\n");
        promptBuilder.append("Original Text: ").append(text).append("\n\n");
        promptBuilder.append("Rewriting Result:");

        String prompt = promptBuilder.toString();

        String json = "{ \"contents\": [{ \"parts\": [{ \"text\": \""
                + prompt.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\" }] }] }";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getApiUrl()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            String body = response.body();
                            return parseResponse(body);
                        } catch (Exception e) {
                            return "Error processing AI response: " + e.getMessage();
                        }
                    }
                    System.err.println("AI API Error: " + response.statusCode() + " - " + response.body());
                    return "AI Error (" + response.statusCode() + "): " + response.body();
                });
    }

    private static String parseResponse(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            return obj.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text");
        } catch (Exception e) {
            System.err.println("JSON parse error: " + e.getMessage());
            return "ERROR: Unable to parse AI response content.";
        }
    }
}
