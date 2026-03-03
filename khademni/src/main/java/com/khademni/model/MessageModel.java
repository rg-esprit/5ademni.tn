package com.khademni.model;

import java.time.LocalDateTime;

public class MessageModel {

    private int id;
    private String contenu;
    private LocalDateTime dateEnvoi;
    private String expediteur;
    private String pieceJointeUrl;
    private int conversationId;
    private String typeMessage; // "TEXTE", "IMAGE", "AUDIO", "FICHIER"
    private String dureeAudio;   // Pour les messages vocaux

    public MessageModel() {}

    public MessageModel(int id, String contenu, LocalDateTime dateEnvoi, String expediteur, int conversationId) {
        this.id = id;
        this.contenu = contenu;
        this.dateEnvoi = dateEnvoi;
        this.expediteur = expediteur;
        this.conversationId = conversationId;
        this.typeMessage = "TEXTE";
    }

    public MessageModel(int id, String contenu, LocalDateTime dateEnvoi, String expediteur,
                        String pieceJointeUrl, int conversationId) {
        this.id = id;
        this.contenu = contenu;
        this.dateEnvoi = dateEnvoi;
        this.expediteur = expediteur;
        this.pieceJointeUrl = pieceJointeUrl;
        this.conversationId = conversationId;
        this.typeMessage = determinerTypeMessage(pieceJointeUrl);
    }

    public MessageModel(int id, String contenu, LocalDateTime dateEnvoi, String expediteur,
                        String pieceJointeUrl, int conversationId, String typeMessage) {
        this.id = id;
        this.contenu = contenu;
        this.dateEnvoi = dateEnvoi;
        this.expediteur = expediteur;
        this.pieceJointeUrl = pieceJointeUrl;
        this.conversationId = conversationId;
        this.typeMessage = typeMessage;
    }

    private String determinerTypeMessage(String url) {
        if (url == null || url.isEmpty()) return "TEXTE";
        if (url.contains("/images/")) return "IMAGE";
        if (url.contains("/audio/")) return "AUDIO";
        return "FICHIER";
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }

    public LocalDateTime getDateEnvoi() { return dateEnvoi; }
    public void setDateEnvoi(LocalDateTime dateEnvoi) { this.dateEnvoi = dateEnvoi; }

    public String getDateEnvoiFormatted() {
        if (dateEnvoi == null) return "";
        java.time.format.DateTimeFormatter formatter =
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        return dateEnvoi.format(formatter);
    }

    public String getExpediteur() { return expediteur; }
    public void setExpediteur(String expediteur) { this.expediteur = expediteur; }

    public String getPieceJointeUrl() { return pieceJointeUrl; }
    public void setPieceJointeUrl(String pieceJointeUrl) {
        this.pieceJointeUrl = pieceJointeUrl;
        if (this.typeMessage == null) {
            this.typeMessage = determinerTypeMessage(pieceJointeUrl);
        }
    }

    public boolean hasPieceJointe() {
        return pieceJointeUrl != null && !pieceJointeUrl.isEmpty();
    }

    public int getConversationId() { return conversationId; }
    public void setConversationId(int conversationId) { this.conversationId = conversationId; }

    public String getTypeMessage() { return typeMessage; }
    public void setTypeMessage(String typeMessage) { this.typeMessage = typeMessage; }

    public String getDureeAudio() { return dureeAudio; }
    public void setDureeAudio(String dureeAudio) { this.dureeAudio = dureeAudio; }

    public String getIconeType() {
        if (typeMessage == null) return "💬";
        switch (typeMessage) {
            case "IMAGE": return "📷";
            case "AUDIO": return "🎤";
            case "FICHIER": return "📎";
            default: return "💬";
        }
    }

    @Override
    public String toString() {
        return getIconeType() + " " + expediteur + ": " + contenu;
    }
}