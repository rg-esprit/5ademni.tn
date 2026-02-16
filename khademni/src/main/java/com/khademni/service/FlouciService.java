package com.khademni.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

public class FlouciService {

    private static final String API_URL = "https://developers.flouci.com/api/generate_payment";
    private static final String APP_TOKEN = "e46e392c-6338-4e3a-9e12-32a76f235f3d"; // Example token, user should update
    private static final String APP_PUBLIC = "9f270a25-83c9-4b6e-827d-773a246101cd"; // Example public key

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public FlouciService() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public String generatePaymentLink(double amount, String developerDescription) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("app_token", APP_TOKEN);
        body.put("app_public", APP_PUBLIC);
        body.put("amount", (int) (amount * 1000)); // Amount in millimes
        body.put("accept_card", "true");
        body.put("session_timeout_secs", 1200);
        body.put("success_link", "https://5ademni.tn/success");
        body.put("fail_link", "https://5ademni.tn/fail");
        body.put("developer_tracking_id", developerDescription);

        String jsonBody = objectMapper.writeValueAsString(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JsonNode root = objectMapper.readTree(response.body());
            if (root.get("success").asBoolean()) {
                return root.get("result").get("payment_url").asText();
            } else {
                throw new Exception("Flouci API error: " + root.get("message").asText());
            }
        } else {
            throw new Exception("HTTP error: " + response.statusCode() + " - " + response.body());
        }
    }
}
