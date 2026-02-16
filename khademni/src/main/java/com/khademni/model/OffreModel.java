package com.khademni.model;

import java.time.LocalDateTime;

public class OffreModel {
    private int id;
    private int userId; // Creator of the offer or demand
    private String titre;
    private String description;
    private double prix;
    private LocalDateTime dateCreation;
    private String statut;
    private String type; // "OFFRE" or "DEMANDE"

    public OffreModel() {
    }

    public OffreModel(int id, int userId, String titre, String description, double prix,
            LocalDateTime dateCreation, String statut, String type) {
        this.id = id;
        this.userId = userId;
        this.titre = titre;
        this.description = description;
        this.prix = prix;
        this.dateCreation = dateCreation;
        this.statut = statut;
        this.type = type;
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
}
