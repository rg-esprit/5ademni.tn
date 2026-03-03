package com.khademni.model;

import java.time.LocalDate;

public class UserModel {
    private int id;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private double balance;
    private String email;
    private String password;
    private boolean isAdmin;
    private String profileImg;
    private String bio;
    private String faceEmbedding;

    public UserModel() {
    }
public UserModel(int id) {
    this.id = id;
}
    public UserModel(String firstName, String lastName, LocalDate dateOfBirth, String email, String password) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.email = email;
        this.password = password;
        this.balance = 0.0;
        this.isAdmin = false;
        this.profileImg = "";
        this.bio = "";
    }

    public UserModel(int id, String firstName, String lastName, LocalDate dateOfBirth, double balance, String email, String password, boolean isAdmin, String profileImg, String bio) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.balance = balance;
        this.email = email;
        this.password = password;
        this.isAdmin = isAdmin;
        this.profileImg = profileImg;
        this.bio = bio;
    }

    public String getFaceEmbedding() {
        return faceEmbedding;
    }

    public void setFaceEmbedding(String faceEmbedding) {
        this.faceEmbedding = faceEmbedding;
    }

    public boolean hasFaceEnrolled() {
        return faceEmbedding != null && !faceEmbedding.isBlank();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isIsAdmin() {
        return isAdmin;
    }

    public void setIsAdmin(boolean isAdmin) {
        this.isAdmin = isAdmin;
    }

    public String getProfileImg() {
        return profileImg;
    }

    public void setProfileImg(String profileImg) {
        this.profileImg = profileImg;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    @Override
    public String toString() {
        return "UserModel{id=" + id + ", firstName='" + firstName + "', lastName='" + lastName + "', email='" + email + "', balance=" + balance + ", isAdmin=" + isAdmin + ", bio='" + bio + "'}";
    }
}
