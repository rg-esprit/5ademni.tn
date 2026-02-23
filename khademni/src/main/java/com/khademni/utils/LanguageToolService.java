package com.khademni.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Calls the free LanguageTool public REST API to detect grammar and spelling
 * errors.
 * API endpoint: https://api.languagetool.org/v2/check
 * No API key required for the free tier.
 */
public class LanguageToolService {

    private static final String API_URL = "https://api.languagetool.org/v2/check";
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Represents a single match returned by LanguageTool.
     */
    public static class Match {
        public final int offset;
        public final int length;
        public final List<String> replacements;

        public Match(int offset, int length, List<String> replacements) {
            this.offset = offset;
            this.length = length;
            this.replacements = replacements;
        }
    }

    /**
     * Sends text to the LanguageTool API and returns a list of suggested
     * corrections.
     *
     * @param text     the text to check
     * @param language BCP47 language code, e.g. "fr", "en-US", "ar"
     * @return list of Match objects (may be empty); returns empty list on any error
     */
    public static List<Match> check(String text, String language) {
        List<Match> results = new ArrayList<>();
        if (text == null || text.isBlank())
            return results;

        try {
            String body = "text=" + URLEncoder.encode(text, StandardCharsets.UTF_8)
                    + "&language=" + URLEncoder.encode(language, StandardCharsets.UTF_8)
                    + "&enabledOnly=false";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .timeout(Duration.ofSeconds(8))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = MAPPER.readTree(response.body());
                JsonNode matches = root.path("matches");
                for (JsonNode m : matches) {
                    int offset = m.path("offset").asInt();
                    int length = m.path("length").asInt();
                    List<String> reps = new ArrayList<>();
                    for (JsonNode r : m.path("replacements")) {
                        reps.add(r.path("value").asText());
                    }
                    if (!reps.isEmpty()) {
                        results.add(new Match(offset, length, reps));
                    }
                }
            }
        } catch (Exception e) {
            // Silently ignore — network errors must not break the UI
        }
        return results;
    }
}
