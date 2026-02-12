package com.khademni.model;

import java.time.LocalDate;

public class ContratModel {

    private int idContrat;
    private int idClient;
    private int idFreelancer;
    private String description;
    private LocalDate dateContrat;

    public ContratModel() {
    }

    public ContratModel(int idContrat, int idClient, int idFreelancer,
                        String description, LocalDate dateContrat) {
        this.idContrat = idContrat;
        this.idClient = idClient;
        this.idFreelancer = idFreelancer;
        this.description = description;
        this.dateContrat = dateContrat;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getDateContrat() {
        return dateContrat;
    }

    public void setDateContrat(LocalDate dateContrat) {
        this.dateContrat = dateContrat;
    }
}
