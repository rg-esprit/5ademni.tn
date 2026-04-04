<?php

namespace App\Entity;

use App\Repository\ReviewRepository;
use Doctrine\DBAL\Types\Types;
use Doctrine\ORM\Mapping as ORM;

#[ORM\Entity(repositoryClass: ReviewRepository::class)]
#[ORM\Table(name: 'reviews')]
#[ORM\Index(name: 'fk_reviews_client', columns: ['client_id'])]
#[ORM\Index(name: 'fk_reviews_freelancer', columns: ['freelancer_id'])]
class Review
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column]
    private ?int $id = null;

    #[ORM\ManyToOne(targetEntity: User::class)]
    #[ORM\JoinColumn(name: 'client_id', referencedColumnName: 'id', nullable: false, onDelete: 'CASCADE')]
    private ?User $client = null;

    #[ORM\ManyToOne(targetEntity: User::class)]
    #[ORM\JoinColumn(name: 'freelancer_id', referencedColumnName: 'id', nullable: false, onDelete: 'CASCADE')]
    private ?User $freelancer = null;

    #[ORM\Column(type: Types::SMALLINT)]
    private int $rating = 0;

    #[ORM\Column(name: 'review_text', type: Types::TEXT, length: 65535)]
    private string $reviewText = '';

    public function getId(): ?int
    {
        return $this->id;
    }

    public function getClient(): ?User
    {
        return $this->client;
    }

    public function setClient(User $client): static
    {
        $this->client = $client;

        return $this;
    }

    public function getFreelancer(): ?User
    {
        return $this->freelancer;
    }

    public function setFreelancer(User $freelancer): static
    {
        $this->freelancer = $freelancer;

        return $this;
    }

    public function getRating(): int
    {
        return $this->rating;
    }

    public function setRating(int $rating): static
    {
        $this->rating = $rating;

        return $this;
    }

    public function getReviewText(): string
    {
        return $this->reviewText;
    }

    public function setReviewText(string $reviewText): static
    {
        $this->reviewText = trim($reviewText);

        return $this;
    }
}
