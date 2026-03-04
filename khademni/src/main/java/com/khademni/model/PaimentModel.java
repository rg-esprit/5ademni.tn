package com.khademni.model;

import javafx.beans.property.*;
import java.time.LocalDate;

public class PaimentModel {
    private IntegerProperty id;
    private IntegerProperty contratId;
    private DoubleProperty montant;
    private ObjectProperty<LocalDate> datePaiement;
    private StringProperty methode; // Flouci

    public PaimentModel() {
        this.id = new SimpleIntegerProperty();
        this.contratId = new SimpleIntegerProperty();
        this.montant = new SimpleDoubleProperty();
        this.datePaiement = new SimpleObjectProperty<>();
        this.methode = new SimpleStringProperty();
    }

    public PaimentModel(int contratId, double montant) {
        this.id = new SimpleIntegerProperty();
        this.contratId = new SimpleIntegerProperty(contratId);
        this.montant = new SimpleDoubleProperty(montant);
        this.datePaiement = new SimpleObjectProperty<>(LocalDate.now());
        this.methode = new SimpleStringProperty("Flouci");
    }

    public int getContratId() { return contratId.get(); }
    public double getMontant() { return montant.get(); }
    public String getMethode() { return methode.get(); }
}
