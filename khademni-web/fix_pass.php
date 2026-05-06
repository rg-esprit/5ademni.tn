<?php
require 'vendor/autoload.php';
use App\Entity\User;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Console\Application;
use Symfony\Component\Console\Input\ArrayInput;
use Symfony\Component\Console\Output\BufferedOutput;

// Simple SQL update to avoid Doctrine complexity for now
$pdo = new PDO('mysql:host=127.0.0.1;dbname=appdb', 'root', '');
$email = 'dakhliayoub99@gmail.com';
$pass = '7swAPi4D8M9FihD';
// We'll use SHA-256 for now since JavaFX uses it, and we want to UNBLOCK the user.
$hash = hash('sha256', $pass);

$stmt = $pdo->prepare("UPDATE users SET password = ? WHERE email = ?");
$stmt->execute([$hash, $email]);

echo "Password updated for $email to SHA-256 hash: $hash\n";
