<?php

namespace App\Service;

use Symfony\Contracts\HttpClient\HttpClientInterface;
use Symfony\Contracts\HttpClient\Exception\TransportExceptionInterface;
use Symfony\Component\DependencyInjection\ParameterBag\ParameterBagInterface;
use Psr\Log\LoggerInterface;

/**
 * AI service backed by Groq while preserving the existing GeminiService API.
 */
class GeminiService
{
    private const MODEL = 'llama-3.3-70b-versatile';

    private HttpClientInterface $httpClient;
    private string $apiKey;
    private string $apiUrl;
    private LoggerInterface $logger;

    public function __construct(HttpClientInterface $httpClient, ParameterBagInterface $params, LoggerInterface $logger)
    {
        $this->httpClient = $httpClient;
        $this->apiKey = (string) $params->get('app.groq_api_key');
        $this->apiUrl = (string) $params->get('app.groq_url');
        $this->logger = $logger;

        if (trim($this->apiUrl) === '') {
            $this->apiUrl = 'https://api.groq.com/openai/v1/chat/completions';
        }
    }

    private function chat(string $systemPrompt, string $userMessage): string
    {
        if (trim($this->apiKey) === '') {
            throw new \RuntimeException('Groq API key is missing.');
        }

        try {
            $response = $this->httpClient->request('POST', $this->apiUrl, [
                'headers' => [
                    'Authorization' => 'Bearer ' . $this->apiKey,
                    'Content-Type' => 'application/json',
                ],
                'timeout' => 60,
                'json' => [
                    'model' => self::MODEL,
                    'temperature' => 0.3,
                    'messages' => [
                        ['role' => 'system', 'content' => $systemPrompt],
                        ['role' => 'user', 'content' => $userMessage],
                    ],
                ],
            ]);

            $statusCode = $response->getStatusCode();
            $content = $response->getContent(false);

            if ($statusCode !== 200) {
                $errorData = json_decode($content, true);
                $errorMessage = $errorData['error']['message'] ?? $content;
                $this->logger->error(sprintf('Groq API Error (%d): %s', $statusCode, $errorMessage));

                throw new \RuntimeException(sprintf('Groq API Error (%d): %s', $statusCode, $errorMessage));
            }

            $result = json_decode($content, true);
            $text = $result['choices'][0]['message']['content'] ?? null;

            if (!is_string($text) || trim($text) === '') {
                throw new \RuntimeException('Groq API returned an empty response.');
            }

            return $text;
        } catch (TransportExceptionInterface $e) {
            $this->logger->error('Groq API transport error: ' . $e->getMessage());

            throw new \RuntimeException('Network error while connecting to Groq API: ' . $e->getMessage());
        }
    }

    private function extractJson(string $text): ?array
    {
        $clean = trim($text);

        if (preg_match('/```(?:json)?\s*(.*?)```/s', $clean, $matches)) {
            $clean = trim($matches[1]);
        }

        $decoded = json_decode($clean, true);
        if (json_last_error() === JSON_ERROR_NONE && is_array($decoded)) {
            return $decoded;
        }

        if (preg_match('/\{.*\}/s', $clean, $matches)) {
            $decoded = json_decode($matches[0], true);
            if (json_last_error() === JSON_ERROR_NONE && is_array($decoded)) {
                return $decoded;
            }
        }

        return null;
    }

    /**
     * Generates a full job posting from a simple idea.
     */
    public function generateJobPosting(string $idea): ?array
    {
        $system = 'Act as a professional recruitment expert. Only output valid JSON, never markdown.';
        $user = $this->constructPrompt($idea);

        try {
            $text = $this->chat($system, $user);
        } catch (\RuntimeException $e) {
            return ['error' => $e->getMessage()];
        }

        $decoded = $this->extractJson($text);
        if ($decoded === null) {
            $this->logger->error('Groq job generation response was not valid JSON: ' . $text);

            return ['error' => 'AI returned an invalid JSON format.'];
        }

        return $decoded;
    }

