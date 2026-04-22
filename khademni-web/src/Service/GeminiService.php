<?php

namespace App\Service;

use Symfony\Contracts\HttpClient\HttpClientInterface;
use Symfony\Component\DependencyInjection\ParameterBag\ParameterBagInterface;
use Psr\Log\LoggerInterface;

class GeminiService
{
    private HttpClientInterface $httpClient;
    private string $apiKey;
    private LoggerInterface $logger;

    public function __construct(HttpClientInterface $httpClient, ParameterBagInterface $params, LoggerInterface $logger)
    {
        $this->httpClient = $httpClient;
        $this->apiKey = $params->get('app.gemini_api_key');
        $this->logger = $logger;
    }

    /**
     * Generates a full job posting from a simple idea.
     */
    public function generateJobPosting(string $idea): ?array
    {
        // Using gemini-flash-latest as it is confirmed available for this API key
        $apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" . $this->apiKey;

        $prompt = $this->constructPrompt($idea);

        try {
            $response = $this->httpClient->request('POST', $apiUrl, [
                'headers' => [
                    'Content-Type' => 'application/json',
                ],
                'json' => [
                    'contents' => [
                        [
                            'parts' => [
                                ['text' => $prompt]
                            ]
                        ]
                    ]
                ]
            ]);

            if ($response->getStatusCode() !== 200) {
                $errorMsg = $response->getContent(false);
                $this->logger->error('Gemini API Error (' . $response->getStatusCode() . '): ' . $errorMsg);
                return ['error' => 'API Error: ' . $response->getStatusCode() . ' - ' . $errorMsg];
            }

            $result = $response->toArray();
            $text = $result['candidates'][0]['content']['parts'][0]['text'] ?? '';
            
            // Extract JSON from the response
            if (preg_match('/\{.*\}/s', $text, $matches)) {
                $decoded = json_decode($matches[0], true);
                if (json_last_error() === JSON_ERROR_NONE) {
                    return $decoded;
                }
            }

            $this->logger->error('Gemini response was not a valid JSON: ' . $text);
            return ['error' => 'Invalid JSON from Gemini'];
        } catch (\Exception $e) {
            $this->logger->error('Gemini Integration failed: ' . $e->getMessage());
            return ['error' => 'Connection failed: ' . $e->getMessage()];
        }
    }

    /**
     * Analyzes a CV and returns a score, status, and reason.
     */
    public function analyzeCVText(string $cvText): ?array
    {
        $apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" . $this->apiKey;

        $prompt = "You are an expert recruiter and CV evaluator.
Analyze the following CV and evaluate its quality.
Give a score from 0 to 100 based on:
* clarity of experience
* presence of skills
* realism of claims
* completeness of information

Rules:
* 0–40: good and clear CV (Status: SAFE)
* 40–70: average CV (Status: AVERAGE)
* 70–100: suspicious or low quality (Status: SUSPICIOUS)

Return ONLY this JSON format:
{
  \"score\": number,
  \"status\": \"SAFE or AVERAGE or SUSPICIOUS\",
  \"reason\": \"short explanation\"
}

CV CONTENT:
\"$cvText\"";

        try {
            $response = $this->httpClient->request('POST', $apiUrl, [
                'headers' => [
                    'Content-Type' => 'application/json',
                ],
                'json' => [
                    'contents' => [
                        [
                            'parts' => [
                                ['text' => $prompt]
                            ]
                        ]
                    ]
                ]
            ]);

            if ($response->getStatusCode() !== 200) {
                $errorMsg = $response->getContent(false);
                $this->logger->error('Gemini API Error (' . $response->getStatusCode() . '): ' . $errorMsg);
                return ['error' => 'API Error: ' . $response->getStatusCode() . ' - ' . $errorMsg];
            }

            $result = $response->toArray();
            $text = $result['candidates'][0]['content']['parts'][0]['text'] ?? '';
            
            // Log the raw response for debugging
            $this->logger->info('Gemini CV Analysis Raw Response: ' . $text);
            
            if (preg_match('/\{.*\}/s', $text, $matches)) {
                $decoded = json_decode($matches[0], true);
                if (json_last_error() === JSON_ERROR_NONE) {
                    // Ensure the expected keys exist, otherwise it might break the JS
                    if (!isset($decoded['score']) || !isset($decoded['status']) || !isset($decoded['reason'])) {
                        $this->logger->error('Gemini response missing required keys: ' . $text);
                        return null;
                    }
                    return $decoded;
                }
            }

            $this->logger->error('Gemini CV analysis response was not a valid JSON: ' . $text);
            return ['error' => 'Invalid JSON from Gemini'];
        } catch (\Exception $e) {
            $this->logger->error('Gemini CV Analysis failed: ' . $e->getMessage());
            return ['error' => 'Connection failed: ' . $e->getMessage()];
        }
    }

