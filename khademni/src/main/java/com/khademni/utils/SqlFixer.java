package com.khademni.utils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class SqlFixer {
    public static void main(String[] args) {
        System.out.println("Applying database fix...");
        try (Connection conn = MyDataBase.getConnection();
                Statement stmt = conn.createStatement()) {

            // 1. Add unique_id column if it doesn't exist
            try {
                stmt.execute("ALTER TABLE users ADD COLUMN unique_id INT(4) UNSIGNED ZEROFILL AFTER id");
                System.out.println("Column unique_id added successfully.");
            } catch (Exception e) {
                // Column might already exist
            }

            // 1a. Add date_limite column to offres and demandes
            try {
                stmt.execute("ALTER TABLE offres ADD COLUMN date_limite DATETIME AFTER date_creation");
                stmt.execute("ALTER TABLE demandes ADD COLUMN date_limite DATETIME AFTER date_creation");
                System.out.println("Column date_limite added to offres and demandes.");
            } catch (Exception e) {
                // Columns might already exist
            }

            // 1b. Assign unique IDs to users who don't have one (Migration)
            try {
                // This is a simple migration: assign IDs starting from 1000
                ResultSet rs = stmt.executeQuery("SELECT id FROM users WHERE unique_id IS NULL OR unique_id = 0");
                int nextId = 1000;
                // Find current max unique_id to avoid collisions
                ResultSet rsMax = conn.createStatement().executeQuery("SELECT MAX(unique_id) FROM users");
                if (rsMax.next()) {
                    int maxId = rsMax.getInt(1);
                    if (maxId >= 1000)
                        nextId = maxId + 1;
                }

                while (rs.next()) {
                    int technicalId = rs.getInt("id");
                    stmt.addBatch("UPDATE users SET unique_id = " + nextId + " WHERE id = " + technicalId);
                    System.out.println("Assigned UniqueID " + nextId + " to User ID " + technicalId);
                    nextId++;
                }
                stmt.executeBatch();
            } catch (Exception e) {
                System.out.println("Migration error: " + e.getMessage());
            }

            // 1c. Add UNIQUE constraint if missing
            try {
                stmt.execute("ALTER TABLE users ADD UNIQUE (unique_id)");
            } catch (Exception e) {
                // Might already be unique
            }

            // 1d. Rename role to current_mode if it exists (compatibility fix)
            try {
                stmt.execute(
                        "ALTER TABLE users CHANGE COLUMN role current_mode ENUM('CLIENT', 'FREELANCER') NOT NULL DEFAULT 'CLIENT'");
                System.out.println("Column role renamed to current_mode successfully.");
            } catch (Exception e) {
                // Ignore if role doesn't exist
            }

            // 1e. Migrate foreign keys in other tables to use unique_id
            String[] tablesToMigrate = { "offres", "demandes", "contrats" };
            for (String table : tablesToMigrate) {
                try {
                    String userCol = table.equals("contrats") ? "client_id" : "user_id";
                    // Update user_id/client_id to technical users.id join
                    // Previously we might have migrated to unique_id, let's revert it to
                    // технический id
                    stmt.execute(
                            "UPDATE " + table + " t JOIN users u ON t." + userCol + " = u.unique_id SET t." + userCol
                                    + " = u.id WHERE u.unique_id IS NOT NULL AND u.unique_id > 0");

                    if (table.equals("contrats")) {
                        stmt.execute(
                                "UPDATE contrats t JOIN users u ON t.freelancer_id = u.unique_id SET t.freelancer_id = u.id WHERE u.unique_id IS NOT NULL AND u.unique_id > 0");
                    }
                    System.out.println("Reverted FK IDs to technical primary keys in table: " + table);
                } catch (Exception e) {
                    System.out.println("Warning: Could not migrate table " + table + ": " + e.getMessage());
                }
            }

            try {
                stmt.execute(
                        "INSERT INTO users (unique_id, current_mode, first_name, last_name, email, password) VALUES "
                                +
                                "(1234, 'CLIENT', 'Ayoub', 'Dakhli', 'dakhliayoub99@gmail.com', '"
                                + PasswordUtil.hashPassword("123456") + "'), "
                                +
                                "(5678, 'FREELANCER', 'Freelancer', 'Expert', 'free@expert.tn', '"
                                + PasswordUtil.hashPassword("123456") + "') "
                                +
                                "ON DUPLICATE KEY UPDATE password = VALUES(password)");
                System.out.println("Seed users added/updated successfully.");
            } catch (Exception e) {
                System.out.println("Error seeding users: " + e.getMessage());
            }

            // 3. Cleanup: Delete users who don't have a valid 4-digit unique_id (optional
            // safety)
            try {
                int deleted = stmt.executeUpdate(
                        "DELETE FROM users WHERE unique_id < 1000 OR unique_id > 9999 OR unique_id IS NULL");
                if (deleted > 0) {
                    System.out.println("Cleaned up " + deleted + " legacy/invalid users.");
                }
            } catch (Exception e) {
                System.out.println("Cleanup warning: " + e.getMessage());
            }

            System.out.println("Database fix completed.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