    /**
     * Analyzes a CV and returns a score, status, and reason.
     */
    public function analyzeCVText(string $cvText): ?array
    {
        $system = 'You are an expert recruiter and CV evaluator. Only output valid JSON, never markdown.';

        $user = "Analyze the following CV and evaluate its quality.
Give a score from 0 to 100 based on:
* clarity of experience
* presence of skills
* realism of claims
* completeness of information

Rules:
* 0-40: good and clear CV (Status: SAFE)
* 40-70: average CV (Status: AVERAGE)
* 70-100: suspicious or low quality (Status: SUSPICIOUS)

Return ONLY this JSON format:
{
  \"score\": <number>,
  \"status\": \"SAFE or AVERAGE or SUSPICIOUS\",
  \"reason\": \"short explanation\"
}

CV CONTENT:
\"$cvText\"";

        try {
            $text = $this->chat($system, $user);
        } catch (\RuntimeException $e) {
            return ['error' => $e->getMessage()];
        }

        $decoded = $this->extractJson($text);
        if ($decoded === null) {
            $this->logger->error('Groq CV analysis response was not valid JSON: ' . $text);

            return ['error' => 'AI returned an invalid JSON format.'];
        }

        if (!isset($decoded['score'], $decoded['status'], $decoded['reason'])) {
            $this->logger->error('Groq CV analysis response missing required keys: ' . $text);

            return null;
        }

        return $decoded;
    }

    public function translateJobData(string $title, string $description, ?string $requirements, string $targetLanguage): ?array
    {
        $targetLangStr = strtolower($targetLanguage) === 'fr' ? 'French' : 'Arabic';
        $system = 'You are a professional translator specializing in HR and recruitment. Only output valid JSON, never markdown.';

        $user = "Translate the following job data from its original language into $targetLangStr.
Ensure the translation sounds natural, professional, and uses standard industry terminology in $targetLangStr.
Preserve the exact formatting, including any line breaks (\\n).

MANDATORY: Your response MUST be a valid JSON object ONLY. Do NOT include markdown code blocks.

INPUT DATA:
Title: \"$title\"
Description: \"$description\"
Requirements: \"" . ($requirements ?? 'None provided') . "\"

JSON Structure:
{
  \"title\": \"[Translated title]\",
  \"description\": \"[Translated description]\",
  \"requirements\": \"[Translated requirements]\"
}";

        try {
            $text = $this->chat($system, $user);
        } catch (\RuntimeException $e) {
            return ['error' => $e->getMessage()];
        }

        $decoded = $this->extractJson($text);
        if ($decoded === null) {
            $this->logger->error('Groq translation response was not valid JSON: ' . $text);

            return ['error' => 'AI returned an invalid JSON format.'];
        }

        return $decoded;
    }

    /**
     * Calculates a match percentage between a CV and job requirements.
     */
    public function calculateMatchScore(string $cvText, string $jobRequirements): ?int
    {
        $system = 'You are an expert recruitment AI. Only output valid JSON, never markdown.';

        $user = "Calculate the match percentage between the candidate's CV and the job requirements.
Return ONLY a valid JSON object with a single key 'match_score' containing an integer from 0 to 100.

CV CONTENT:
\"$cvText\"

JOB REQUIREMENTS:
\"$jobRequirements\"";

        try {
            $text = $this->chat($system, $user);
        } catch (\RuntimeException $e) {
            $this->logger->error('Groq match score failed: ' . $e->getMessage());

            return null;
        }

        $decoded = $this->extractJson($text);
        if ($decoded !== null && isset($decoded['match_score'])) {
            return (int) $decoded['match_score'];
        }

        $this->logger->error('Groq match score response was not valid JSON: ' . $text);

        return null;
    }

    private function constructPrompt(string $idea): string
    {
        $categories = ['IT & Software', 'Design & Creative', 'Marketing', 'Writing & Translation', 'Sales & Support', 'Other'];
        $jobTypes = ['Full-time', 'Part-time', 'Contract', 'Freelance', 'Internship'];

        return "Based on the user's idea provided below, generate a complete and formal job posting.
        
        INPUT IDEA: \"$idea\"
        
        MANDATORY: Your response MUST be a valid JSON object only. Do NOT include any markdown formatting.
        
        JSON Structure:
        {
          \"title\": \"A professional and catchy job title\",
          \"description\": \"A detailed job description explaining the role, objectives and company culture. Use plain text and double newlines (\\\\n\\\\n) for paragraph structure. Do NOT use HTML tags.\",
          \"requirements\": \"5 to 7 specific technical and soft skills, separated by newlines\",
          \"minSalary\": [Suggested minimum monthly salary in TND (Numeric)],
          \"maxSalary\": [Suggested maximum monthly salary in TND (Numeric)],
          \"category\": \"[Choose EXACTLY ONE from: " . implode(', ', $categories) . "]\",
          \"jobType\": \"[Choose EXACTLY ONE from: " . implode(', ', $jobTypes) . "]\"
        }
        
        Guidelines:
        - Be realistic for the Tunisian job market.
        - Ensure the 'category' and 'jobType' match the provided list exactly.";
    }
}
