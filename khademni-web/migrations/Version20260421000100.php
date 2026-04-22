<?php

declare(strict_types=1);

namespace DoctrineMigrations;

use Doctrine\DBAL\Schema\Schema;
use Doctrine\Migrations\AbstractMigration;

final class Version20260421000100 extends AbstractMigration
{
    public function getDescription(): string
    {
        return 'Add category slug and timestamp fields on category/gig for Gedmo sluggable and timestampable behaviors';
    }

    public function up(Schema $schema): void
    {
        // Add columns as nullable first to keep migration safe on existing rows.
        $this->addSql("ALTER TABLE category ADD slug VARCHAR(140) DEFAULT NULL, ADD created_at DATETIME DEFAULT NULL COMMENT '(DC2Type:datetime_immutable)', ADD updated_at DATETIME DEFAULT NULL COMMENT '(DC2Type:datetime_immutable)'");
        $this->addSql("ALTER TABLE gig ADD created_at DATETIME DEFAULT NULL COMMENT '(DC2Type:datetime_immutable)', ADD updated_at DATETIME DEFAULT NULL COMMENT '(DC2Type:datetime_immutable)'");

        // Backfill existing data.
        $this->addSql('UPDATE category SET created_at = NOW(), updated_at = NOW() WHERE created_at IS NULL OR updated_at IS NULL');
        $this->addSql('UPDATE gig SET created_at = NOW(), updated_at = NOW() WHERE created_at IS NULL OR updated_at IS NULL');
        $this->addSql("UPDATE category SET slug = CONCAT(REPLACE(REPLACE(REPLACE(LOWER(TRIM(name)), ' ', '-'), '/', '-'), '_', '-'), '-', id) WHERE slug IS NULL OR slug = ''");

        // Enforce final constraints.
        $this->addSql("ALTER TABLE category MODIFY slug VARCHAR(140) NOT NULL, MODIFY created_at DATETIME NOT NULL COMMENT '(DC2Type:datetime_immutable)', MODIFY updated_at DATETIME NOT NULL COMMENT '(DC2Type:datetime_immutable)'");
        $this->addSql("ALTER TABLE gig MODIFY created_at DATETIME NOT NULL COMMENT '(DC2Type:datetime_immutable)', MODIFY updated_at DATETIME NOT NULL COMMENT '(DC2Type:datetime_immutable)'");
        $this->addSql('CREATE UNIQUE INDEX UNIQ_64C19C1989D9B62 ON category (slug)');
    }

    public function down(Schema $schema): void
    {
        $this->addSql('DROP INDEX UNIQ_64C19C1989D9B62 ON category');
        $this->addSql('ALTER TABLE category DROP slug, DROP created_at, DROP updated_at');
        $this->addSql('ALTER TABLE gig DROP created_at, DROP updated_at');
    }
}
