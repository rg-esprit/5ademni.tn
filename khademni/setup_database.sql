-- Script de création des tables pour l'application Khademni
-- Exécutez ce script dans MySQL pour créer/vérifier vos tables

USE appdb;

-- ===== CRÉATION DE LA TABLE CATEGORY =====
CREATE TABLE IF NOT EXISTS category (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nom VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===== INSERTION DE CATÉGORIES DE TEST =====
INSERT INTO category (nom, description) VALUES
('Web Development', 'Services de développement web, création de sites et applications'),
('Graphic Design', 'Design graphique, logos, bannières et créations visuelles'),
('Writing & Translation', 'Rédaction de contenu, traduction et services linguistiques'),
('Digital Marketing', 'Marketing digital, SEO, publicité en ligne et réseaux sociaux'),
('Video & Animation', 'Montage vidéo, animation et production multimédia'),
('Music & Audio', 'Production musicale, voix-off et services audio'),
('Programming & Tech', 'Développement logiciel, scripts et solutions techniques'),
('Business', 'Conseil en affaires, plans d\'affaires et services entrepreneuriaux'),
('Lifestyle', 'Coaching, conseils personnels et services de bien-être'),
('Data', 'Analyse de données, visualisation et services de data science')
ON DUPLICATE KEY UPDATE nom=VALUES(nom);

-- ===== VÉRIFIER LES CATÉGORIES =====
SELECT * FROM category;

-- ===== CRÉATION DE LA TABLE GIG (si pas encore créée) =====
CREATE TABLE IF NOT EXISTS gig (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    delivery_time DATETIME NOT NULL,
    image VARCHAR(500),
    status VARCHAR(50) DEFAULT 'active',
    category_id INT,
    user_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (category_id) REFERENCES category(id) ON DELETE SET NULL,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,

    INDEX idx_user_id (user_id),
    INDEX idx_category_id (category_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===== INSERTION DE GIGS DE TEST (remplacez user_id par un ID valide) =====
-- Décommentez et modifiez user_id selon votre base de données
/*
INSERT INTO gig (title, description, price, delivery_time, image, status, category_id, user_id) VALUES
(
    'Je vais créer un site web moderne et responsive',
    'Je créerai pour vous un site web professionnel et responsive avec les dernières technologies. Inclut design moderne, optimisation SEO et support mobile.',
    500.00,
    DATE_ADD(NOW(), INTERVAL 7 DAY),
    'https://via.placeholder.com/320x180/4f46e5/ffffff?text=Web+Development',
    'active',
    1,
    4  -- Remplacez par votre user_id
),
(
    'Je vais designer votre logo professionnel',
    'Création de logo unique et professionnel pour votre entreprise. 3 propositions, révisions illimitées, fichiers source inclus.',
    150.00,
    DATE_ADD(NOW(), INTERVAL 3 DAY),
    'https://via.placeholder.com/320x180/10b981/ffffff?text=Logo+Design',
    'active',
    2,
    4  -- Remplacez par votre user_id
),
(
    'Je vais rédiger vos articles de blog en français',
    'Rédaction d\'articles de blog optimisés SEO en français. 1000 mots minimum, recherche approfondie, ton professionnel.',
    75.00,
    DATE_ADD(NOW(), INTERVAL 2 DAY),
    'https://via.placeholder.com/320x180/f59e0b/ffffff?text=Blog+Writing',
    'active',
    3,
    4  -- Remplacez par votre user_id
);
*/

-- ===== VÉRIFIER LES GIGS =====
SELECT g.*, c.nom as category_name
FROM gig g
LEFT JOIN category c ON g.category_id = c.id
ORDER BY g.created_at DESC;

-- ===== STATISTIQUES =====
SELECT
    COUNT(*) as total_gigs,
    SUM(CASE WHEN status = 'active' THEN 1 ELSE 0 END) as active_gigs,
    SUM(CASE WHEN status = 'inactive' THEN 1 ELSE 0 END) as inactive_gigs,
    SUM(price) as total_revenue
FROM gig;

SELECT c.nom, COUNT(g.id) as gig_count
FROM category c
LEFT JOIN gig g ON c.id = g.category_id
GROUP BY c.id, c.nom
ORDER BY gig_count DESC;

COMMIT;

