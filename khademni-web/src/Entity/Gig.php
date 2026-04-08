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

    #[ORM\Column(length: 50, nullable: true, options: ['default' => 'ACTIVE'])]
    private ?string $status = 'ACTIVE';

    #[ORM\Column(name: 'relevance_score', type: Types::FLOAT, nullable: true, options: ['default' => 0])]
    private ?float $relevanceScore = 0.0;

    #[ORM\ManyToOne(targetEntity: Category::class, inversedBy: 'gigs')]
    #[ORM\JoinColumn(name: 'category_id', referencedColumnName: 'id', nullable: true, onDelete: 'SET NULL')]
    private ?Category $category = null;

    #[ORM\Column(name: 'user_id', nullable: true)]
    private ?int $userId = null;

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
        return $this->status;
    }

    public function setStatus(?string $status): static
    {
        $value = strtoupper(trim((string) $status));
        $this->status = '' === $value ? 'ACTIVE' : $value;

        return $this;
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

    public function getDisplayStatus(?\DateTimeInterface $now = null): string
    {
        $current = $now ?? new \DateTimeImmutable();

        if ($this->deliveryTime instanceof \DateTimeInterface && $this->deliveryTime < $current) {
            return 'EXPIRED';
        }

        return strtoupper($this->status ?? 'ACTIVE');
    }

    public function isOwner(?User $user): bool
    {
        return $user instanceof User && null !== $this->userId && $this->userId === $user->getId();
    }
}
