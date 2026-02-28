package com.khademni.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDataBase {

    // Database credentials
    private static final String DB_URL = "jdbc:mysql://localhost:3306/appdb";
    private static final String USER = "root";
    private static final String PASS = ""; 


    private static Connection connection = null;

    private MyDataBase() {
    }

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            synchronized (MyDataBase.class) {
                if (connection == null || connection.isClosed()) {
                    try {
                        connection = DriverManager.getConnection(DB_URL, USER, PASS);
                        System.out.println("Database connection established successfully.");
                    } catch (SQLException e) {
                        System.out.println("Connection failed: " + e.getMessage());
                        throw e;
                    }
                }
            }
        }
        return connection;
    }
 public static void closeConnection() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}
