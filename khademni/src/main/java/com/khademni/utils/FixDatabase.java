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

                        stmt.execute("DROP TABLE IF EXISTS users");
                        stmt.execute("CREATE TABLE users (" +
                                        "id INT AUTO_INCREMENT PRIMARY KEY," +
                                        "unique_id INT(4) UNSIGNED ZEROFILL UNIQUE NOT NULL," +
                                        "current_mode ENUM('CLIENT', 'FREELANCER') NOT NULL DEFAULT 'CLIENT'," +
                                        "first_name VARCHAR(255) NOT NULL," +
                                        "last_name VARCHAR(255) NOT NULL," +
                                        "date_of_birth DATE," +
                                        "balance DOUBLE DEFAULT 0," +
                                        "email VARCHAR(255) UNIQUE NOT NULL," +
                                        "password VARCHAR(255) NOT NULL," +
                                        "is_admin TINYINT(1) DEFAULT 0," +
                                        "profile_image VARCHAR(255)," +
                                        "cv_path VARCHAR(255)," +
                                        "cv_uploaded_at TIMESTAMP NULL DEFAULT NULL," +
                                        "bio TEXT" +
                                        ") ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4");

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

                        // 4. Recreate cvs table (Starting from ID 0)
                        System.out.println("Reconfiguring 'cvs' table (ID starts at 0)...");
                        stmt.execute("DROP TABLE IF EXISTS cvs");
                        stmt.execute("CREATE TABLE cvs (" +
                                        "id INT AUTO_INCREMENT PRIMARY KEY," +
                                        "user_id INT NOT NULL," +
                                        "file_name VARCHAR(255) NOT NULL," +
                                        "file_path VARCHAR(500) NOT NULL," +
                                        "statut VARCHAR(50) DEFAULT 'EN_ATTENTE'," +
                                        "date_upload TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                                        ") ENGINE=InnoDB AUTO_INCREMENT=0 DEFAULT CHARSET=utf8mb4");

                        // 5. Recreate payments table
                        System.out.println("Reconfiguring 'payments' table...");
                        stmt.execute("DROP TABLE IF EXISTS payments");
                        stmt.execute("CREATE TABLE payments (" +
                                        "id INT AUTO_INCREMENT PRIMARY KEY," +
                                        "contrat_id INT NOT NULL," +
                                        "stripe_session_id VARCHAR(255) NOT NULL," +
                                        "amount DOUBLE NOT NULL," +
                                        "status VARCHAR(50) DEFAULT 'PENDING'," +
                                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

                        // 6. Recreate transactions table (deposits to user balance via Stripe)
                        System.out.println("Reconfiguring 'transactions' table...");
                        stmt.execute("DROP TABLE IF EXISTS transactions");
                        stmt.execute("CREATE TABLE transactions (" +
                                        "id INT AUTO_INCREMENT PRIMARY KEY," +
                                        "user_id INT NOT NULL," +
                                        "stripe_payment_id VARCHAR(255) NOT NULL," +
                                        "amount DOUBLE NOT NULL," +
                                        "status VARCHAR(50) NOT NULL," +
                                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                                        "INDEX idx_stripe_payment_id (stripe_payment_id)," +
                                        "INDEX idx_user_id (user_id)" +
                                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

                        // Seeding users
                        System.out.println("Seeding initial users...");
                        stmt.execute("INSERT INTO users (unique_id, current_mode, first_name, last_name, email, password) VALUES "
                                        +
                                        "(1234, 'CLIENT', 'Ayoub', 'Dakhli', 'dakhliayoub99@gmail.com', '"
                                        + PasswordUtil.hashPassword("123456") + "'), "
                                        +
                                        "(5678, 'FREELANCER', 'Freelancer', 'Expert', 'free@expert.tn', '"
                                        + PasswordUtil.hashPassword("123456") + "')");

                        // Seeding static contract
                        stmt.execute(
                                        "INSERT IGNORE INTO contrats (client_id, freelancer_id, titre, description, prix, date_contrat, statut) "
                                                        +
                                                        "VALUES (1, 2, 'Développement Web Portfolio', 'Création d\\'un site portfolio moderne avec React et Node.js', 500.0, '2026-02-14', 'EN_COURS')");

                        // Re-enable foreign key checks
                        stmt.execute("SET FOREIGN_KEY_CHECKS = 1");

                        System.out.println("Database reconfiguration completed successfully. All tables reset.");

                } catch (Exception e) {
                        System.err.println("Database reconfiguration failed!");
                        e.printStackTrace();
                }
        }
}
