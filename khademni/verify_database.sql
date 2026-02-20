-- Script de vérification et création de tables pour l'application Khademni

-- Vérifier la base de données
USE appdb;

-- Afficher les tables existantes
SHOW TABLES;

-- Vérifier la structure de la table category
DESCRIBE category;

-- Vérifier la structure de la table gig
DESCRIBE gig;

-- Vérifier s'il y a des données dans category
SELECT * FROM category LIMIT 10;

-- Vérifier s'il y a des données dans gig
SELECT * FROM gig LIMIT 10;

-- Si la table category n'existe pas ou n'a pas les bonnes colonnes, créer/modifier :

-- Option 1 : Si la table existe avec 'name' au lieu de 'nom'
-- ALTER TABLE category CHANGE COLUMN name nom VARCHAR(255);

-- Option 2 : Si la table n'existe pas du tout
-- CREATE TABLE IF NOT EXISTS category (
--     id INT AUTO_INCREMENT PRIMARY KEY,
--     nom VARCHAR(255) NOT NULL,
--     description TEXT,
--     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
-- );

-- Option 3 : Si besoin d'ajouter des données de test
-- INSERT INTO category (nom, description) VALUES
-- ('Web Development', 'Services de développement web'),
-- ('Graphic Design', 'Services de design graphique'),
-- ('Writing', 'Services de rédaction'),
-- ('Marketing', 'Services de marketing digital'),
-- ('Video Editing', 'Services de montage vidéo');

-- Vérifier la structure de la table gig
-- CREATE TABLE IF NOT EXISTS gig (
--     id INT AUTO_INCREMENT PRIMARY KEY,
--     title VARCHAR(255) NOT NULL,
--     description TEXT,
--     price DECIMAL(10,2) NOT NULL,
--     delivery_time DATETIME NOT NULL,
--     image VARCHAR(500),
--     status VARCHAR(50) DEFAULT 'active',
--     category_id INT,
--     user_id INT NOT NULL,
--     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
--     FOREIGN KEY (category_id) REFERENCES category(id),
--     FOREIGN KEY (user_id) REFERENCES user(id)
-- );

