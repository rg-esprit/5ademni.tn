<?php

namespace App\Entity;

use App\Repository\WorkLogRepository;
use Doctrine\DBAL\Types\Types;
use Doctrine\ORM\Mapping as ORM;

#[ORM\Entity(repositoryClass: WorkLogRepository::class)]
#[ORM\Table(name: 'work_logs')]
class WorkLog
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column]
    private ?int $id = null;

    #[ORM\ManyToOne(targetEntity: Job::class)]
    #[ORM\JoinColumn(name: 'job_id', referencedColumnName: 'id', nullable: false, onDelete: 'CASCADE')]
    private ?Job $job = null;

    #[ORM\ManyToOne(targetEntity: User::class)]
    #[ORM\JoinColumn(name: 'freelancer_id', referencedColumnName: 'id', nullable: false, onDelete: 'CASCADE')]
    private ?User $freelancer = null;

    #[ORM\Column(name: 'progress_change', type: Types::INTEGER)]
    private int $progressChange = 0;

    #[ORM\Column(type: Types::TEXT, nullable: true)]
    private ?string $description = null;

    #[ORM\Column(name: 'created_at', type: Types::DATETIME_MUTABLE, options: ['default' => 'CURRENT_TIMESTAMP'])]
    private ?\DateTimeInterface $createdAt = null;

    public function __construct()
    {
        $this->createdAt = new \DateTime();
    }

    public function getId(): ?int { return $this->id; }

    public function getJob(): ?Job { return $this->job; }
    public function setJob(?Job $job): static { $this->job = $job; return $this; }

    public function getFreelancer(): ?User { return $this->freelancer; }
    public function setFreelancer(?User $freelancer): static { $this->freelancer = $freelancer; return $this; }

    public function getProgressChange(): int { return $this->progressChange; }
    public function setProgressChange(int $progressChange): static { $this->progressChange = max(0, min(100, $progressChange)); return $this; }

    public function getDescription(): ?string { return $this->description; }
    public function setDescription(?string $description): static { $this->description = $description; return $this; }

    public function getCreatedAt(): ?\DateTimeInterface { return $this->createdAt; }
    public function setCreatedAt(\DateTimeInterface $createdAt): static { $this->createdAt = $createdAt; return $this; }
}
