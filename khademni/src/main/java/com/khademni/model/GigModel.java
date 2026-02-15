package com.khademni.model;

import com.khademni.model.CategoryModel;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class GigModel {
    private int id;
    private String title;
    private String description;
    private double price;
    private LocalDateTime deliveryTime;
    private String image;
    private String status;
    private CategoryModel category;

    public GigModel() {}

    public GigModel(int id, String title, String description, double price, LocalDateTime deliveryTime, String image, String status) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.price = price;
        this.deliveryTime = deliveryTime;
        this.image = image;
        this.status = status;
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public double getPrice() {
        return price;
    }

    public LocalDateTime getDeliveryTime() {
        return deliveryTime;
    }

    /**
     * Retourne le delivery time formaté en jour et heure (format: "dd/MM/yyyy HH:mm")
     */
    public String getFormattedDeliveryTime() {
        if (deliveryTime == null) return "Non défini";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return deliveryTime.format(formatter);
    }

    public String getImage() {
        return image;
    }

    public String getStatus() {
        return status;
    }

    public CategoryModel getCategory() {
        return category;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public void setDeliveryTime(LocalDateTime deliveryTime) {
        this.deliveryTime = deliveryTime;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setCategory(CategoryModel category) {
        this.category = category;
    }

    // toString
    @Override
    public String toString() {
        return "GigModel{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", price=" + price +
                ", deliveryTime=" + getFormattedDeliveryTime() +
                ", image='" + image + '\'' +
                ", status='" + status + '\'' +
                ", category=" + (category != null ? category.toString() : "null") +
                '}';
    }
}
