package com.khademni.model;

import java.time.LocalDateTime;

public class OffreModel {
    private int id;
    private int userId; // Technical Primary Key (FK)
    private int userUniqueId; // Business ID (4-digit) for display
    private String titre;
    private String description;
    private double prix;
    private LocalDateTime dateCreation;
    private String statut;
    private String type; // "OFFRE" or "DEMANDE"
    private LocalDateTime dateLimite;
    private String userName; // Full name of the author (for Global Feed display)

    public OffreModel() {
    }

    public OffreModel(int id, int userId, int userUniqueId, String titre, String description, double prix,
            LocalDateTime dateCreation, String statut, String type, LocalDateTime dateLimite, String userName) {
        this.id = id;
        this.userId = userId;
        this.userUniqueId = userUniqueId;
        this.titre = titre;
        this.description = description;
        this.prix = prix;
        this.dateCreation = dateCreation;
        this.statut = statut;
        this.type = type;
        this.dateLimite = dateLimite;
        this.userName = userName;
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

    public int getUserUniqueId() {
        return userUniqueId;
    }

    public void setUserUniqueId(int userUniqueId) {
        this.userUniqueId = userUniqueId;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getPrix() {
        return prix;
    }

    public void setPrix(double prix) {
        this.prix = prix;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LocalDateTime getDateLimite() {
        return dateLimite;
    }

    public void setDateLimite(LocalDateTime dateLimite) {
        this.dateLimite = dateLimite;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
