<?php

namespace App\Entity;

use App\Repository\GigRepository;
use Doctrine\DBAL\Types\Types;
use Doctrine\ORM\Mapping as ORM;

#[ORM\Entity(repositoryClass: GigRepository::class)]
#[ORM\Table(name: 'gig')]
#[ORM\Index(name: 'category_id', columns: ['category_id'])]
class Gig
{
    public const STATUS_DRAFT = 'DRAFT';
    public const STATUS_PENDING = 'PENDING';
    public const STATUS_APPROVED = 'APPROVED';
    public const STATUS_REJECTED = 'REJECTED';
    public const STATUS_ARCHIVED = 'ARCHIVED';
    public const STATUS_EXPIRED = 'EXPIRED';

    private const LEGACY_STATUS_MAP = [
        'ACTIVE' => self::STATUS_APPROVED,
        'INACTIVE' => self::STATUS_DRAFT,
    ];

    private const WORKFLOW_STATUSES = [
        self::STATUS_DRAFT,
        self::STATUS_PENDING,
        self::STATUS_APPROVED,
        self::STATUS_REJECTED,
        self::STATUS_ARCHIVED,
    ];

    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column]
    private ?int $id = null;

    #[ORM\Column(length: 255)]
    private string $title = '';

    #[ORM\Column(type: Types::TEXT)]
    private string $description = '';

    #[ORM\Column(type: Types::FLOAT)]
    private float $price = 0.0;

    #[ORM\Column(name: 'delivery_time', type: Types::DATETIME_IMMUTABLE, nullable: true)]
    private ?\DateTimeImmutable $deliveryTime = null;

    #[ORM\Column(length: 500, nullable: true)]
    private ?string $image = null;

    #[ORM\Column(length: 50, nullable: true, options: ['default' => 'DRAFT'])]
    private ?string $status = self::STATUS_DRAFT;

    #[ORM\Column(name: 'relevance_score', type: Types::FLOAT, nullable: true, options: ['default' => 0])]
    private ?float $relevanceScore = 0.0;

    #[ORM\ManyToOne(targetEntity: Category::class, inversedBy: 'gigs')]
    #[ORM\JoinColumn(name: 'category_id', referencedColumnName: 'id', nullable: true, onDelete: 'SET NULL')]
    private ?Category $category = null;

    #[ORM\Column(name: 'user_id', nullable: true)]
    private ?int $userId = null;

    private ?\DateTimeImmutable $createdAt = null;

    private ?\DateTimeImmutable $updatedAt = null;

    public function getId(): ?int
    {
        return $this->id;
    }

    public function getTitle(): string
    {
        return $this->title;
    }

    public function setTitle(string $title): static
    {
        $this->title = trim($title);

        return $this;
    }

    public function getDescription(): string
    {
        return $this->description;
    }

    public function setDescription(string $description): static
    {
        $this->description = trim($description);

        return $this;
    }

    public function getPrice(): float
    {
        return $this->price;
    }

    public function setPrice(float $price): static
    {
        $this->price = $price;

        return $this;
    }

    public function getDeliveryTime(): ?\DateTimeImmutable
    {
        return $this->deliveryTime;
    }

    public function setDeliveryTime(?\DateTimeInterface $deliveryTime): static
    {
        if (null === $deliveryTime) {
            $this->deliveryTime = null;

            return $this;
        }

        $this->deliveryTime = $deliveryTime instanceof \DateTimeImmutable
            ? $deliveryTime
            : \DateTimeImmutable::createFromMutable($deliveryTime);

        return $this;
    }

    public function getImage(): ?string
    {
        return $this->image;
    }

    public function setImage(?string $image): static
    {
        $clean = trim((string) $image);
        $this->image = '' === $clean ? null : $clean;

        return $this;
    }

    public function getStatus(): ?string
    {
        return self::normalizeStatus((string) $this->status);
    }

    public function setStatus(?string $status): static
    {
        $this->status = self::normalizeStatus((string) $status);

        return $this;
    }

    public function getWorkflowStatus(): string
    {
        return self::normalizeStatus((string) $this->status);
    }

    public function getRelevanceScore(): ?float
    {
        return $this->relevanceScore;
    }

    public function setRelevanceScore(?float $relevanceScore): static
    {
        $this->relevanceScore = $relevanceScore;

        return $this;
    }

    public function getCategory(): ?Category
    {
        return $this->category;
    }

    public function setCategory(?Category $category): static
    {
        $this->category = $category;

        return $this;
    }

    public function getUserId(): ?int
    {
        return $this->userId;
    }

    public function setUserId(?int $userId): static
    {
        $this->userId = $userId;

        return $this;
    }

    public function getCreatedAt(): ?\DateTimeImmutable
    {
        return $this->createdAt;
    }

    public function setCreatedAt(?\DateTimeImmutable $createdAt): static
    {
        $this->createdAt = $createdAt;

        return $this;
    }

    public function getUpdatedAt(): ?\DateTimeImmutable
    {
        return $this->updatedAt;
    }

    public function setUpdatedAt(?\DateTimeImmutable $updatedAt): static
    {
        $this->updatedAt = $updatedAt;

        return $this;
    }

    public function getDisplayStatus(?\DateTimeInterface $now = null): string
    {
        $current = $now ?? new \DateTimeImmutable();
        $status = $this->getWorkflowStatus();

        if (self::STATUS_APPROVED === $status && $this->deliveryTime instanceof \DateTimeInterface && $this->deliveryTime < $current) {
            return self::STATUS_EXPIRED;
        }

        return $status;
    }

    public function isOwner(?User $user): bool
    {
        return $user instanceof User && null !== $this->userId && $this->userId === $user->getId();
    }

    public static function normalizeStatus(string $status): string
    {
        $normalized = strtoupper(trim($status));
        if ('' === $normalized) {
            return self::STATUS_DRAFT;
        }

        if (isset(self::LEGACY_STATUS_MAP[$normalized])) {
            return self::LEGACY_STATUS_MAP[$normalized];
        }

        if (in_array($normalized, self::WORKFLOW_STATUSES, true)) {
            return $normalized;
        }

        return self::STATUS_DRAFT;
    }

    /**
     * @return string[]
     */
    public static function getFilterableStatuses(): array
    {
        return [
            'ALL',
            self::STATUS_DRAFT,
            self::STATUS_PENDING,
            self::STATUS_APPROVED,
            self::STATUS_REJECTED,
            self::STATUS_ARCHIVED,
            self::STATUS_EXPIRED,
        ];
    }

    /**
     * @return string[]
     */
    public static function getUserFormStatuses(): array
    {
        return [
            self::STATUS_DRAFT,
            self::STATUS_PENDING,
            self::STATUS_ARCHIVED,
        ];
    }

    /**
     * @return string[]
     */
    public static function getAdminActionStatuses(): array
    {
        return [
            self::STATUS_PENDING,
            self::STATUS_APPROVED,
            self::STATUS_REJECTED,
            self::STATUS_ARCHIVED,
        ];
    }

    public function canTransitionTo(string $targetStatus, bool $isAdmin): bool
    {
        $current = $this->getWorkflowStatus();
        $target = self::normalizeStatus($targetStatus);
        $map = $isAdmin ? self::adminTransitions() : self::userTransitions();

        return in_array($target, $map[$current] ?? [], true);
    }

    /**
     * @return string[]
     */
    public function getApprovalValidationErrors(?\DateTimeInterface $now = null): array
    {
        $errors = [];
        $current = $now ?? new \DateTimeImmutable();

        if ('' === trim($this->title)) {
            $errors[] = 'Title is required.';
        }

        if ('' === trim($this->description)) {
            $errors[] = 'Description is required.';
        }

        if (null === $this->category) {
            $errors[] = 'Category is required.';
        }

        if ($this->price < 10.0) {
            $errors[] = 'Price must be at least 10.00 TND.';
        }

        if (!$this->deliveryTime instanceof \DateTimeInterface) {
            $errors[] = 'Delivery date is required.';
        } elseif ($this->deliveryTime < $current) {
            $errors[] = 'Delivery date must be in the future.';
        }

        return $errors;
    }

    /**
     * @return array<string, string[]>
     */
    private static function userTransitions(): array
    {
        return [
            self::STATUS_DRAFT => [self::STATUS_DRAFT, self::STATUS_PENDING, self::STATUS_ARCHIVED],
            self::STATUS_PENDING => [self::STATUS_DRAFT, self::STATUS_PENDING, self::STATUS_ARCHIVED],
            self::STATUS_APPROVED => [self::STATUS_APPROVED, self::STATUS_ARCHIVED],
            self::STATUS_REJECTED => [self::STATUS_DRAFT, self::STATUS_PENDING, self::STATUS_ARCHIVED],
            self::STATUS_ARCHIVED => [self::STATUS_ARCHIVED],
        ];
    }

    /**
     * @return array<string, string[]>
     */
    private static function adminTransitions(): array
    {
        return [
            self::STATUS_DRAFT => [self::STATUS_DRAFT, self::STATUS_PENDING, self::STATUS_REJECTED, self::STATUS_ARCHIVED],
            self::STATUS_PENDING => [self::STATUS_PENDING, self::STATUS_APPROVED, self::STATUS_REJECTED, self::STATUS_ARCHIVED],
            self::STATUS_APPROVED => [self::STATUS_APPROVED, self::STATUS_REJECTED, self::STATUS_ARCHIVED],
            self::STATUS_REJECTED => [self::STATUS_PENDING, self::STATUS_REJECTED, self::STATUS_ARCHIVED],
            self::STATUS_ARCHIVED => [self::STATUS_ARCHIVED],
        ];
    }
}
