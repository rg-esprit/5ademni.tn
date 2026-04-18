<?php

declare(strict_types=1);

namespace DoctrineMigrations;

use Doctrine\DBAL\Schema\Schema;
use Doctrine\Migrations\AbstractMigration;

/**
 * Auto-generated Migration: Please modify to your needs!
 */
final class Version20260418173129 extends AbstractMigration
{
    public function getDescription(): string
    {
        return '';
    }

    public function up(Schema $schema): void
    {
        // this up() migration is auto-generated, please modify it to your needs
        $this->addSql('ALTER TABLE gig CHANGE relevance_score relevance_score DOUBLE PRECISION DEFAULT 0');
        $this->addSql('ALTER TABLE job_applications ADD ai_match_score INT DEFAULT NULL');
        $this->addSql('ALTER TABLE notifications CHANGE is_read is_read TINYINT DEFAULT 0 NOT NULL');
        $this->addSql('ALTER TABLE notifications RENAME INDEX idx_4b6fbf22996b1f9c TO IDX_6000B0D3E92F8F78');
        $this->addSql('ALTER TABLE work_logs ADD CONSTRAINT FK_94C7764BE04EA9 FOREIGN KEY (job_id) REFERENCES jobs (id) ON DELETE CASCADE');
        $this->addSql('ALTER TABLE work_logs ADD CONSTRAINT FK_94C77648545BDF5 FOREIGN KEY (freelancer_id) REFERENCES users (id) ON DELETE CASCADE');
        $this->addSql('CREATE INDEX IDX_94C7764BE04EA9 ON work_logs (job_id)');
        $this->addSql('CREATE INDEX IDX_94C77648545BDF5 ON work_logs (freelancer_id)');
    }

    public function down(Schema $schema): void
    {
        // this down() migration is auto-generated, please modify it to your needs
        $this->addSql('ALTER TABLE gig CHANGE relevance_score relevance_score DOUBLE PRECISION DEFAULT \'0\'');
        $this->addSql('ALTER TABLE job_applications DROP ai_match_score');
        $this->addSql('ALTER TABLE notifications CHANGE is_read is_read TINYINT NOT NULL');
        $this->addSql('ALTER TABLE notifications RENAME INDEX idx_6000b0d3e92f8f78 TO IDX_4B6FBF22996B1F9C');
        $this->addSql('ALTER TABLE work_logs DROP FOREIGN KEY FK_94C7764BE04EA9');
        $this->addSql('ALTER TABLE work_logs DROP FOREIGN KEY FK_94C77648545BDF5');
        $this->addSql('DROP INDEX IDX_94C7764BE04EA9 ON work_logs');
        $this->addSql('DROP INDEX IDX_94C77648545BDF5 ON work_logs');
    }
}
