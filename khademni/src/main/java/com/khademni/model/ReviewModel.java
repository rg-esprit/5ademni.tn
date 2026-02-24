package com.khademni.model;

public class ReviewModel {

    private int id;
    private int clientId;
    private int freelancerId;
    private int rating;
    private String reviewText;

    // Constructeur vide
    public ReviewModel() {
    }

    // Constructeur avec paramètres
    public ReviewModel(int id, int clientId, int freelancerId, int rating, String reviewText) {
        this.id = id;
        this.clientId = clientId;
        this.freelancerId = freelancerId;
        this.rating = rating;
        this.reviewText = reviewText;
    }

    // Getters
    public int getId() { return id; }
    public int getClientId() { return clientId; }
    public int getFreelancerId() { return freelancerId; }
    public int getRating() { return rating; }
    public String getReviewText() { return reviewText; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setClientId(int clientId) { this.clientId = clientId; }
    public void setFreelancerId(int freelancerId) { this.freelancerId = freelancerId; }
    public void setRating(int rating) { this.rating = rating; }
    public void setReviewText(String reviewText) { this.reviewText = reviewText; }
}
