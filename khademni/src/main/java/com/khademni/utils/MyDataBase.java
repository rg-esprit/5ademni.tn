package com.khademni.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class MyDataBase {

    // Database credentials
    private static final String DB_URL = "jdbc:mysql://localhost:3306/appdb";
    private static final String USER = "root";
    private static final String PASS = "";

    private static Connection connection = null;

    private static void checkAndFixSchema(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            // Ensure jobs.posted_date is a DATETIME or TIMESTAMP to store time information
            // and fix the "21 hours ago" issue (caused by plain DATE type truncating time)
            try {
                stmt.execute("ALTER TABLE jobs MODIFY posted_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
                System.out.println("Schema update: jobs.posted_date modified to TIMESTAMP.");
            } catch (SQLException e) {
                // Already correct or lacks privileges
                System.out.println("Note: jobs.posted_date fix skipped (likely already correct).");
            }

            try {
                stmt.execute(
                        "ALTER TABLE job_applications MODIFY application_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
                System.out.println("Schema update: job_applications.application_date modified to TIMESTAMP.");
            } catch (SQLException e) {
                System.out.println("Note: job_applications.application_date fix skipped.");
            }
        } catch (SQLException e) {
            System.err.println("Error during schema check: " + e.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            synchronized (MyDataBase.class) {
                if (connection == null || connection.isClosed()) {
                    try {
                        connection = DriverManager.getConnection(DB_URL, USER, PASS);
                        System.out.println("Database connection established successfully.");
                        checkAndFixSchema(connection);
                    } catch (SQLException e) {
                        System.out.println("Connection failed: " + e.getMessage());
                        throw e;
                    }
                }
            }
        }
        return connection;
    }

}
