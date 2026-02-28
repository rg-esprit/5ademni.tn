package com.khademni.utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;


//Tu es nul et stupide.
// tu es  beau et intelligent
public class ContentModeration {

    private static final String API_KEY = "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"; // ⚠️ Mets ton token ici

    private static final String MODEL_URL ="https://router.huggingface.co/hf-inference/models/unitary/toxic-bert";

    private static final double TOXIC_THRESHOLD = 0.3;

   public static boolean isCommentAcceptable(String commentaire) {
    try {
        URL url = new URL(MODEL_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

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
            // Handle API error
            InputStream errorStream = conn.getErrorStream();
            if (errorStream != null) {
                Scanner errorScanner = new Scanner(errorStream, StandardCharsets.UTF_8);
                String errorResponse = errorScanner.useDelimiter("\\A").next();
                errorScanner.close();
                System.out.println("Erreur API détail : " + errorResponse);
            }
            System.out.println("Erreur API : " + status);
            return false;
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

            // Reject comment if any label exceeds the threshold
            if ((label.equals("toxic") || label.equals("obscene") || label.equals("insult")) && score >= TOXIC_THRESHOLD) {
                System.out.println("Comment rejected due to label: " + label + " with score: " + score);
                return false;
            }
        }

        return true;

    } catch (Exception e) {
        e.printStackTrace();
        return false;
    }
}
}