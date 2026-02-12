package com.khademni.model;

import javafx.beans.property.*;
import java.time.LocalDate;

public class ContratModel {

    private SimpleIntegerProperty id;
    private SimpleIntegerProperty clientId;
    private SimpleIntegerProperty freelancerId;
    private ObjectProperty<LocalDate> dateContrat;
    private StringProperty description;

    // Constructeur vide
    public ContratModel() {
        this.id = new SimpleIntegerProperty();
        this.clientId = new SimpleIntegerProperty();
        this.freelancerId = new SimpleIntegerProperty();
        this.dateContrat = new SimpleObjectProperty<>();
        this.description = new SimpleStringProperty();
    }

    // Constructeur avec paramètres
    public ContratModel(int id, int clientId, int freelancerId, LocalDate dateContrat, String description) {
        this.id = new SimpleIntegerProperty(id);
        this.clientId = new SimpleIntegerProperty(clientId);
        this.freelancerId = new SimpleIntegerProperty(freelancerId);
        this.dateContrat = new SimpleObjectProperty<>(dateContrat);
        this.description = new SimpleStringProperty(description);
    }

    // Getters
    public int getId() { return id.get(); }
    public int getClientId() { return clientId.get(); }
    public int getFreelancerId() { return freelancerId.get(); }
    public LocalDate getDateContrat() { return dateContrat.get(); }
    public String getDescription() { return description.get(); }

    // Setters
    public void setId(int id) { this.id.set(id); }
    public void setClientId(int clientId) { this.clientId.set(clientId); }
    public void setFreelancerId(int freelancerId) { this.freelancerId.set(freelancerId); }
    public void setDateContrat(LocalDate dateContrat) { this.dateContrat.set(dateContrat); }
    public void setDescription(String description) { this.description.set(description); }

    // Properties
    public IntegerProperty idProperty() { return id; }
    public IntegerProperty clientIdProperty() { return clientId; }
    public IntegerProperty freelancerIdProperty() { return freelancerId; }
    public ObjectProperty<LocalDate> dateContratProperty() { return dateContrat; }
    public StringProperty descriptionProperty() { return description; }
}
