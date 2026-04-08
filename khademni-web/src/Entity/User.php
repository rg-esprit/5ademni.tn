<?php

namespace App\Entity;

use App\Repository\UserRepository;
use Doctrine\DBAL\Types\Types;
use Doctrine\ORM\Mapping as ORM;
use Symfony\Component\Security\Core\User\PasswordAuthenticatedUserInterface;
use Symfony\Component\Security\Core\User\UserInterface;

#[ORM\Entity(repositoryClass: UserRepository::class)]
#[ORM\Table(name: 'users')]
#[ORM\UniqueConstraint(name: 'email', columns: ['email'])]
class User implements UserInterface, PasswordAuthenticatedUserInterface
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column]
    private ?int $id = null;

    #[ORM\Column(name: 'first_name', length: 100)]
    private string $firstName = '';

    #[ORM\Column(name: 'last_name', length: 100)]
    private string $lastName = '';

    #[ORM\Column(name: 'date_of_birth', type: Types::DATE_MUTABLE)]
    private ?\DateTimeInterface $dateOfBirth = null;

    #[ORM\Column(type: Types::FLOAT, options: ['default' => '0'])]
    private float $balance = 0.0;

    #[ORM\Column(length: 255)]
    private string $email = '';

    #[ORM\Column(length: 255)]
    private string $password = '';

    #[ORM\Column(name: 'is_admin', type: Types::BOOLEAN, options: ['default' => false])]
    private bool $isAdmin = false;

    #[ORM\Column(name: 'profile_img', length: 500, options: ['default' => ''])]
    private string $profileImg = '';

    #[ORM\Column(type: Types::TEXT, length: 65535, nullable: true)]
    private ?string $bio = null;

    #[ORM\Column(name: 'face_embedding', type: Types::JSON, nullable: true, options: ['comment' => '128-dim Facenet embedding stored as JSON array'])]
    private ?array $faceEmbedding = null;

    #[ORM\ManyToMany(targetEntity: Job::class, inversedBy: 'savedByUsers')]
    #[ORM\JoinTable(name: 'saved_jobs')]
    #[ORM\JoinColumn(name: 'user_id', referencedColumnName: 'id', onDelete: 'CASCADE')]
    #[ORM\InverseJoinColumn(name: 'job_id', referencedColumnName: 'id', onDelete: 'CASCADE')]
    private \Doctrine\Common\Collections\Collection $savedJobs;

    public function __construct()
    {
        $this->savedJobs = new \Doctrine\Common\Collections\ArrayCollection();
    }

    public function getSavedJobs(): \Doctrine\Common\Collections\Collection
    {
        return $this->savedJobs;
    }

    public function addSavedJob(Job $job): static
    {
        if (!$this->savedJobs->contains($job)) {
            $this->savedJobs->add($job);
        }

        return $this;
    }

    public function removeSavedJob(Job $job): static
    {
        $this->savedJobs->removeElement($job);

        return $this;
    }

    public function getId(): ?int
    {
        return $this->id;
    }

    public function getFirstName(): string
    {
        return $this->firstName;
    }

    public function setFirstName(string $firstName): static
    {
        $this->firstName = $firstName;

        return $this;
    }

    public function getLastName(): string
    {
        return $this->lastName;
    }

    public function setLastName(string $lastName): static
    {
        $this->lastName = $lastName;

        return $this;
    }

    public function getDateOfBirth(): ?\DateTimeInterface
    {
        return $this->dateOfBirth;
    }

    public function setDateOfBirth(?\DateTimeInterface $dateOfBirth): static
    {
        $this->dateOfBirth = $dateOfBirth;

        return $this;
    }

    public function getBalance(): float
    {
        return $this->balance;
    }

    public function setBalance(float $balance): static
    {
        $this->balance = $balance;

        return $this;
    }

    public function getEmail(): string
    {
        return $this->email;
    }

    public function setEmail(string $email): static
    {
        $this->email = strtolower(trim($email));

        return $this;
    }

    public function getUserIdentifier(): string
    {
        return $this->email;
    }

    public function getPassword(): string
    {
        return $this->password;
    }

    public function setPassword(string $password): static
    {
        $this->password = $password;

        return $this;
    }

    public function isAdmin(): bool
    {
        return $this->isAdmin;
    }

    public function setIsAdmin(bool $isAdmin): static
    {
        $this->isAdmin = $isAdmin;

        return $this;
    }

    public function getRoles(): array
    {
        $roles = ['ROLE_USER'];

        if ($this->isAdmin) {
            $roles[] = 'ROLE_ADMIN';
        }

        return array_values(array_unique($roles));
    }

    public function eraseCredentials(): void
    {
    }

    public function getProfileImg(): string
    {
        return $this->profileImg;
    }

    public function setProfileImg(?string $profileImg): static
    {
        $this->profileImg = trim((string) $profileImg);

        return $this;
    }

    public function hasProfileImage(): bool
    {
        return '' !== $this->profileImg;
    }

    public function getBio(): ?string
    {
        return $this->bio;
    }

    public function setBio(?string $bio): static
    {
        $this->bio = '' === trim((string) $bio) ? null : trim((string) $bio);

        return $this;
    }

    public function getFaceEmbedding(): ?array
    {
        return $this->faceEmbedding;
    }

    public function setFaceEmbedding(?array $faceEmbedding): static
    {
        $this->faceEmbedding = $faceEmbedding;

        return $this;
    }

    public function hasFaceEnrollment(): bool
    {
        return null !== $this->faceEmbedding && [] !== $this->faceEmbedding;
    }

    public function getDisplayName(): string
    {
        return trim($this->firstName.' '.$this->lastName);
    }

    public function getInitials(): string
    {
        $initials = '';

        if ('' !== $this->firstName) {
            $initials .= substr($this->firstName, 0, 1);
        }

        if ('' !== $this->lastName) {
            $initials .= substr($this->lastName, 0, 1);
        }

        return strtoupper($initials ?: 'U');
    }

    public function getAvatarVersion(): string
    {
        return '' === $this->profileImg ? 'default' : sha1($this->profileImg);
    }
}
