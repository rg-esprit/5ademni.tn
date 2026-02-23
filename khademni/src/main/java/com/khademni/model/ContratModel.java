package com.khademni.model;

import java.time.LocalDate;

public class ContratModel {

    private int idContrat;
    private int idClient;
    private int idFreelancer;
    private int clientUniqueId; // Business ID (4-digit)
    private int freelancerUniqueId; // Business ID (4-digit)
    private String clientName;
    private String freelancerName;
    private String titre;
    private String description;
    private double prix;
    private LocalDate dateContrat;
    private String statut;

    public ContratModel() {
    }

    public ContratModel(int idContrat, int idClient, int idFreelancer, int clientUniqueId, int freelancerUniqueId,
            String clientName, String freelancerName, String titre, String description, double prix,
            LocalDate dateContrat, String statut) {
        this.idContrat = idContrat;
        this.idClient = idClient;
        this.idFreelancer = idFreelancer;
        this.clientUniqueId = clientUniqueId;
        this.freelancerUniqueId = freelancerUniqueId;
        this.clientName = clientName;
        this.freelancerName = freelancerName;
        this.titre = titre;
        this.description = description;
        this.prix = prix;
        this.dateContrat = dateContrat;
        this.statut = statut;
    }

    public int getIdContrat() {
        return idContrat;
    }

    public void setIdContrat(int idContrat) {
        this.idContrat = idContrat;
    }

    public int getIdClient() {
        return idClient;
    }

    public void setIdClient(int idClient) {
        this.idClient = idClient;
    }

    public int getIdFreelancer() {
        return idFreelancer;
    }

    public void setIdFreelancer(int idFreelancer) {
        this.idFreelancer = idFreelancer;
    }

    public int getClientUniqueId() {
        return clientUniqueId;
    }

    public void setClientUniqueId(int clientUniqueId) {
        this.clientUniqueId = clientUniqueId;
    }

    public int getFreelancerUniqueId() {
        return freelancerUniqueId;
    }

    public void setFreelancerUniqueId(int freelancerUniqueId) {
        this.freelancerUniqueId = freelancerUniqueId;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getFreelancerName() {
        return freelancerName;
    }

    public void setFreelancerName(String freelancerName) {
        this.freelancerName = freelancerName;
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

    public LocalDate getDateContrat() {
        return dateContrat;
    }

    public void setDateContrat(LocalDate dateContrat) {
        this.dateContrat = dateContrat;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }
}
