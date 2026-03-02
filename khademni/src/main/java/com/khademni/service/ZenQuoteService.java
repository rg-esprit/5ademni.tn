package com.khademni.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.prefs.Preferences;

/**
 * Service pour récupérer des citations motivationnelles depuis ZenQuotes API.
 * 
 * Features:
 * - Fetch asynchrone depuis https://zenquotes.io/api/random
 * - Citation de secours (fallback) si l'API est hors ligne
 * - Cache local via Preferences pour éviter les appels répétitifs
 */
public class ZenQuoteService {

    private static final String API_URL = "https://zenquotes.io/api/random";
    private static final String PREF_KEY_QUOTE = "zen_quote_text";
    private static final String PREF_KEY_AUTHOR = "zen_quote_author";

    // Fallback quotes if API is unavailable
    private static final String[][] FALLBACK_QUOTES = {
            { "Le succès n'est pas final, l'échec n'est pas fatal : c'est le courage de continuer qui compte.",
                    "Winston Churchill" },
            { "La seule façon de faire du bon travail est d'aimer ce que vous faites.", "Steve Jobs" },
            { "Chaque accomplissement commence par la décision d'essayer.", "John F. Kennedy" },
            { "Le futur appartient à ceux qui croient en la beauté de leurs rêves.", "Eleanor Roosevelt" },
            { "Ne jugez pas chaque jour par la récolte que vous faites, mais par les graines que vous plantez.",
                    "Robert Louis Stevenson" },
    };

    private String currentQuote;
    private String currentAuthor;
    private boolean loading = false;

    // Callback interface pour notifier l'UI quand la citation est prête
    @FunctionalInterface
    public interface QuoteCallback {
        void onQuoteReady(String quote, String author);
    }

    public ZenQuoteService() {
        // Load from cache first
        loadFromCache();
        if (currentQuote == null) {
            loadFallback();
        }
    }

    /**
     * Returns the current quote text.
     */
    public String getCurrentQuote() {
        return currentQuote;
    }

    /**
     * Returns the current quote author.
     */
    public String getCurrentAuthor() {
        return currentAuthor;
    }

    /**
     * Returns true if a fetch is in progress.
     */
    public boolean isLoading() {
        return loading;
    }

    /**
     * Fetches a new random quote asynchronously.
     * Calls the callback on the JavaFX Application Thread when done.
     */
    public void fetchNewQuote(QuoteCallback callback) {
        loading = true;
        Thread thread = new Thread(() -> {
            try {
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(8))
                        .build();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_URL))
                        .timeout(Duration.ofSeconds(10))
                        .header("Accept", "application/json")
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode root = mapper.readTree(response.body());

                    if (root.isArray() && root.size() > 0) {
                        JsonNode quoteNode = root.get(0);
                        String quote = quoteNode.get("q").asText();
                        String author = quoteNode.get("a").asText();

                        // Avoid the "Too many requests" placeholder
                        if (!"zenquotes.io".equalsIgnoreCase(author)) {
                            currentQuote = quote;
                            currentAuthor = author;
                            saveToCache();
                            System.out.println("[ZenQuote] Nouvelle citation: \"" + quote + "\" — " + author);
                        } else {
                            System.out.println("[ZenQuote] API rate limited, using cached/fallback.");
                        }
                    }
                } else {
                    System.err.println("[ZenQuote] API HTTP " + response.statusCode());
                }
            } catch (Exception e) {
                System.err.println("[ZenQuote] Erreur fetch: " + e.getMessage());
                // Keep current quote (cached or fallback)
            } finally {
                loading = false;
                if (callback != null) {
                    javafx.application.Platform.runLater(
                            () -> callback.onQuoteReady(currentQuote, currentAuthor));
                }
            }
        }, "ZenQuote-Fetcher");
        thread.setDaemon(true);
        thread.start();
    }

    // ---------- Cache ----------

    private void loadFromCache() {
        try {
            Preferences prefs = Preferences.userNodeForPackage(ZenQuoteService.class);
            String quote = prefs.get(PREF_KEY_QUOTE, null);
            String author = prefs.get(PREF_KEY_AUTHOR, null);
            if (quote != null && author != null) {
                currentQuote = quote;
                currentAuthor = author;
                System.out.println("[ZenQuote] Citation chargée depuis le cache.");
            }
        } catch (Exception e) {
            System.err.println("[ZenQuote] Erreur lecture cache: " + e.getMessage());
        }
    }

    private void saveToCache() {
        try {
            Preferences prefs = Preferences.userNodeForPackage(ZenQuoteService.class);
            prefs.put(PREF_KEY_QUOTE, currentQuote);
            prefs.put(PREF_KEY_AUTHOR, currentAuthor);
            prefs.flush();
        } catch (Exception e) {
            System.err.println("[ZenQuote] Erreur sauvegarde cache: " + e.getMessage());
        }
    }

    private void loadFallback() {
        int index = (int) (Math.random() * FALLBACK_QUOTES.length);
        currentQuote = FALLBACK_QUOTES[index][0];
        currentAuthor = FALLBACK_QUOTES[index][1];
        System.out.println("[ZenQuote] Citation de secours chargée.");
    }
}
