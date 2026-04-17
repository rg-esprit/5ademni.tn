<?php

declare(strict_types=1);

namespace DoctrineMigrations;

use Doctrine\DBAL\Schema\Schema;
use Doctrine\Migrations\AbstractMigration;

final class Version20260417125000 extends AbstractMigration
{
    public function getDescription(): string
    {
        return 'Create notifications table for user alerts';
    }

    public function up(Schema $schema): void
    {
        $this->addSql('CREATE TABLE notifications (
            id INT AUTO_INCREMENT NOT NULL,
            recipient_id INT NOT NULL,
            message VARCHAR(500) NOT NULL,
            link VARCHAR(255) DEFAULT NULL,
            type VARCHAR(50) NOT NULL DEFAULT \'info\',
            is_read TINYINT(1) NOT NULL,
            created_at DATETIME NOT NULL,
            INDEX IDX_4B6FBF22996B1F9C (recipient_id),
            PRIMARY KEY(id)
        ) DEFAULT CHARACTER SET utf8mb4 COLLATE `utf8mb4_unicode_ci` ENGINE = InnoDB');

        $this->addSql('ALTER TABLE notifications ADD CONSTRAINT FK_4B6FBF22996B1F9C FOREIGN KEY (recipient_id) REFERENCES users (id) ON DELETE CASCADE');
    }

    public function down(Schema $schema): void
    {
        $this->addSql('DROP TABLE notifications');
    }
}
