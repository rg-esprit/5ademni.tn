<?php

namespace App\Entity;

use App\Repository\JobRepository;
use Doctrine\Common\Collections\ArrayCollection;
use Doctrine\Common\Collections\Collection;
use Doctrine\DBAL\Types\Types;
use Doctrine\ORM\Mapping as ORM;
use Symfony\Component\Validator\Constraints as Assert;

#[ORM\Entity(repositoryClass: JobRepository::class)]
#[ORM\Table(name: 'jobs')]
#[ORM\Index(name: 'fk_jobs_user', columns: ['user_id'])]
class Job
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column]
    private ?int $id = null;

    #[ORM\Column(length: 255)]
    #[Assert\NotBlank(message: 'Please enter a job title.')]
    #[Assert\Length(min: 5, max: 100, minMessage: 'Title must be at least {{ limit }} characters.', maxMessage: 'Title cannot exceed {{ limit }} characters.')]
    private ?string $title = null;

    #[ORM\Column(length: 255)]
    #[Assert\NotBlank(message: 'Please enter a company name.')]
    #[Assert\Length(min: 2, max: 100, minMessage: 'Company name must be at least {{ limit }} characters.')]
    private ?string $company = null;

    #[ORM\Column(length: 255)]
    #[Assert\NotBlank(message: 'Please enter a location.')]
    #[Assert\Length(min: 2, max: 100)]
    private ?string $location = null;

    #[ORM\Column(type: Types::TEXT)]
    #[Assert\NotBlank(message: 'Please enter a job description.')]
    #[Assert\Length(min: 20, minMessage: 'Description must be at least {{ limit }} characters.')]
    private ?string $description = null;

    #[ORM\Column(length: 100)]
    #[Assert\NotBlank(message: 'Please select a category.')]
    private ?string $category = null;

    #[ORM\Column(name: 'salary_range', length: 100, nullable: true)]
    private ?string $salaryRange = null;

    private ?string $minSalary = null;

    private ?string $maxSalary = null;

    #[ORM\Column(length: 20, options: ['default' => 'OPEN'])]
    private string $status = 'OPEN';

    #[ORM\Column(name: 'job_type', length: 50, nullable: true)]
    #[Assert\NotBlank(message: 'Please select a job type.')]
    private ?string $jobType = null;

    #[ORM\Column(name: 'posted_date', type: Types::DATETIME_MUTABLE, options: ['default' => 'CURRENT_TIMESTAMP'])]
    private ?\DateTimeInterface $postedDate = null;

    #[ORM\Column(type: Types::TEXT, nullable: true)]
    private ?string $requirements = null;

    #[ORM\ManyToOne(targetEntity: User::class)]
    #[ORM\JoinColumn(name: 'user_id', referencedColumnName: 'id', nullable: false, onDelete: 'CASCADE')]
    private ?User $user = null;

    /**
     * @var Collection<int, JobMilestone>
     */
    #[ORM\OneToMany(mappedBy: 'job', targetEntity: JobMilestone::class, cascade: ['persist', 'remove'])]
    private Collection $milestones;

    public function __construct()
    {
        $this->postedDate = new \DateTime();
        $this->applications = new ArrayCollection();
        $this->savedByUsers = new ArrayCollection();
        $this->milestones = new ArrayCollection();
    }

    public function getId(): ?int
    {
        return $this->id;
    }

    public function getTitle(): ?string
    {
        return $this->title;
    }

    public function setTitle(string $title): static
    {
        $this->title = $title;

        return $this;
    }

    public function getCompany(): ?string
    {
        return $this->company;
    }

    public function setCompany(string $company): static
    {
        $this->company = $company;

        return $this;
    }

    public function getLocation(): ?string
    {
        return $this->location;
    }

    public function setLocation(string $location): static
    {
        $this->location = $location;

        return $this;
    }

    public function getDescription(): ?string
    {
        return $this->description;
    }

    public function setDescription(string $description): static
    {
        $this->description = $description;

        return $this;
    }

    public function getCategory(): ?string
    {
        return $this->category;
    }

    public function setCategory(string $category): static
    {
        $this->category = $category;

        return $this;
    }

    public function getSalaryRange(): ?string
    {
        return $this->salaryRange;
    }

    public function setSalaryRange(?string $salaryRange): static
    {
        $this->salaryRange = $salaryRange;
        $this->hydrateSalaryBoundsFromRange();

        return $this;
    }

    public function getMinSalary(): ?string
    {
        $this->hydrateSalaryBoundsFromRange();

        return $this->minSalary;
    }

    public function setMinSalary(?string $minSalary): static
    {
        $this->minSalary = $minSalary;
        $this->syncSalaryRange();

        return $this;
    }

    public function getMaxSalary(): ?string
    {
        $this->hydrateSalaryBoundsFromRange();

        return $this->maxSalary;
    }

    public function setMaxSalary(?string $maxSalary): static
    {
        $this->maxSalary = $maxSalary;
        $this->syncSalaryRange();

        return $this;
    }

    public function getStatus(): string
    {
        return $this->status;
    }

    public function setStatus(string $status): static
    {
        $this->status = $status;
        return $this;
    }

    public function getSalaryDisplay(): string
    {
        $minSalary = $this->getMinSalary();
        $maxSalary = $this->getMaxSalary();

        if ($minSalary && $maxSalary) {
            return sprintf('%g - %g', $minSalary, $maxSalary);
        }
        if ($minSalary) {
            return sprintf('From %g', $minSalary);
        }
        if ($maxSalary) {
            return sprintf('Up to %g', $maxSalary);
        }
        if ($this->salaryRange) {
            return $this->salaryRange;
        }

        return 'Not specified';
    }

    public function getJobType(): ?string
    {
        return $this->jobType;
    }

    public function setJobType(?string $jobType): static
    {
        $this->jobType = $jobType;

        return $this;
    }

    public function getPostedDate(): ?\DateTimeInterface
    {
        return $this->postedDate;
    }

    public function setPostedDate(\DateTimeInterface $postedDate): static
    {
        $this->postedDate = $postedDate;

        return $this;
    }

    public function getRequirements(): ?string
    {
        return $this->requirements;
    }

    public function setRequirements(?string $requirements): static
    {
        $this->requirements = $requirements;

        return $this;
    }

    public function getUser(): ?User
    {
        return $this->user;
    }

    public function setUser(?User $user): static
    {
        $this->user = $user;

        return $this;
    }

    /**
     * @var Collection<int, JobApplication>
     */
    #[ORM\OneToMany(mappedBy: 'job', targetEntity: JobApplication::class, cascade: ['remove'])]
    private Collection $applications;

    /**
     * @var Collection<int, User>
     */
    #[ORM\ManyToMany(targetEntity: User::class, mappedBy: 'savedJobs')]
    private Collection $savedByUsers;

    /**
     * @return Collection<int, JobApplication>
     */
    public function getApplications(): Collection
    {
        return $this->applications;
    }

    /**
     * @return Collection<int, User>
     */
    public function getSavedByUsers(): Collection
    {
        return $this->savedByUsers;
    }

    public function getPostedAgo(): string
    {
        $now = new \DateTime();
        $diff = $now->diff($this->postedDate);

        if ($diff->y > 0) return $diff->y . ' year' . ($diff->y > 1 ? 's' : '') . ' ago';
        if ($diff->m > 0) return $diff->m . ' month' . ($diff->m > 1 ? 's' : '') . ' ago';
        if ($diff->d > 0) return $diff->d . ' day' . ($diff->d > 1 ? 's' : '') . ' ago';
        if ($diff->h > 0) return $diff->h . ' hour' . ($diff->h > 1 ? 's' : '') . ' ago';
        if ($diff->i > 0) return $diff->i . ' minute' . ($diff->i > 1 ? 's' : '') . ' ago';
        
        return 'just now';
    }

    public function hasAcceptedApplication(): bool
    {
        foreach ($this->applications as $application) {
            if ($application->getStatus() === 'ACCEPTED') {
                return true;
            }
        }
        return false;
    }

    public function getAcceptedApplication(): ?JobApplication
    {
        foreach ($this->applications as $application) {
            if ($application->getStatus() === 'ACCEPTED') {
                return $application;
            }
        }

        return null;
    }

    /**
     * @return Collection<int, JobMilestone>
     */
    public function getMilestones(): Collection
    {
        return $this->milestones;
    }

    public function addMilestone(JobMilestone $milestone): static
    {
        if (!$this->milestones->contains($milestone)) {
            $this->milestones->add($milestone);
            $milestone->setJob($this);
        }
        return $this;
    }

    public function removeMilestone(JobMilestone $milestone): static
    {
        if ($this->milestones->removeElement($milestone)) {
            if ($milestone->getJob() === $this) {
                $milestone->setJob(null);
            }
        }
        return $this;
    }

    public function getProgressPercentage(): int
    {
        if ($this->milestones->isEmpty()) {
            return 0;
        }

        $completed = $this->milestones->filter(fn(JobMilestone $m) => $m->isCompleted())->count();
        return (int) (($completed / $this->milestones->count()) * 100);
    }

    private function syncSalaryRange(): void
    {
        $minSalary = $this->normalizeSalaryValue($this->minSalary);
        $maxSalary = $this->normalizeSalaryValue($this->maxSalary);

        if (null !== $minSalary && null !== $maxSalary) {
            $this->salaryRange = sprintf('%s - %s', $minSalary, $maxSalary);

            return;
        }

        if (null !== $minSalary) {
            $this->salaryRange = $minSalary;

            return;
        }

        if (null !== $maxSalary) {
            $this->salaryRange = $maxSalary;

            return;
        }

        $this->salaryRange = null;
    }

    private function hydrateSalaryBoundsFromRange(): void
    {
        if ((null !== $this->minSalary && '' !== $this->minSalary) || (null !== $this->maxSalary && '' !== $this->maxSalary)) {
            return;
        }

        if (null === $this->salaryRange || '' === trim($this->salaryRange)) {
            return;
        }

        preg_match_all('/\d+(?:\.\d+)?/', $this->salaryRange, $matches);
        $values = $matches[0] ?? [];

        if ([] === $values) {
            return;
        }

        $this->minSalary = $values[0];
        $this->maxSalary = $values[1] ?? null;
    }

    private function normalizeSalaryValue(?string $value): ?string
    {
        if (null === $value) {
            return null;
        }

        $trimmed = trim($value);

        return '' === $trimmed ? null : $trimmed;
    }
}
