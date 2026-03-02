package com.khademni.model;

public class ReviewModel {

    private int id;
    private int clientId;
    private int freelancerId;
    private int rating;
    private String reviewText;

    // Display-only fields (not stored in DB, populated by JOIN queries)
    private String clientFirstName;
    private String clientLastName;
    private String clientEmail;
    private String freelancerFirstName;
    private String freelancerLastName;
    private String freelancerEmail;

    public ReviewModel() {
    }

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

    public String getClientFirstName() { return clientFirstName; }
    public String getClientLastName() { return clientLastName; }
    public String getClientEmail() { return clientEmail; }
    public String getFreelancerFirstName() { return freelancerFirstName; }
    public String getFreelancerLastName() { return freelancerLastName; }
    public String getFreelancerEmail() { return freelancerEmail; }

    public String getClientFullName() {
        return (clientFirstName != null ? clientFirstName : "") + " " + (clientLastName != null ? clientLastName : "");
    }

    public String getFreelancerFullName() {
        return (freelancerFirstName != null ? freelancerFirstName : "") + " " + (freelancerLastName != null ? freelancerLastName : "");
    }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setClientId(int clientId) { this.clientId = clientId; }
    public void setFreelancerId(int freelancerId) { this.freelancerId = freelancerId; }
    public void setRating(int rating) { this.rating = rating; }
    public void setReviewText(String reviewText) { this.reviewText = reviewText; }

    public void setClientFirstName(String clientFirstName) { this.clientFirstName = clientFirstName; }
    public void setClientLastName(String clientLastName) { this.clientLastName = clientLastName; }
    public void setClientEmail(String clientEmail) { this.clientEmail = clientEmail; }
    public void setFreelancerFirstName(String freelancerFirstName) { this.freelancerFirstName = freelancerFirstName; }
    public void setFreelancerLastName(String freelancerLastName) { this.freelancerLastName = freelancerLastName; }
    public void setFreelancerEmail(String freelancerEmail) { this.freelancerEmail = freelancerEmail; }
}
