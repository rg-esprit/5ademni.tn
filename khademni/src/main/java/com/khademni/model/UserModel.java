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

    public UserModel() {
    }

    public UserModel(String firstName, String lastName, LocalDate dateOfBirth, String email, String password) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.email = email;
        this.password = password;
        this.balance = 0.0;
    }

    public UserModel(int id, String firstName, String lastName, LocalDate dateOfBirth, double balance, String email, String password) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.balance = balance;
        this.email = email;
        this.password = password;
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

    @Override
    public String toString() {
        return "UserModel{id=" + id + ", firstName='" + firstName + "', lastName='" + lastName + "', email='" + email + "', balance=" + balance + "}";
    }
}
