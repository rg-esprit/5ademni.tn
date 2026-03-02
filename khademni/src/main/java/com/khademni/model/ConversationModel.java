package com.khademni.model;

import java.time.LocalDateTime;

public class ConversationModel {

    private int id;
    private int clientId;
    private int freelanceId;
    private LocalDateTime dateCreation;
    private String statut;

    public ConversationModel() {}

    public ConversationModel(int id, int clientId, int freelanceId, String statut) {
        this.id = id;
        this.clientId = clientId;
        this.freelanceId = freelanceId;
        this.statut = statut;
        this.dateCreation = LocalDateTime.now(); // Date par défaut
    }

    public ConversationModel(int id, int clientId, int freelanceId, String statut, LocalDateTime dateCreation) {
        this.id = id;
        this.clientId = clientId;
        this.freelanceId = freelanceId;
        this.statut = statut;
        this.dateCreation = dateCreation;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getClientId() { return clientId; }
    public void setClientId(int clientId) { this.clientId = clientId; }

    public int getFreelanceId() { return freelanceId; }
    public void setFreelanceId(int freelanceId) { this.freelanceId = freelanceId; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public String getDateCreationFormatted() {
        if (dateCreation == null) return "";
        java.time.format.DateTimeFormatter formatter =
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return dateCreation.format(formatter);
    }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    @Override
    public String toString() {
        return "Conversation #" + id + " (Client: " + clientId + ", Freelance: " + freelanceId + ")";
    }
}