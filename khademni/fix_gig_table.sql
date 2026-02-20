-- Script de correction pour la table 'gig'
-- Ajoute la colonne user_id si elle n'existe pas

USE appdb;

-- Vérifier la structure actuelle
DESCRIBE gig;

-- Ajouter la colonne user_id si elle n'existe pas
-- (Si elle existe déjà, cette commande ne fera rien)
ALTER TABLE gig
ADD COLUMN IF NOT EXISTS user_id INT NOT NULL DEFAULT 0 AFTER category_id;

-- Ajouter l'index pour user_id
ALTER TABLE gig
ADD INDEX IF NOT EXISTS idx_user_id (user_id);

-- Si vous voulez ajouter la contrainte de clé étrangère (optionnel)
-- ALTER TABLE gig
-- ADD CONSTRAINT fk_gig_user
-- FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE;

-- Vérifier que la colonne est bien ajoutée
DESCRIBE gig;

-- Afficher les gigs existants
SELECT * FROM gig;

COMMIT;

