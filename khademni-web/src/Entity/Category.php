<?php

namespace App\Entity;

use App\Repository\CategoryRepository;
use Doctrine\Common\Collections\ArrayCollection;
use Doctrine\Common\Collections\Collection;
use Doctrine\ORM\Mapping as ORM;

#[ORM\Entity(repositoryClass: CategoryRepository::class)]
#[ORM\Table(name: 'category')]
class Category
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column]
    private ?int $id = null;

    #[ORM\Column(length: 100)]
    private string $name = '';

    #[ORM\Column(type: 'text', nullable: true)]
    private ?string $description = null;

    #[ORM\Column(name: 'is_active', type: 'boolean', nullable: true, options: ['default' => true])]
    private ?bool $isActive = true;

    /**
     * @var Collection<int, Gig>
     */
    #[ORM\OneToMany(mappedBy: 'category', targetEntity: Gig::class)]
    private Collection $gigs;

    public function __construct()
    {
        $this->gigs = new ArrayCollection();
    }

    public function getId(): ?int
    {
        return $this->id;
    }

    public function getName(): string
    {
        return $this->name;
    }

    public function setName(string $name): static
    {
        $this->name = trim($name);

        return $this;
    }

    public function getDescription(): ?string
    {
        return $this->description;
    }

    public function setDescription(?string $description): static
    {
        $clean = trim((string) $description);
        $this->description = '' === $clean ? null : $clean;

        return $this;
    }

    public function isActive(): bool
    {
        return true === $this->isActive;
    }

    public function setIsActive(bool $isActive): static
    {
        $this->isActive = $isActive;

        return $this;
    }

    /**
     * @return Collection<int, Gig>
     */
    public function getGigs(): Collection
    {
        return $this->gigs;
    }

    public function __toString(): string
    {
        return $this->name;
    }
}
