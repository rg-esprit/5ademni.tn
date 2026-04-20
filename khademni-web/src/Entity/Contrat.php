<?php

namespace App\Entity;

use Doctrine\ORM\Mapping as ORM;
use Symfony\Component\Validator\Constraints as Assert;

#[ORM\Entity(repositoryClass: \App\Repository\ContratRepository::class)]
#[ORM\Table(name: "contrats")]
class Contrat
{
    #[ORM\Id]
    #[ORM\GeneratedValue(strategy: "IDENTITY")]
    #[ORM\Column(type: "integer")]
    private ?int $id = null;

    #[ORM\Column(type: "integer")]
    #[Assert\NotBlank(message: "Client is required.")]
    private ?int $clientId = null;

    #[ORM\Column(type: "integer")]
    #[Assert\NotBlank(message: "Freelancer is required.")]
    private ?int $freelancerId = null;

    #[ORM\Column(type: "string", length: 255, nullable: true)]
    #[Assert\NotBlank(message: "Titre is required.")]
    private ?string $titre = null;

    #[ORM\Column(type: "date")]
    #[Assert\NotNull(message: "Date cannot be null.")]
    private ?\DateTimeInterface $dateContrat = null;

    #[ORM\Column(type: "text", nullable: true)]
    private ?string $description = null;

    #[ORM\Column(type: "float", nullable: true)]
    #[Assert\Positive(message: "Prix must be positive.")]
    private ?float $prix = null;

    #[ORM\Column(type: "string", length: 50, nullable: true, options: ["default" => "EN_ATTENTE"])]
    private ?string $statut = 'EN_ATTENTE';

    #[ORM\Column(type: "string", length: 20, nullable: true)]
    private ?string $numTelephone = null;

    public function getId(): ?int
    {
        return $this->id;
    }

    public function getClientId(): ?int
    {
        return $this->clientId;
    }
    public function setClientId(?int $clientId): static
    {
        $this->clientId = $clientId;
        return $this;
    }

    public function getFreelancerId(): ?int
    {
        return $this->freelancerId;
    }
    public function setFreelancerId(?int $freelancerId): static
    {
        $this->freelancerId = $freelancerId;
        return $this;
    }

    public function getTitre(): ?string
    {
        return $this->titre;
    }
    public function setTitre(?string $titre): static
    {
        $this->titre = $titre;
        return $this;
    }

    public function getDateContrat(): ?\DateTimeInterface
    {
        return $this->dateContrat;
    }
    public function setDateContrat(?\DateTimeInterface $dateContrat): static
    {
        $this->dateContrat = $dateContrat;
        return $this;
    }

    public function getDescription(): ?string
    {
        return $this->description;
    }
    public function setDescription(?string $description): static
    {
        $this->description = $description;
        return $this;
    }

    public function getPrix(): ?float
    {
        return $this->prix;
    }
    public function setPrix(?float $prix): static
    {
        $this->prix = $prix;
        return $this;
    }

    public function getStatut(): ?string
    {
        return $this->statut;
    }
    public function setStatut(?string $statut): static
    {
        $this->statut = $statut;
        return $this;
    }

    public function getNumTelephone(): ?string
    {
        return $this->numTelephone;
    }
    public function setNumTelephone(?string $numTelephone): static
    {
        $this->numTelephone = $numTelephone;
        return $this;
    }
}