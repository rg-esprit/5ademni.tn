<?php

namespace App\Entity;

use App\Repository\ArticleRepository;
use Doctrine\ORM\Mapping as ORM;
use Doctrine\Common\Collections\ArrayCollection;
use Doctrine\Common\Collections\Collection;
use Symfony\Component\Validator\Constraints as Assert;

#[ORM\Entity(repositoryClass: ArticleRepository::class)]
#[ORM\Table(name: 'article')]
class Article
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column(type: 'bigint')]
    private ?int $id = null;

    #[ORM\Column(type: 'string', length: 255)]
 #[Assert\NotBlank(message: "Le titre est obligatoire")]
    #[Assert\Length(min: 4, minMessage: "Le titre doit contenir au moins 4 caractères")]
   
        private string $title;

    #[ORM\Column(type: 'text')]
   #[Assert\NotBlank(message: "Le contenu est obligatoire")]
    #[Assert\Length(min: 10, minMessage: "Le contenu est trop court")]
    private string $content;

    #[ORM\Column(type: 'string', length: 20)]
    #[Assert\NotBlank(message: "Le statut est obligatoire")]
    private string $status;

    #[ORM\Column(type: 'datetime')]
    private \DateTime $createdAt;

    #[ORM\Column(type: 'string', length: 255, nullable: true)]
    private ?string $imagePath = null;

    #[ORM\OneToMany(mappedBy: 'article', targetEntity: Commentaire::class, cascade: ['remove'])]
    private Collection $commentaires;

    #[ORM\OneToMany(mappedBy: 'article', targetEntity: Favori::class, cascade: ['remove'])]
    private Collection $favoris;

    public function __construct()
    {
        $this->createdAt = new \DateTime(); 
        $this->commentaires = new ArrayCollection();
        $this->favoris = new ArrayCollection();
    }

    public function getId(): ?int
    {
        return $this->id;
    }

    public function getTitle(): string
    {
        return $this->title;
    }

    public function setTitle(string $title): self
    {
        $this->title = $title;
        return $this;
    }

    public function getContent(): string
    {
        return $this->content;
    }

    public function setContent(string $content): self
    {
        $this->content = $content;
        return $this;
    }

    public function getStatus(): string
    {
        return $this->status;
    }

    public function setStatus(string $status): self
    {
        $this->status = $status;
        return $this;
    }

    public function getCreatedAt(): \DateTime
{
    return $this->createdAt;
}

    public function setCreatedAt(\DateTimeInterface $createdAt): self
{
    // Convert DateTimeImmutable to DateTime
    $this->createdAt = $createdAt instanceof \DateTimeImmutable
        ? \DateTime::createFromImmutable($createdAt)
        : $createdAt;

    return $this;
}

    public function getImagePath(): ?string
    {
        return $this->imagePath;
    }

    public function setImagePath(?string $imagePath): self
    {
        $this->imagePath = $imagePath;
        return $this;
    }

    public function getCommentaires(): Collection
    {
        return $this->commentaires;
    }

    public function addCommentaire(Commentaire $commentaire): self
    {
        if (!$this->commentaires->contains($commentaire)) {
            $this->commentaires[] = $commentaire;
            $commentaire->setArticle($this);
        }

        return $this;
    }

    public function removeCommentaire(Commentaire $commentaire): self
    {
        if ($this->commentaires->removeElement($commentaire)) {
            if ($commentaire->getArticle() === $this) {
                // Optionnel: relation non-nullable côté DB
            }
        }

        return $this;
    }

    public function getFavoris(): Collection
    {
        return $this->favoris;
    }

    public function addFavori(Favori $favori): self
    {
        if (!$this->favoris->contains($favori)) {
            $this->favoris[] = $favori;
            $favori->setArticle($this);
        }

        return $this;
    }

    public function removeFavori(Favori $favori): self
    {
        if ($this->favoris->removeElement($favori)) {
            if ($favori->getArticle() === $this) {
                // Optionnel: relation non-nullable côté DB
            }
        }

        return $this;
    }
}