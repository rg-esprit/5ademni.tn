package com.khademni.utils;

import com.khademni.config.ConfigManager;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class MyDataBase {
    private static final String DB_URL = ConfigManager.get("DB_URL", "jdbc:mariadb://localhost:3306/appdb");
    private static final String USER = ConfigManager.get("DB_USER", "root");
    private static final String PASS = ConfigManager.get("DB_PASS", "");
    private static Connection connection = null;

    private MyDataBase() {
    }

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
            // Add face_embedding column for face recognition feature.
            // Error code 1060 = "Duplicate column name" — safe to ignore.
            try {
                stmt.execute("ALTER TABLE users ADD COLUMN face_embedding JSON NULL");
                System.out.println("Schema update: users.face_embedding column added.");
            } catch (SQLException e) {
                if (e.getErrorCode() == 1060) {
                    System.out.println("Note: users.face_embedding already exists — OK.");
                } else {
                    System.out.println("Note: users.face_embedding fix skipped: " + e.getMessage());
                }
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
        if (connection == null || connection.isClosed()) {
            synchronized (MyDataBase.class) {
                if (connection == null || connection.isClosed()) {
                    try {
                        // Ensure driver is loaded (fallback for some runtime/module setups)
                        try {
                            Class.forName("org.mariadb.jdbc.Driver");
                        } catch (ClassNotFoundException ignored) {
                        }
                        connection = DriverManager.getConnection(DB_URL, USER, PASS);
                        checkAndFixSchema(connection);
                    } catch (SQLException e) {
                        throw e;
                    }
                }
            }
        }
        return connection;
    }

    public static void resetAutoIncrementIfEmpty(String tableName, int startValue) {
        String countQuery = "SELECT COUNT(*) FROM " + tableName;
        String resetQuery = "ALTER TABLE " + tableName + " AUTO_INCREMENT = " + startValue;
        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(countQuery);
            if (rs.next() && rs.getInt(1) == 0) {
                stmt.execute(resetQuery);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void closeConnection() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}