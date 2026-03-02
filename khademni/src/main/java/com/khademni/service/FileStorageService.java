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

    public static String storeCV(File file, int userId) throws IOException {
        validateCV(file);

        // Find existing CV for the user and delete it to prevent orphaned files
        deleteExistingCV(userId);

        String extension = getFileExtension(file.getName());
        String fileName = "cv_user_" + userId + extension;
        Path targetPath = Paths.get(CV_DIR).resolve(fileName);
        Files.copy(file.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        return targetPath.toAbsolutePath().toString();
    }

    private static void deleteExistingCV(int userId) {
        String baseName = "cv_user_" + userId;
        File dir = new File(CV_DIR);
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles((d, name) -> name.startsWith(baseName + "."));
            if (files != null) {
                for (File f : files) {
                    f.delete();
                }
            }
        }
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

    private static void validateCV(File file) throws IOException {
        long sizeBytes = file.length();
        System.out.println("Processing file: " + file.getName() + ", Size: " + sizeBytes + " bytes");

        if (sizeBytes > MAX_FILE_SIZE) {
            double sizeInMB = sizeBytes / (1024.0 * 1024.0);
            throw new IOException(String.format("File is too large (%.2f MB). Maximum allowed size is 5MB.", sizeInMB));
        }

        String name = file.getName().toLowerCase();
        if (!(name.endsWith(".pdf") || name.endsWith(".doc") || name.endsWith(".docx"))) {
            throw new IOException("Invalid CV format (only PDF, DOC, DOCX allowed)");
        }
    }

    private static String getFileExtension(String fileName) {
        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            return fileName.substring(i);
        }
        return "";
    }

    private static String generateSecureFileName(String originalName) {
        return UUID.randomUUID().toString() + getFileExtension(originalName);
    }

    public static String getProfileDir() {
        return PROFILE_DIR;
    }

    public static String getCvDir() {
        return CV_DIR;
    }
}
