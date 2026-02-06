package com.khademni.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDataBase {
    private static MyDataBase instance;
    private Connection connection;

    private MyDataBase() {
        try {
            // Establish a connection to the database
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/appdb", "root", "");
            System.out.println("Database connection established successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public static MyDataBase getInstance() {
        if (instance == null) {
            instance = new MyDataBase();
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }
}
