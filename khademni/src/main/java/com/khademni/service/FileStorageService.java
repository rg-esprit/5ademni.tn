package com.khademni.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

public class FileStorageService {

    private static final String BASE_UPLOAD_DIR = "uploads";
    private static final String PROFILE_DIR = BASE_UPLOAD_DIR + "/profile";
    private static final String CV_DIR = BASE_UPLOAD_DIR + "/cv";

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    static {
        createDirectories();
    }

    private static void createDirectories() {
        try {
            Files.createDirectories(Paths.get(PROFILE_DIR));
            Files.createDirectories(Paths.get(CV_DIR));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String storeProfileImage(File file) throws IOException {
        validateImage(file);
        String fileName = generateSecureFileName(file.getName());
        Path targetPath = Paths.get(PROFILE_DIR).resolve(fileName);
        Files.copy(file.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        return targetPath.toAbsolutePath().toString();
    }

    public static String storeCV(File file) throws IOException {
        return storeCV(file, generateSecureFileName(file.getName()));
    }

    public static String storeCV(File file, String fileName) throws IOException {
        validatePDF(file);
        Path targetPath = Paths.get(CV_DIR).resolve(fileName);
        Files.copy(file.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        return targetPath.toAbsolutePath().toString();
    }

    private static void validateImage(File file) throws IOException {
        if (file.length() > MAX_FILE_SIZE) {
            throw new IOException("File is too large (max 5MB)");
        }
        String name = file.getName().toLowerCase();
        if (!(name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png"))) {
            throw new IOException("Invalid image type (only JPG, JPEG, PNG allowed)");
        }
    }

    private static void validatePDF(File file) throws IOException {
        if (file.length() > MAX_FILE_SIZE) {
            throw new IOException("File is too large (max 5MB)");
        }
        if (!file.getName().toLowerCase().endsWith(".pdf")) {
            throw new IOException("Invalid CV format (only PDF allowed)");
        }
    }

    private static String generateSecureFileName(String originalName) {
        String extension = "";
        int i = originalName.lastIndexOf('.');
        if (i > 0) {
            extension = originalName.substring(i);
        }
        return UUID.randomUUID().toString() + extension;
    }

    public static String getProfileDir() {
        return PROFILE_DIR;
    }

    public static String getCvDir() {
        return CV_DIR;
    }
}
