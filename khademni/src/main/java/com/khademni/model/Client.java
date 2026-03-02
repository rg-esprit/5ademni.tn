package com.khademni.model;

public class Client {
    private int id;
    private int uniqueId;
    private String firstName;
    private String lastName;

    public Client(int id, int uniqueId, String firstName, String lastName) {
        this.id = id;
        this.uniqueId = uniqueId;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public int getId() {
        return id;
    }

    public int getUniqueId() {
        return uniqueId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    @Override
    public String toString() {
        return uniqueId + " - " + firstName + " " + lastName;
    }
}
