package com.khademni.service;

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
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for real-time grammar and spelling correction using the
 * <a href="https://api.languagetool.org/v2/check">LanguageTool public API</a>.
 *
 * <p>
 * Detects grammar/spelling errors and automatically replaces them with
 * the first suggested correction. Designed to run on a background thread
 * so the JavaFX UI is never blocked.
 * </p>
 *
 * <h3>Usage</h3>
 * 
 * <pre>{@code
 * GrammarCorrectionService svc = GrammarCorrectionService.getInstance();
 * String fixed = svc.correctText("je suis contant");
 * // fixed → "je suis content"
 * }</pre>
 */
public class GrammarCorrectionService {

    private static final Logger LOG = Logger.getLogger(GrammarCorrectionService.class.getName());

    /** LanguageTool v2 check endpoint. */
    private static final String API_URL = "https://api.languagetool.org/v2/check";

    /** Default language for correction (French). */
    private static final String DEFAULT_LANGUAGE = "fr";

    /** Shared HTTP client — singleton, thread-safe, 5 s connect timeout. */
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /** Jackson mapper — thread-safe singleton. */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ── Singleton ──────────────────────────────────────────────────────

    private static final GrammarCorrectionService INSTANCE = new GrammarCorrectionService();

    private GrammarCorrectionService() {
    }

    /** Returns the shared singleton instance. */
    public static GrammarCorrectionService getInstance() {
        return INSTANCE;
    }

    // ── Inner types ────────────────────────────────────────────────────

    /**
     * Represents a single match (error) detected by LanguageTool.
     *
     * @param offset       character offset of the error in the original text
     * @param length       length of the erroneous span
     * @param replacements suggested replacements (first is the best)
     */
    public record CorrectionMatch(int offset, int length, List<String> replacements) {
    }

    // ── Public API ─────────────────────────────────────────────────────

    /**
     * Corrects grammar and spelling in the given French text.
     * Every detected error is replaced with the <b>first</b> suggestion.
     *
     * @param input text to correct
     * @return corrected text, or the original input on error / empty input
     */
    public String correctText(String input) {
        return correctText(input, DEFAULT_LANGUAGE);
    }

    /**
     * Corrects grammar and spelling in the given text for the specified language.
     *
     * @param input    text to correct
     * @param language BCP-47 language code (e.g. {@code "fr"}, {@code "en-US"})
     * @return corrected text, or the original input on error / empty input
     */
    public String correctText(String input, String language) {
        if (input == null || input.isBlank()) {
            return input;
        }

        List<CorrectionMatch> matches = check(input, language);
        if (matches.isEmpty()) {
            return input;
        }

        // Apply replacements in reverse offset order so earlier offsets
        // are not shifted by preceding replacements.
        matches.sort(Comparator.comparingInt(CorrectionMatch::offset).reversed());

        StringBuilder sb = new StringBuilder(input);
        for (CorrectionMatch m : matches) {
            if (m.offset() < 0 || m.offset() + m.length() > sb.length()) {
                continue; // safety guard
            }
            sb.replace(m.offset(), m.offset() + m.length(), m.replacements().get(0));
        }
        return sb.toString();
    }

    /**
     * Sends text to the LanguageTool API and returns raw correction matches.
     *
     * @param text     the text to check
     * @param language BCP-47 language code
     * @return list of {@link CorrectionMatch} (empty on error or if no issues
     *         found)
     */
    public List<CorrectionMatch> check(String text, String language) {
        List<CorrectionMatch> results = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return results;
        }

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

            int status = response.statusCode();

            if (status == 429) {
                LOG.warning("LanguageTool API rate limit reached (HTTP 429). Skipping correction.");
                return results;
            }

            if (status != 200) {
                LOG.warning("LanguageTool API returned HTTP " + status);
                return results;
            }

            JsonNode root = MAPPER.readTree(response.body());
            JsonNode matchesNode = root.path("matches");
            for (JsonNode m : matchesNode) {
                int offset = m.path("offset").asInt();
                int length = m.path("length").asInt();
                List<String> reps = new ArrayList<>();
                for (JsonNode r : m.path("replacements")) {
                    reps.add(r.path("value").asText());
                }
                if (!reps.isEmpty()) {
                    results.add(new CorrectionMatch(offset, length, reps));
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.log(Level.WARNING, "LanguageTool API call interrupted", e);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "LanguageTool API call failed — returning original text", e);
        }
        return results;
    }
}
