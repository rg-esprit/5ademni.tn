package com.khademni.model;

import java.time.LocalDateTime;

public class CvModel {
    private int id;
    private int userId;
    private String fileName;
    private String filePath;
    private String statut; // EN_ATTENTE, ACCEPTE, REFUSE
    private LocalDateTime dateUpload;

    public CvModel() {
    }

    public CvModel(int id, int userId, String fileName, String filePath, String statut, LocalDateTime dateUpload) {
        this.id = id;
        this.userId = userId;
        this.fileName = fileName;
        this.filePath = filePath;
        this.statut = statut;
        this.dateUpload = dateUpload;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public LocalDateTime getDateUpload() {
        return dateUpload;
    }

    public void setDateUpload(LocalDateTime dateUpload) {
        this.dateUpload = dateUpload;
    }

    @Override
    public String toString() {
        return "CvModel{id=" + id + ", userId=" + userId + ", fileName='" + fileName + "', statut='" + statut + "'}";
    }
}
