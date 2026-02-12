-- Création de la base de données si elle n'existe pas
CREATE DATABASE IF NOT EXISTS appdb;
USE appdb;

-- Création de la table contrats
CREATE TABLE IF NOT EXISTS contrats (
    id INT AUTO_INCREMENT PRIMARY KEY,
    client_id INT NOT NULL,
    freelancer_id INT NOT NULL,
    description TEXT NOT NULL,
    date_contrat DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Insertion de données de test (optionnel)
INSERT INTO contrats (client_id, freelancer_id, description, date_contrat) 
VALUES (1, 2, 'Contrat de développement site web', '2026-03-01');
