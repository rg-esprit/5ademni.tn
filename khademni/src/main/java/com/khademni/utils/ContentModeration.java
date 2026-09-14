package com.khademni.utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

//Tu es nul et stupide.
// tu es  beau et intelligent
public class ContentModeration {

    private static final String API_KEY = loadApiKey();

    private static String loadApiKey() {
        String key = System.getenv("HUGGINGFACE_API_KEY");
        if (key != null && !key.isBlank()) {
            return key.trim();
        }
        try (InputStream input = ContentModeration.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input != null) {
                java.util.Properties prop = new java.util.Properties();
                prop.load(input);
                String val = prop.getProperty("HUGGINGFACE_API_KEY", "").trim();
                if (!val.isEmpty() && !val.contains("your_")) {
                    return val;
                }
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private static final String MODEL_URL = "https://router.huggingface.co/hf-inference/models/unitary/toxic-bert";

    private static final double TOXIC_THRESHOLD = 0.05;

    private static final int API_TIMEOUT_MS = 5000;

public static boolean isCommentAcceptable(String commentaire) {
    System.out.println("Debug: Entered isCommentAcceptable method with comment: " + commentaire);
    if (API_KEY == null || API_KEY.isBlank()) {
        System.out.println("ContentModeration: HuggingFace API key not configured, skipping remote check.");
        return true;
    }
    try {
        URL url = new URL(MODEL_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setConnectTimeout(API_TIMEOUT_MS);
        conn.setReadTimeout(API_TIMEOUT_MS);

        conn.setRequestMethod("POST");

        // Set headers
        conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("X-Use-Cache", "false");

        conn.setDoOutput(true);

        // Create JSON body
        JSONObject body = new JSONObject();
        body.put("inputs", commentaire);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = body.toString().getBytes(StandardCharsets.UTF_8);
            os.write(input);
        }

        int status = conn.getResponseCode();

        if (status != 200) {
            InputStream errorStream = conn.getErrorStream();
            if (errorStream != null) {
                Scanner errorScanner = new Scanner(errorStream, StandardCharsets.UTF_8);
                String errorResponse = errorScanner.useDelimiter("\\A").next();
                errorScanner.close();
                System.out.println("Erreur API détail : " + errorResponse);
            }
            System.out.println("Erreur API : " + status);
            return true;
        }

        Scanner scanner = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8);
        String response = scanner.useDelimiter("\\A").next();
        scanner.close();

        System.out.println("Réponse API : " + response);

        JSONArray outerArray = new JSONArray(response);
        JSONArray predictions = outerArray.getJSONArray(0);

        // Check for toxicity
        for (int i = 0; i < predictions.length(); i++) {
            JSONObject obj = predictions.getJSONObject(i);
            String label = obj.getString("label");
            double score = obj.getDouble("score");

            // Log the score in the terminal
            System.out.println("Label: " + label + ", Score: " + score);

            // Reject the comment if any label exceeds the threshold
            if ((label.equals("toxic") || label.equals("obscene") || label.equals("insult"))
                    && score >= TOXIC_THRESHOLD) {
                System.out.println("Comment rejected due to label: " + label + " with score: " + score);
                return false;
            }
        }

        return true;

    } catch (SocketTimeoutException e) {
        System.err.println("Moderation API timeout. Commentaire accepté sans vérification distante.");
        return true;
    } catch (IOException e) {
        System.err.println("Moderation API indisponible. Commentaire accepté sans vérification distante.");
        e.printStackTrace();
        return true;
    } catch (Exception e) {
        System.err.println("Erreur pendant la modération du commentaire. Commentaire accepté par défaut.");
        e.printStackTrace();
        return true;
    }
}
}
