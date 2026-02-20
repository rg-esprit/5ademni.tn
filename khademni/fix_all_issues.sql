-- ========================================
-- SCRIPT DE CORRECTION COMPLET
-- Exécutez ce script pour corriger tous les problèmes
-- ========================================

USE appdb;

-- ===== ÉTAPE 1 : Vérifier la structure actuelle =====
SELECT '===== Structure actuelle de category =====' as Info;
DESCRIBE category;

SELECT '===== Structure actuelle de gig =====' as Info;
DESCRIBE gig;

-- ===== ÉTAPE 2 : Ajouter user_id à gig si manquante =====
SELECT '===== Ajout de user_id à gig =====' as Info;

-- Pour MySQL 5.7+ (supporte IF NOT EXISTS)
-- ALTER TABLE gig ADD COLUMN IF NOT EXISTS user_id INT NOT NULL DEFAULT 0 AFTER category_id;

-- Pour toutes versions de MySQL (ignore l'erreur si la colonne existe déjà)
SET @dbname = DATABASE();
SET @tablename = 'gig';
SET @columnname = 'user_id';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (column_name = @columnname)
  ) > 0,
  "SELECT 1",
  CONCAT("ALTER TABLE ", @tablename, " ADD COLUMN ", @columnname, " INT NOT NULL DEFAULT 0 AFTER category_id")
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- ===== ÉTAPE 3 : Mettre à jour les gigs sans user_id =====
SELECT '===== Mise à jour des gigs sans user_id =====' as Info;

-- Remplacer 4 par l'ID de votre utilisateur (celui qui est connecté)
UPDATE gig SET user_id = 4 WHERE user_id = 0 OR user_id IS NULL;

-- ===== ÉTAPE 4 : Vérification finale =====
SELECT '===== Vérification finale =====' as Info;

-- Structure mise à jour
DESCRIBE gig;

-- Données dans category
SELECT '===== Catégories =====' as Info;
SELECT id, name, description, is_active FROM category;

-- Données dans gig
SELECT '===== Gigs =====' as Info;
SELECT g.id, g.title, g.price, g.status, g.user_id, c.name as category_name
FROM gig g
LEFT JOIN category c ON g.category_id = c.id;

-- Statistiques
SELECT '===== Statistiques =====' as Info;
SELECT
    COUNT(*) as total_gigs,
    SUM(CASE WHEN status = 'active' THEN 1 ELSE 0 END) as active_gigs,
    SUM(CASE WHEN status = 'inactive' THEN 1 ELSE 0 END) as inactive_gigs,
    COUNT(DISTINCT user_id) as total_users,
    COUNT(DISTINCT category_id) as categories_used
FROM gig;

COMMIT;

SELECT '===== ✓ CORRECTION TERMINÉE ! =====' as Info;

