<?php

namespace App\Entity;

use App\Repository\MessageRepository;
use Doctrine\DBAL\Types\Types;
use Doctrine\ORM\Mapping as ORM;

#[ORM\Entity(repositoryClass: MessageRepository::class)]
#[ORM\Table(name: 'message')]
class Message
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column]
    private ?int $id = null;

    #[ORM\ManyToOne(targetEntity: Conversation::class, inversedBy: 'messages')]
    #[ORM\JoinColumn(nullable: false, onDelete: 'CASCADE')]
    private ?Conversation $conversation = null;

    #[ORM\Column(name: 'expediteur', length: 50)]
    private string $senderIdentifier = '';

    #[ORM\Column(type: Types::TEXT)]
    private string $contenu = '';

    #[ORM\Column(name: 'date_envoie', type: Types::DATETIME_MUTABLE, nullable: true)]
    private ?\DateTimeInterface $dateEnvoi = null;

    #[ORM\Column(name: 'piece_jointe_url', length: 500, nullable: true)]
    private ?string $pieceJointeUrl = null;

    #[ORM\Column(name: 'type_message', length: 20, nullable: true, options: ['default' => 'TEXTE'])]
    private ?string $typeMessage = 'TEXTE';

    #[ORM\Column(name: 'duree_audio', length: 20, nullable: true)]
    private ?string $dureeAudio = null;

    public function __construct()
    {
        $this->dateEnvoi = new \DateTime();
    }

    public function getId(): ?int
    {
        return $this->id;
    }

    public function getConversation(): ?Conversation
    {
        return $this->conversation;
    }

    public function setConversation(?Conversation $conversation): static
    {
        $this->conversation = $conversation;

        return $this;
    }

    public function getSenderIdentifier(): string
    {
        return $this->senderIdentifier;
    }

    public function setSenderIdentifier(string $senderIdentifier): static
    {
        $this->senderIdentifier = trim($senderIdentifier);

        return $this;
    }

    public function markSentBy(User $user): static
    {
        $this->senderIdentifier = (string) $user->getId();

        return $this;
    }

    public function getSenderUserId(): ?int
    {
        $identifier = trim($this->senderIdentifier);

        if ('' === $identifier || !ctype_digit($identifier)) {
            return null;
        }

        return (int) $identifier;
    }

    public function isSentBy(?User $user): bool
    {
        return $user instanceof User && (string) $user->getId() === trim($this->senderIdentifier);
    }

    public function getSenderUser(?Conversation $conversation = null): ?User
    {
        if (!$conversation instanceof Conversation) {
            return null;
        }

        return $conversation->findParticipantByIdentifier($this->senderIdentifier);
    }

    public function getSenderDisplayName(?Conversation $conversation = null): string
    {
        $sender = $this->getSenderUser($conversation);

        if ($sender instanceof User) {
            return $sender->getDisplayName();
        }

        return '' !== trim($this->senderIdentifier) ? $this->senderIdentifier : 'Système';
    }

    public function getContenu(): string
    {
        return $this->contenu;
    }

    public function setContenu(string $contenu): static
    {
        $this->contenu = trim($contenu);

        return $this;
    }

    public function getDateEnvoi(): ?\DateTimeInterface
    {
        return $this->dateEnvoi;
    }

    public function setDateEnvoi(\DateTimeInterface $dateEnvoi): static
    {
        $this->dateEnvoi = $dateEnvoi instanceof \DateTimeImmutable
            ? \DateTime::createFromInterface($dateEnvoi)
            : $dateEnvoi;

        return $this;
    }

    public function getPieceJointeUrl(): ?string
    {
        return $this->pieceJointeUrl;
    }

    public function setPieceJointeUrl(?string $pieceJointeUrl): static
    {
        $this->pieceJointeUrl = $pieceJointeUrl;

        return $this;
    }

    public function getTypeMessage(): string
    {
        return strtoupper($this->typeMessage ?? 'TEXTE');
    }

    public function setTypeMessage(string $typeMessage): static
    {
        $value = strtoupper(trim($typeMessage));
        $this->typeMessage = '' === $value ? 'TEXTE' : $value;

        return $this;
    }

    public function getDureeAudio(): ?string
    {
        return $this->dureeAudio;
    }

    public function setDureeAudio(?string $dureeAudio): static
    {
        $this->dureeAudio = $dureeAudio;

        return $this;
    }

    public function hasAttachment(): bool
    {
        return null !== $this->pieceJointeUrl && '' !== $this->pieceJointeUrl;
    }
}