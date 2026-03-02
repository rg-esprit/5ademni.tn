package com.khademni.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.khademni.App;
import com.khademni.model.UserModel;
import com.khademni.utils.MyDataBase;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Controls the face-capture dialog.
 *
 * Camera capture is handled entirely by the Python backend via cv2 (arm64 compatible).
 * Java only shows progress UI and drives the HTTP call.
 *
 * Usage:
 *   FaceController fc = loader.getController();
 *   fc.configureEnroll(userId);   // or fc.configureLogin();
 *   dialog.showAndWait();
 */
public class FaceController {

    // ── FXML fields ──────────────────────────────────────────────────────────
    @FXML private ProgressIndicator spinner;
    @FXML private Label             statusLabel;
    @FXML private Label             hintLabel;
    @FXML private Button            startBtn;

    // ── State ─────────────────────────────────────────────────────────────────
    private boolean enrollMode   = false;
    private int     targetUserId = -1;

    // ── Config ────────────────────────────────────────────────────────────────
    private static final String BACKEND_URL = "http://localhost:5003";
    private static final int    ENROLL_FRAMES = 12;
    private static final int    LOGIN_FRAMES  = 5;
    private static final Gson   GSON          = new Gson();

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        spinner.setVisible(false);
        spinner.setManaged(false);
    }

    // ── Public configuration (called before dialog.show()) ───────────────────

    public void configureEnroll(int userId) {
        this.enrollMode   = true;
        this.targetUserId = userId;
        startBtn.setText("Start Enrollment");
        hintLabel.setText("Your webcam will open automatically.\nLook straight at the camera in good lighting.");
        statusLabel.setText("Ready. Click 'Start Enrollment' to begin.");
    }

    public void configureLogin() {
        this.enrollMode = false;
        startBtn.setText("Start Face Login");
        hintLabel.setText("Your webcam will open automatically.\nLook straight at the camera in good lighting.");
        statusLabel.setText("Ready. Click 'Start Face Login' to identify yourself.");
    }

    // ── Button handler ────────────────────────────────────────────────────────

    @FXML
    private void onStart() {
        startBtn.setDisable(true);
        spinner.setVisible(true);
        spinner.setManaged(true);

        if (enrollMode) {
            statusLabel.setText("Opening webcam & capturing " + ENROLL_FRAMES + " frames…\nKeep still and face the camera.");
            runInBackground(() -> {
                try {
                    sendCaptureAndEnroll(targetUserId, ENROLL_FRAMES);
                } catch (Exception e) {
                    showStatus("Error: " + e.getMessage(), true);
                }
            });
        } else {
            statusLabel.setText("Opening webcam & capturing " + LOGIN_FRAMES + " frames…\nKeep still and face the camera.");
            runInBackground(() -> {
                try {
                    sendCaptureAndLogin(LOGIN_FRAMES);
                } catch (Exception e) {
                    showStatus("Error: " + e.getMessage(), true);
                }
            });
        }
    }

    // ── HTTP calls ────────────────────────────────────────────────────────────

    private void sendCaptureAndEnroll(int userId, int frameCount) throws IOException, InterruptedException {
        String body = "user_id=" + userId + "&frame_count=" + frameCount;
        HttpResponse<String> response = postForm(BACKEND_URL + "/capture-and-enroll", body);
        JsonObject json = GSON.fromJson(response.body(), JsonObject.class);
        boolean success = json.has("success") && json.get("success").getAsBoolean();

        if (success) {
            if (App.getCurrentUser() != null) {
                App.getCurrentUser().setFaceEmbedding("enrolled");
            }
            showStatus("✓ Face enrolled successfully!", false);
            Thread.sleep(1400);
            Platform.runLater(this::closeDialog);
        } else {
            String msg = extractDetail(json, "Enrollment failed.");
            showStatus("⚠ " + msg, true);
        }
    }

    private void sendCaptureAndLogin(int frameCount) throws IOException, InterruptedException {
        String body = "frame_count=" + frameCount;
        HttpResponse<String> response = postForm(BACKEND_URL + "/capture-and-login", body);
        JsonObject json = GSON.fromJson(response.body(), JsonObject.class);
        boolean success = json.has("success") && json.get("success").getAsBoolean();

        if (success) {
            int matchedId = json.get("user_id").getAsInt();
            showStatus("✓ Face recognized! Loading profile…", false);
            UserModel user = loadUserById(matchedId);
            if (user != null) {
                Platform.runLater(() -> {
                    try {
                        App.setCurrentUser(user);
                        closeDialog();
                        App.setRoot("profile");
                    } catch (IOException e) {
                        statusLabel.setText("Navigation error: " + e.getMessage());
                    }
                });
            } else {
                showStatus("User data not found. Try password login.", true);
            }
        } else {
            String msg = extractDetail(json, "Face not recognized.");
            String dist = json.has("best_distance") && !json.get("best_distance").isJsonNull()
                    ? String.format(" (dist=%.2f)", json.get("best_distance").getAsDouble()) : "";
            showStatus("⚠ " + msg + dist, true);
        }
    }

    private HttpResponse<String> postForm(String url, String formBody) throws IOException, InterruptedException {
        // Force HTTP/1.1 — uvicorn does not support h2c (HTTP/2 cleartext) upgrades
        // that Java's default HttpClient attempts.
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formBody))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /** Extract a readable message from any FastAPI JSON error response.
     *  FastAPI 422 puts 'detail' as a JsonArray; our 400/401 put it as a String. */
    private String extractDetail(JsonObject json, String fallback) {
        if (!json.has("detail")) return fallback;
        var detail = json.get("detail");
        if (detail.isJsonPrimitive()) return detail.getAsString();
        if (detail.isJsonArray() && detail.getAsJsonArray().size() > 0) {
            var first = detail.getAsJsonArray().get(0);
            if (first.isJsonObject() && first.getAsJsonObject().has("msg"))
                return first.getAsJsonObject().get("msg").getAsString();
            return detail.toString();
        }
        return detail.toString();
    }

    // ── DB helpers ────────────────────────────────────────────────────────────

    private UserModel loadUserById(int id) {
        try {
            Connection conn = MyDataBase.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE id = ?")) {
                stmt.setInt(1, id);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    UserModel user = new UserModel();
                    user.setId(rs.getInt("id"));
                    user.setFirstName(rs.getString("first_name"));
                    user.setLastName(rs.getString("last_name"));
                    user.setDateOfBirth(rs.getDate("date_of_birth").toLocalDate());
                    user.setBalance(rs.getDouble("balance"));
                    user.setEmail(rs.getString("email"));
                    user.setPassword(rs.getString("password"));
                    user.setIsAdmin(rs.getBoolean("is_admin"));
                    user.setProfileImg(rs.getString("profile_img"));
                    user.setBio(rs.getString("bio"));
                    try { user.setFaceEmbedding(rs.getString("face_embedding")); } catch (SQLException ignored) {}
                    return user;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    private void showStatus(String message, boolean isError) {
        Platform.runLater(() -> {
            statusLabel.setText(message);
            spinner.setVisible(false);
            spinner.setManaged(false);
            if (isError) startBtn.setDisable(false);
        });
    }

    private void runInBackground(Runnable task) {
        Thread t = new Thread(task, "face-worker");
        t.setDaemon(true);
        t.start();
    }

    private void closeDialog() {
        Stage stage = (Stage) startBtn.getScene().getWindow();
        stage.close();
    }
}

