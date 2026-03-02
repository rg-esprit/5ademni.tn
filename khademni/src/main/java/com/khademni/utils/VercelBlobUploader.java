package com.khademni.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Uploads files directly to Vercel Blob storage and returns the blob URL.
 * Uses the Vercel Blob REST API with a read-write token.
 * The store is configured with <b>private</b> access.
 */
public class VercelBlobUploader {

    private static final String BLOB_READ_WRITE_TOKEN =
            "vercel_blob_rw_wsE98pP6R5irr1E7_16ktc0GoGArRc4SfxV1GMtj1GymGqM";

    private static final String BLOB_API_URL = "https://vercel.com/api/blob";

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    /**
     * Uploads a local file to Vercel Blob and returns the blob URL.
     *
     * @param file the local image file to upload
     * @return the blob URL (private — use {@link #download(String)} to fetch content)
     * @throws Exception if the upload fails
     */
    public static String upload(File file) throws Exception {
        Path filePath = file.toPath();
        byte[] fileBytes = Files.readAllBytes(filePath);
        String contentType = Files.probeContentType(filePath);
        if (contentType == null) contentType = "application/octet-stream";

        // Generate a unique pathname to avoid collisions
        String extension = "";
        String name = file.getName();
        int dotIdx = name.lastIndexOf('.');
        if (dotIdx > 0) extension = name.substring(dotIdx);
        String pathname = "avatars/" + UUID.randomUUID() + extension;

        // Vercel Blob REST API: pathname is a query parameter
        String apiUrl = BLOB_API_URL + "/?pathname=" + pathname;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Authorization", "Bearer " + BLOB_READ_WRITE_TOKEN)
                .header("x-api-version", "12")
                .header("x-vercel-blob-access", "private")
                .header("x-content-type", contentType)
                .header("Content-Type", "application/octet-stream")
                .PUT(HttpRequest.BodyPublishers.ofByteArray(fileBytes))
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200 && response.statusCode() != 201) {
            throw new RuntimeException("Vercel Blob upload failed (HTTP " + response.statusCode() + "): " + response.body());
        }

        // Parse the JSON response — store the canonical url (not downloadUrl)
        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        return json.get("url").getAsString();
    }

    /**
     * Downloads a private blob's content as raw bytes.
     * The blob store is private, so direct URL access returns 403.
     * This method adds the bearer token to authenticate the request.
     *
     * @param blobUrl the blob URL (e.g. https://xxx.private.blob.vercel-storage.com/...)
     * @return the raw image bytes
     * @throws Exception if the download fails
     */
    public static byte[] download(String blobUrl) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(blobUrl))
                .header("Authorization", "Bearer " + BLOB_READ_WRITE_TOKEN)
                .GET()
                .build();

        HttpResponse<byte[]> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Vercel Blob download failed (HTTP " + response.statusCode() + ")");
        }
        return response.body();
    }

    /**
     * Convenience: returns a JavaFX-compatible {@link InputStream} for a private blob URL.
     */
    public static InputStream downloadAsStream(String blobUrl) throws Exception {
        return new ByteArrayInputStream(download(blobUrl));
    }

    /** Quick CLI test: java VercelBlobUploader <path-to-file> */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: VercelBlobUploader <file-path>");
            System.exit(1);
        }
        File f = new File(args[0]);
        if (!f.exists()) {
            System.out.println("File not found: " + f.getAbsolutePath());
            System.exit(1);
        }
        try {
            System.out.println("Uploading " + f.getName() + " (" + f.length() + " bytes) ...");
            String url = upload(f);
            System.out.println("SUCCESS! Blob URL: " + url);

            // Test download
            System.out.println("Downloading back via API...");
            byte[] data = download(url);
            System.out.println("Downloaded " + data.length + " bytes ✓");
        } catch (Exception e) {
            System.err.println("FAILED: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
