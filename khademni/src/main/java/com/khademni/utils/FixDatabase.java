package com.khademni.utils;

import java.sql.Connection;
import java.sql.Statement;

public class FixDatabase {
    public static void main(String[] args) {
        System.out.println("Starting thorough database reconfiguration...");
        try (Connection conn = MyDataBase.getConnection();
                Statement stmt = conn.createStatement()) {

            // Disable foreign key checks
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0");

            /*
             * // 0. Ensure users table is correct and has test data with 7-digit IDs
             * System.out.println("Reconfiguring 'users' table with 7-digit IDs...");
             * stmt.execute("DROP TABLE IF EXISTS clients");
             * stmt.execute("DROP TABLE IF EXISTS freelancers");
             * stmt.execute("DROP TABLE IF EXISTS users");
             * stmt.execute("CREATE TABLE users (" +
             * "id INT AUTO_INCREMENT PRIMARY KEY," +
             * "role VARCHAR(50) NOT NULL," + // CLIENT or FREELANCER
             * "first_name VARCHAR(255) NOT NULL," +
             * "last_name VARCHAR(255) NOT NULL," +
             * "date_of_birth DATE," +
             * "balance DOUBLE DEFAULT 0," +
             * "email VARCHAR(255) UNIQUE NOT NULL," +
             * "password VARCHAR(255) NOT NULL," +
             * "is_admin TINYINT(1) DEFAULT 0," +
             * "profile_img VARCHAR(255)," +
             * "bio TEXT" +
             * ") ENGINE=InnoDB AUTO_INCREMENT=1000000 DEFAULT CHARSET=utf8mb4");
             * 
             * // SHA-256 hash of '123456'
             * String userHash =
             * "8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92";
             * // SHA-256 hash of 'password' (for test accounts)
             * String testHash =
             * "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8";
             * 
             * stmt.execute(
             * "INSERT IGNORE INTO users (id, role, first_name, last_name, email, password) "
             * +
             * "VALUES (1000000, 'CLIENT', 'Client', 'Test', 'client@khademni.tn', '"
             * + testHash + "')");
             * stmt.execute(
             * "INSERT IGNORE INTO users (id, role, first_name, last_name, email, password) "
             * +
             * "VALUES (1000001, 'FREELANCER', 'Ayoub', 'Dakhli', 'dakhliayoub99@gmail.com', '"
             * + userHash + "')");
             * stmt.execute(
             * "INSERT IGNORE INTO users (id, role, first_name, last_name, email, password) "
             * +
             * "VALUES (1000002, 'FREELANCER', 'Freelancer', 'Test', 'freelancer@khademni.tn', '"
             * + testHash + "')");
             */

            // 1. Recreate contrats table (Starting from ID 0)
            System.out.println("Reconfiguring 'contrats' table (ID starts at 0)...");
            stmt.execute("DROP TABLE IF EXISTS contrats");
            stmt.execute("CREATE TABLE contrats (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "client_id INT NOT NULL," +
                    "freelancer_id INT NOT NULL," +
                    "titre VARCHAR(255) NOT NULL," +
                    "description TEXT NOT NULL," +
                    "prix DOUBLE NOT NULL," +
                    "date_contrat DATE NOT NULL," +
                    "statut VARCHAR(50) DEFAULT 'EN_ATTENTE'," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ") ENGINE=InnoDB AUTO_INCREMENT=0 DEFAULT CHARSET=utf8mb4");

            // 2. Recreate offres table (Starting from ID 0)
            System.out.println("Reconfiguring 'offres' table (ID starts at 0)...");
            stmt.execute("DROP TABLE IF EXISTS offres");
            stmt.execute("CREATE TABLE offres (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "user_id INT NOT NULL," +
                    "titre VARCHAR(255) NOT NULL," +
                    "description TEXT NOT NULL," +
                    "prix DOUBLE NOT NULL," +
                    "date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "statut VARCHAR(50) DEFAULT 'ACTIF'" +
                    ") ENGINE=InnoDB AUTO_INCREMENT=0 DEFAULT CHARSET=utf8mb4");

            // 3. Recreate demandes table (Starting from ID 0)
            System.out.println("Reconfiguring 'demandes' table (ID starts at 0)...");
            stmt.execute("DROP TABLE IF EXISTS demandes");
            stmt.execute("CREATE TABLE demandes (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "user_id INT NOT NULL," +
                    "titre VARCHAR(255) NOT NULL," +
                    "description TEXT NOT NULL," +
                    "prix DOUBLE NOT NULL," +
                    "date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "statut VARCHAR(50) DEFAULT 'ACTIF'" +
                    ") ENGINE=InnoDB AUTO_INCREMENT=0 DEFAULT CHARSET=utf8mb4");

            // 4. Insert static example contract
            System.out.println("Seeding static contract...");
            stmt.execute(
                    "INSERT INTO contrats (client_id, freelancer_id, titre, description, prix, date_contrat, statut) "
                            +
                            "VALUES (1000000, 1000001, 'Développement Web Portfolio', 'Création d\\'un site portfolio moderne avec React et Node.js', 500.0, '2026-02-14', 'EN_COURS')");

            // Re-enable foreign key checks
            stmt.execute("SET FOREIGN_KEY_CHECKS = 1");

            System.out.println("Database reconfiguration completed successfully. All tables reset.");

        } catch (Exception e) {
            System.err.println("Database reconfiguration failed!");
            e.printStackTrace();
        }
    }
}
