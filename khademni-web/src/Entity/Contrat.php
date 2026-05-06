<?php

namespace App\Entity;

use Doctrine\ORM\Mapping as ORM;
use Symfony\Component\Validator\Constraints as Assert;
use Symfony\Component\Validator\Context\ExecutionContextInterface;

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
    private ?string $titre = null;

    #[ORM\Column(type: "date")]
    #[Assert\NotNull(message: "Date cannot be null.")]
    private ?\DateTimeInterface $dateContrat = null;

    #[ORM\Column(type: "text", nullable: true)]
    #[Assert\NotBlank(message: "La description est obligatoire.")]
    private ?string $description = null;

    #[ORM\Column(type: "float", nullable: true)]
    #[Assert\NotBlank(message: "Le prix est obligatoire.")]
    private ?float $prix = null;

    #[ORM\Column(type: "string", length: 50, nullable: true, options: ["default" => "EN_ATTENTE"])]
    private ?string $statut = 'EN_ATTENTE';

    #[ORM\Column(type: "string", length: 20, nullable: true)]
    #[Assert\NotBlank(message: "Le numéro de téléphone est obligatoire.")]
    #[Assert\Regex(pattern: "/^[0-9]{8}$/", message: "Le numéro de téléphone doit contenir exactement 8 chiffres.")]
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
    #[Assert\Callback]
    public function validatePrix(ExecutionContextInterface $context): void
    {
        if ($this->prix === null) {
            return;
        }

        if ($this->prix <= 0) {
            $context->buildViolation('Le prix doit être supérieur à 0 TND.')
                ->atPath('prix')
                ->addViolation();
        }

        // Rounding rule (ends with 0 or 5)
        $lastDigit = (int) round($this->prix) % 10;
        if ($lastDigit !== 0 && $lastDigit !== 5) {
            $context->buildViolation('Le prix doit se terminer par 0 ou 5 (ex: 3000, 3055, 4500).')
                ->atPath('prix')
                ->addViolation();
        }
    }

    #[Assert\Callback]
    public function validateMilestones(ExecutionContextInterface $context): void
    {
        if (!$this->description) {
            return;
        }

        // PARSE MILESTONES: Supports (Milestones: 50/50), (Milestones: 30 / 30 / 40)
        if (preg_match('/\(Milestones:\s*([\d\/%\s]+)\)/i', $this->description, $matches)) {
            $ratios = explode('/', str_replace(['%', ' '], '', $matches[1]));
            $total = 0;
            foreach ($ratios as $r) {
                if (!is_numeric($r)) {
                    $context->buildViolation('Format de milestone invalide (ex: 50/50).')
                        ->atPath('description')
                        ->addViolation();
                    return;
                }
                $total += (float) $r;
            }

            if (abs($total - 100) > 0.01) {
                $context->buildViolation("La somme des milestones doit être égale à 100% (actuelle: $total%).")
                    ->atPath('description')
                    ->addViolation();
            }
        }
    }
}
