package com.khademni.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class UserModel {
    private int id;
    private int uniqueId; // 4-digit ID
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private double balance;
    private String email;
    private String password;
    private boolean isAdmin;
    private String profileImage;
    private String bio;
    private UserRole currentMode;
    private String cvPath;
    private LocalDateTime cvUploadedAt;

    public UserModel() {
        this.currentMode = UserRole.CLIENT;
    }

    public UserModel(int uniqueId, String firstName, String lastName, LocalDate dateOfBirth, String email,
            String password) {
        this();
        this.uniqueId = uniqueId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.email = email;
        this.password = password;
        this.balance = 0.0;
        this.isAdmin = false;
        this.profileImage = "";
        this.bio = "";
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(int uniqueId) {
        this.uniqueId = uniqueId;
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

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public UserRole getCurrentMode() {
        return currentMode;
    }

    public void setCurrentMode(UserRole currentMode) {
        this.currentMode = currentMode;
    }

    public String getCvPath() {
        return cvPath;
    }

    public void setCvPath(String cvPath) {
        this.cvPath = cvPath;
    }

    public LocalDateTime getCvUploadedAt() {
        return cvUploadedAt;
    }

    public void setCvUploadedAt(LocalDateTime cvUploadedAt) {
        this.cvUploadedAt = cvUploadedAt;
    }

    @Override
    public String toString() {
        return "UserModel{id=" + id + ", uniqueId=" + uniqueId + ", firstName='" + firstName + "', lastName='"
                + lastName + "', email='" + email
                + "', balance=" + balance + ", isAdmin=" + isAdmin + ", mode=" + currentMode + "}";
    }
}
