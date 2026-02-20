-- Script pour renommer la colonne 'name' en 'nom' dans la table category
-- Exécutez ce script dans MySQL Workbench ou ligne de commande

USE appdb;

-- Renommer la colonne
ALTER TABLE category CHANGE COLUMN name nom VARCHAR(255);

-- Vérifier que ça a marché
DESCRIBE category;

-- Voir les catégories
SELECT id, nom, description FROM category;

COMMIT;

