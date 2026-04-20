<?php

namespace App\Entity;

use App\Repository\ConversationRepository;
use Doctrine\Common\Collections\ArrayCollection;
use Doctrine\Common\Collections\Collection;
use Doctrine\DBAL\Types\Types;
use Doctrine\ORM\Mapping as ORM;

#[ORM\Entity(repositoryClass: ConversationRepository::class)]
#[ORM\Table(name: 'conversation')]
class Conversation
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column]
    private ?int $id = null;

    #[ORM\ManyToOne(targetEntity: User::class)]
    #[ORM\JoinColumn(nullable: false, name: 'client_id', referencedColumnName: 'id', onDelete: 'CASCADE')]
    private ?User $client = null;

    #[ORM\ManyToOne(targetEntity: User::class)]
    #[ORM\JoinColumn(nullable: false, name: 'freelance_id', referencedColumnName: 'id', onDelete: 'CASCADE')]
    private ?User $freelance = null;

    #[ORM\Column(name: 'titre', length: 255)]
    private string $title = '';

    #[ORM\Column(length: 20, nullable: true, options: ['default' => 'ACTIVE'])]
    private ?string $statut = 'ACTIVE';

    #[ORM\Column(name: 'date_creation', type: Types::DATETIME_MUTABLE, nullable: true)]
    private ?\DateTimeInterface $dateCreation = null;

    #[ORM\Column(name: 'non_lus_client', type: Types::INTEGER, options: ['default' => 0])]
    private int $nonLusClient = 0;

    #[ORM\Column(name: 'non_lus_freelance', type: Types::INTEGER, options: ['default' => 0])]
    private int $nonLusFreelance = 0;

    #[ORM\OneToMany(mappedBy: 'conversation', targetEntity: Message::class, cascade: ['persist', 'remove'], orphanRemoval: true)]
    private Collection $messages;

    #[ORM\OneToMany(mappedBy: 'conversation', targetEntity: ConversationMember::class, cascade: ['persist', 'remove'], orphanRemoval: true)]
    private Collection $members;

    public function __construct()
    {
        $this->messages = new ArrayCollection();
        $this->members = new ArrayCollection();
        $this->dateCreation = new \DateTime();
    }

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

    public function getFreelance(): ?User
    {
        return $this->freelance;
    }

    public function setFreelance(User $freelance): static
    {
        $this->freelance = $freelance;

        return $this;
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

    public function getStatut(): string
    {
        return strtoupper($this->statut ?? 'ACTIVE');
    }

    public function setStatut(string $statut): static
    {
        $value = strtoupper(trim($statut));
        $this->statut = '' === $value ? 'ACTIVE' : $value;

        return $this;
    }

    public function getDateCreation(): ?\DateTimeInterface
    {
        return $this->dateCreation;
    }

    public function setDateCreation(\DateTimeInterface $dateCreation): static
    {
        $this->dateCreation = $dateCreation instanceof \DateTimeImmutable
            ? \DateTime::createFromInterface($dateCreation)
            : $dateCreation;

        return $this;
    }

    public function getNonLusClient(): int
    {
        return $this->nonLusClient;
    }

    public function setNonLusClient(int $nonLusClient): static
    {
        $this->nonLusClient = $nonLusClient;

        return $this;
    }

    public function getNonLusFreelance(): int
    {
        return $this->nonLusFreelance;
    }

    public function setNonLusFreelance(int $nonLusFreelance): static
    {
        $this->nonLusFreelance = $nonLusFreelance;

        return $this;
    }

    /**
     * @return Collection<int, Message>
     */
    public function getMessages(): Collection
    {
        return $this->messages;
    }

    public function addMessage(Message $message): static
    {
        if (!$this->messages->contains($message)) {
            $this->messages->add($message);
            $message->setConversation($this);
        }

        return $this;
    }

    public function removeMessage(Message $message): static
    {
        if ($this->messages->removeElement($message)) {
            if ($message->getConversation() === $this) {
                $message->setConversation(null);
            }
        }

        return $this;
    }

    /**
     * @return Collection<int, ConversationMember>
     */
    public function getMembers(): Collection
    {
        return $this->members;
    }

    public function addMember(ConversationMember $member): static
    {
        if (!$this->members->contains($member)) {
            $this->members->add($member);
            $member->setConversation($this);
        }

        return $this;
    }

    public function removeMember(ConversationMember $member): static
    {
        if ($this->members->removeElement($member)) {
            if ($member->getConversation() === $this) {
                $member->setConversation(null);
            }
        }

        return $this;
    }

    public function hasMember(User $user): bool
    {
        foreach ($this->members as $member) {
            if ($member->getUser()?->getId() === $user->getId()) {
                return true;
            }
        }

        return false;
    }

    public function getParticipantUsers(): array
    {
        $users = [];
        foreach ($this->members as $member) {
            if (null !== $member->getUser()) {
                $users[$member->getUser()->getId()] = $member->getUser();
            }
        }
        if (null !== $this->client) {
            $users[$this->client->getId()] = $this->client;
        }
        if (null !== $this->freelance) {
            $users[$this->freelance->getId()] = $this->freelance;
        }

        return array_values($users);
    }

    public function belongsTo(User $user): bool
    {
        if (null !== $this->client && $this->client->getId() === $user->getId()) {
            return true;
        }

        if (null !== $this->freelance && $this->freelance->getId() === $user->getId()) {
            return true;
        }

        return $this->hasMember($user);
    }

    public function getOtherParticipant(User $currentUser): ?User
    {
        foreach ($this->getParticipantsExcept($currentUser) as $participant) {
            return $participant;
        }

        if (null !== $this->client && $this->client->getId() !== $currentUser->getId()) {
            return $this->client;
        }

        if (null !== $this->freelance && $this->freelance->getId() !== $currentUser->getId()) {
            return $this->freelance;
        }

        return null;
    }

    public function getParticipantsExcept(User $currentUser): array
    {
        $participants = [];

        foreach ($this->getMembers() as $member) {
            $user = $member->getUser();
            if (null !== $user && $user->getId() !== $currentUser->getId()) {
                $participants[$user->getId()] = $user;
            }
        }

        if (null !== $this->client && $this->client->getId() !== $currentUser->getId()) {
            $participants[$this->client->getId()] = $this->client;
        }

        if (null !== $this->freelance && $this->freelance->getId() !== $currentUser->getId()) {
            $participants[$this->freelance->getId()] = $this->freelance;
        }

        return array_values($participants);
    }

    public function getUnreadCountFor(User $user): int
    {
        if (null !== $this->client && $this->client->getId() === $user->getId()) {
            return $this->nonLusClient;
        }

        if (null !== $this->freelance && $this->freelance->getId() === $user->getId()) {
            return $this->nonLusFreelance;
        }

        return 0;
    }

    public function findParticipantByIdentifier(?string $identifier): ?User
    {
        $normalized = trim((string) $identifier);

        if ('' === $normalized) {
            return null;
        }

        foreach ($this->getParticipantUsers() as $participant) {
            if ((string) $participant->getId() === $normalized) {
                return $participant;
            }

            if (0 === strcasecmp($participant->getEmail(), $normalized) || 0 === strcasecmp($participant->getDisplayName(), $normalized)) {
                return $participant;
            }
        }

        return null;
    }

}