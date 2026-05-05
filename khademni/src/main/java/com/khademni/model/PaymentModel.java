package com.khademni.model;

import javafx.beans.property.*;
import java.time.LocalDateTime;

public class PaymentModel {
    private IntegerProperty id;
    private IntegerProperty contratId;
    private StringProperty stripeSessionId;
    private DoubleProperty amount;
    private StringProperty status;
    private ObjectProperty<LocalDateTime> createdAt;

    // For display
    private StringProperty contractTitle;

    public PaymentModel() {
        this.id = new SimpleIntegerProperty();
        this.contratId = new SimpleIntegerProperty();
        this.stripeSessionId = new SimpleStringProperty();
        this.amount = new SimpleDoubleProperty();
        this.status = new SimpleStringProperty("PAID");
        this.createdAt = new SimpleObjectProperty<>();
        this.contractTitle = new SimpleStringProperty();
    }

    public PaymentModel(int id, int contratId, String stripeSessionId, double amount, String status, LocalDateTime createdAt) {
        this();
        this.id.set(id);
        this.contratId.set(contratId);
        this.stripeSessionId.set(stripeSessionId);
        this.amount.set(amount);
        this.status.set(status);
        this.createdAt.set(createdAt);
    }

    public int getId() { return id.get(); }
    public void setId(int id) { this.id.set(id); }

    public int getContratId() { return contratId.get(); }
    public void setContratId(int contratId) { this.contratId.set(contratId); }

    public String getStripeSessionId() { return stripeSessionId.get(); }
    public void setStripeSessionId(String stripeSessionId) { this.stripeSessionId.set(stripeSessionId); }

    public double getAmount() { return amount.get(); }
    public void setAmount(double amount) { this.amount.set(amount); }

    public String getStatus() { return status.get(); }
    public void setStatus(String status) { this.status.set(status); }

    public LocalDateTime getCreatedAt() { return createdAt.get(); }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt.set(createdAt); }

    public String getContractTitle() { return contractTitle.get(); }
    public void setContractTitle(String contractTitle) { this.contractTitle.set(contractTitle); }
}
