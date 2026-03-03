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

    // List of tables is checked/fixed on the first call to getConnection by another
    // part of the system
    // or we can add an explicit init method.
    public static void init() {
        try (Connection conn = getConnection()) {
            checkAndFixSchema(conn);
        } catch (SQLException e) {
            System.err.println("Database initialization failed: " + e.getMessage());
        }
    }

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
            // ensure saved_jobs table exists so users can keep track of favorites
            try {
                stmt.execute("CREATE TABLE IF NOT EXISTS saved_jobs ("
                        + "user_id INT NOT NULL, "
                        + "job_id INT NOT NULL, "
                        + "saved_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                        + "PRIMARY KEY(user_id, job_id))");
                System.out.println("Schema update: ensured saved_jobs table exists.");
            } catch (SQLException e) {
                System.out.println("Note: saved_jobs table creation skipped or already exists: " + e.getMessage());
            }

            // progress tracking columns
            try {
                stmt.execute("ALTER TABLE jobs ADD COLUMN IF NOT EXISTS progress INT DEFAULT 0");
                stmt.execute("ALTER TABLE jobs ADD COLUMN IF NOT EXISTS status VARCHAR(50) DEFAULT 'OPEN'");
                stmt.execute("ALTER TABLE jobs ADD COLUMN IF NOT EXISTS accepted_freelancer_id INT DEFAULT NULL");
                System.out.println("Schema update: ensured progress, status and accepted_freelancer_id columns exist.");
            } catch (SQLException e) {
                System.out.println("Note: progress/status columns already exist or update failed.");
            }

            // work logs table
            try {
                stmt.execute("CREATE TABLE IF NOT EXISTS work_logs ("
                        + "id INT AUTO_INCREMENT PRIMARY KEY, "
                        + "job_id INT NOT NULL, "
                        + "freelancer_id INT NOT NULL, "
                        + "progress_change INT NOT NULL, "
                        + "description TEXT, "
                        + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                        + "FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE)");
                System.out.println("Schema update: ensured work_logs table exists.");
            } catch (SQLException e) {
                System.out.println("Note: work_logs table creation failed: " + e.getMessage());
            }
            // phone number for SMS notifications
            try {
                stmt.execute("ALTER TABLE job_applications ADD COLUMN IF NOT EXISTS phone_number VARCHAR(20)");
                System.out.println("Schema update: ensured phone_number column exists in job_applications.");
            } catch (SQLException e) {
                System.out.println("Note: phone_number column already exists or update failed.");
            }
        } catch (SQLException e) {
            System.err.println("Error during schema check: " + e.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        try {
            Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
            // System.out.println("Database connection established successfully.");
            // We could call checkAndFixSchema(conn) here if we want absolute assurance,
            // but it's better to do it once at startup. For safety in this fix,
            // we'll keep it but optimize it or just rely on the new connection for
            // stability.
            // checkAndFixSchema(conn);
            return conn;
        } catch (SQLException e) {
            System.out.println("Connection failed: " + e.getMessage());
            throw e;
        }
    }
 public static void closeConnection() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}
