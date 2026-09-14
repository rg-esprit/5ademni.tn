package com.khademni.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.prefs.Preferences;

/**
 * Service singleton pour la conversion de devises via ExchangeRate-API.
 * 
 * Features:
 * - Fetch asynchrone des taux TND -> EUR, USD (et toutes devises)
 * - Cache local via java.util.prefs.Preferences (TTL 24h)
 * - Methode utilitaire convertPrice(amount, currency)
 */
public class ExchangeRateService {

    private static final String API_KEY = loadApiKey();
    private static final String API_URL = "https://v6.exchangerate-api.com/v6/" + API_KEY + "/latest/TND";

    private static String loadApiKey() {
        String key = System.getenv("EXCHANGERATE_API_KEY");
        if (key == null || key.isBlank()) {
            key = System.getenv("EXCHANGE_RATE_API_KEY");
        }
        if (key != null && !key.isBlank()) {
            return key.trim();
        }
        try (InputStream input = ExchangeRateService.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input != null) {
                Properties prop = new Properties();
                prop.load(input);
                String val = prop.getProperty("EXCHANGERATE_API_KEY", "").trim();
                if (!val.isEmpty() && !val.contains("your_")) {
                    return val;
                }
            }
        } catch (Exception ignored) {
        }
        return "";
    }
    private static final long CACHE_TTL_MS = 24 * 60 * 60 * 1000L; // 24 hours
    private static final String PREF_KEY_RATES = "exchange_rates_json";
    private static final String PREF_KEY_TIMESTAMP = "exchange_rates_timestamp";

    private static ExchangeRateService instance;
    private final Map<String, Double> rates = new ConcurrentHashMap<>();
    private boolean loaded = false;

    private ExchangeRateService() {
        loadFromCache();
        // Always try to refresh in background
        refreshRatesAsync();
    }

    public static synchronized ExchangeRateService getInstance() {
        if (instance == null) {
            instance = new ExchangeRateService();
        }
        return instance;
    }

    /**
     * Converts an amount from TND to the target currency.
     * Returns the converted value rounded to 2 decimal places.
     * Returns -1 if rates are not available.
     */
    public double convertPrice(double amountTND, String targetCurrency) {
        Double rate = rates.get(targetCurrency.toUpperCase());
        if (rate == null) {
            return -1;
        }
        return Math.round(amountTND * rate * 100.0) / 100.0;
    }

    /**
     * Returns the exchange rate for TND -> target currency.
     * Returns -1 if not available.
     */
    public double getRate(String currency) {
        Double rate = rates.get(currency.toUpperCase());
        return rate != null ? rate : -1;
    }

    /**
     * Returns true if rates are loaded (from cache or API).
     */
    public boolean isLoaded() {
        return loaded;
    }

    /**
     * Formats a price with EUR and USD conversions.
     * Example: "~ 59.14 EUR / 69.84 $"
     */
    public String formatConversions(double amountTND) {
        if (!loaded) {
            return "Chargement des taux...";
        }
        double eur = convertPrice(amountTND, "EUR");
        double usd = convertPrice(amountTND, "USD");
        if (eur < 0 || usd < 0) {
            return "Taux indisponibles";
        }
        return String.format("~ %.2f EUR / %.2f $", eur, usd);
    }

    // ---------- Cache Management ----------

    private void loadFromCache() {
        try {
            Preferences prefs = Preferences.userNodeForPackage(ExchangeRateService.class);
            long timestamp = prefs.getLong(PREF_KEY_TIMESTAMP, 0);
            String json = prefs.get(PREF_KEY_RATES, null);

            if (json != null && (System.currentTimeMillis() - timestamp) < CACHE_TTL_MS) {
                parseRatesJson(json);
                loaded = true;
                System.out.println("[ExchangeRate] Taux charges depuis le cache local.");
            }
        } catch (Exception e) {
            System.err.println("[ExchangeRate] Erreur lecture cache: " + e.getMessage());
        }
    }

    private void saveToCache(String json) {
        try {
            Preferences prefs = Preferences.userNodeForPackage(ExchangeRateService.class);
            prefs.put(PREF_KEY_RATES, json);
            prefs.putLong(PREF_KEY_TIMESTAMP, System.currentTimeMillis());
            prefs.flush();
            System.out.println("[ExchangeRate] Taux sauvegardes dans le cache local.");
        } catch (Exception e) {
            System.err.println("[ExchangeRate] Erreur sauvegarde cache: " + e.getMessage());
        }
    }

    // ---------- API Fetch ----------

    private void refreshRatesAsync() {
        if (API_KEY == null || API_KEY.isBlank()) {
            System.out.println("[ExchangeRate] API key not configured. Skipping live fetch.");
            return;
        }
        Thread thread = new Thread(() -> {
            try {
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .build();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_URL))
                        .timeout(Duration.ofSeconds(15))
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    String body = response.body();
                    parseRatesJson(body);
                    saveToCache(body);
                    loaded = true;
                    System.out.println("[ExchangeRate] Taux rafraichis depuis l'API. EUR="
                            + rates.get("EUR") + " USD=" + rates.get("USD"));
                } else {
                    System.err.println("[ExchangeRate] API HTTP " + response.statusCode());
                }
            } catch (Exception e) {
                System.err.println("[ExchangeRate] Erreur fetch API: " + e.getMessage());
                // Cache remains valid if previously loaded
            }
        }, "ExchangeRate-Fetcher");
        thread.setDaemon(true);
        thread.start();
    }

    private void parseRatesJson(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);
            JsonNode conversionRates = root.get("conversion_rates");

            if (conversionRates != null && conversionRates.isObject()) {
                conversionRates.fields().forEachRemaining(entry -> {
                    rates.put(entry.getKey(), entry.getValue().asDouble());
                });
            }
        } catch (Exception e) {
            System.err.println("[ExchangeRate] Erreur parsing JSON: " + e.getMessage());
        }
    }
}
