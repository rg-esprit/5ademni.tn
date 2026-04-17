<?php
use Symfony\Component\Dotenv\Dotenv;

require __DIR__.'/../vendor/autoload.php';

if (class_exists(Dotenv::class)) {
    (new Dotenv())->bootEnv(__DIR__.'/../.env');
}

$apiKey = $_ENV['GEMINI_API_KEY'] ?? '';
if (!$apiKey) {
    echo "No API key found.";
    exit;
}

$url = "https://generativelanguage.googleapis.com/v1beta/models?key=" . $apiKey;

$ch = curl_init();
curl_setopt($ch, CURLOPT_URL, $url);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
// Disable SSL verification for local testing if needed
curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false); 
$result = curl_exec($ch);
curl_close($ch);

echo $result;