    public function translateJobData(string $title, string $description, ?string $requirements, string $targetLanguage): ?array
    {
        $apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=" . $this->apiKey;

        $targetLangStr = strtolower($targetLanguage) === 'fr' ? 'French' : 'Arabic';

        $prompt = "You are a professional translator specializing in HR and recruitment. 
Translate the following job data from its original language into $targetLangStr.
Ensure the translation sounds natural, professional, and uses standard industry terminology in $targetLangStr.
Preserve the exact formatting, including any line breaks (\\n).

MANDATORY: Your response MUST be a valid JSON object ONLY. Do NOT include markdown code blocks like ```json.

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
            $response = $this->httpClient->request('POST', $apiUrl, [
                'headers' => [
                    'Content-Type' => 'application/json',
                ],
                'json' => [
                    'contents' => [
                        [
                            'parts' => [
                                ['text' => $prompt]
                            ]
                        ]
                    ]
                ]
            ]);

            $data = $response->toArray(false);

            if (isset($data['error'])) {
                $this->logger->error('Gemini API Error (Translation): ' . $data['error']['message']);
                return ['error' => 'API Error: ' . $data['error']['code'] . ' - ' . $data['error']['message']];
            }

            $text = $data['candidates'][0]['content']['parts'][0]['text'] ?? '';
            $text = str_replace(['```json', '```'], '', trim($text));

            $decoded = json_decode($text, true);
            if (json_last_error() === JSON_ERROR_NONE) {
                return $decoded;
            }

            $this->logger->error('Gemini Translation response was not valid JSON: ' . $text);
            return ['error' => 'Invalid JSON from Gemini'];
        } catch (\Exception $e) {
            $this->logger->error('Gemini Translation failed: ' . $e->getMessage());
            return ['error' => 'Connection failed: ' . $e->getMessage()];
        }
    }

    /**
     * Calculates a match percentage between a CV and job requirements.
     */
    public function calculateMatchScore(string $cvText, string $jobRequirements): ?int
    {
        $apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" . $this->apiKey;

        $prompt = "You are an expert recruitment AI. Calculate the match percentage between the candidate's CV and the job requirements.
Return ONLY a valid JSON object with a single key 'match_score' containing an integer from 0 to 100.

CV CONTENT:
\"$cvText\"

JOB REQUIREMENTS:
\"$jobRequirements\"";

        try {
            $response = $this->httpClient->request('POST', $apiUrl, [
                'headers' => [
                    'Content-Type' => 'application/json',
                ],
                'json' => [
                    'contents' => [
                        [
                            'parts' => [
                                ['text' => $prompt]
                            ]
                        ]
                    ]
                ]
            ]);

            if ($response->getStatusCode() !== 200) {
                $this->logger->error('Gemini API Error (Match): ' . $response->getContent(false));
                return null;
            }

            $result = $response->toArray();
            $text = $result['candidates'][0]['content']['parts'][0]['text'] ?? '';
            
            if (preg_match('/\{.*\}/s', $text, $matches)) {
                $decoded = json_decode($matches[0], true);
                if (json_last_error() === JSON_ERROR_NONE && isset($decoded['match_score'])) {
                    return (int)$decoded['match_score'];
                }
            }
            
            return null;
        } catch (\Exception $e) {
            $this->logger->error('Gemini Match Score failed: ' . $e->getMessage());
            return null;
        }
    }

    private function constructPrompt(string $idea): string
    {
        $categories = ['IT & Software', 'Design & Creative', 'Marketing', 'Writing & Translation', 'Sales & Support', 'Other'];
        $jobTypes = ['Full-time', 'Part-time', 'Contract', 'Freelance', 'Internship'];

        return "Act as a professional recruitment expert. Based on the user's idea provided below, generate a complete and formal job posting.
        
        INPUT IDEA: \"$idea\"
        
        MANDATORY: Your response MUST be a valid JSON object only. Do NOT include any markdown formatting like ```json.
        
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
