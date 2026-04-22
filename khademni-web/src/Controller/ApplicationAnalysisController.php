<?php

namespace App\Controller;

use App\Entity\JobApplication;
use App\Service\CVParserService;
use App\Service\GeminiService;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\Security\Http\Attribute\IsGranted;

#[Route('/management/applications')]
class ApplicationAnalysisController extends AbstractController
{
    #[Route('/{id}/analyze', name: 'app_management_application_analyze', methods: ['POST'])]
    public function analyze(
        JobApplication $application,
        CVParserService $cvParser,
        GeminiService $gemini,
        EntityManagerInterface $em
    ): JsonResponse {
        // Security: Only the job owner can analyze
        $job = $application->getJob();
        if ($job->getUser() !== $this->getUser() && !$this->isGranted('ROLE_ADMIN')) {
            return new JsonResponse(['error' => 'Unauthorized'], 403);
        }

        $cvPath = $application->getCvPath();
        if (!$cvPath) {
            return new JsonResponse(['error' => 'No CV uploaded for this application.'], 400);
        }

        // Resolve absolute path (assuming stored in public/uploads or similar)
        // Adjust this path based on where files are actually stored
        $projectDir = $this->getParameter('kernel.project_dir');
        $absolutePath = $projectDir . '/public/' . ltrim($cvPath, '/');

        $text = $cvParser->extractTextFromPdf($absolutePath);

        if (empty(trim($text))) {
            // Fallback: Use the cover letter description if PDF is empty or unreadable
            $text = "Cover Letter Content: " . $application->getDescription();
        }

        if (empty(trim($text))) {
            return new JsonResponse(['error' => 'Could not extract any text from CV or cover letter.'], 400);
        }

        $analysis = $gemini->analyzeCVText($text);

        if (!$analysis) {
            return new JsonResponse(['error' => 'AI analysis failed. Please check your API key and connection.'], 500);
        }

        // Calculate Match Score if missing
        if ($application->getAiMatchScore() === null) {
            $jobRequirements = $job->getRequirements() ?: $job->getDescription();
            if (!empty(trim($jobRequirements))) {
                $matchScore = $gemini->calculateMatchScore($text, $jobRequirements);
                if ($matchScore !== null) {
                    $application->setAiMatchScore($matchScore);
                    $em->flush();
                }
            }
        }
        
        $analysis['matchScore'] = $application->getAiMatchScore();

        return new JsonResponse($analysis);
    }
}
