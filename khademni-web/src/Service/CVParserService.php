<?php
// src/Service/CVParserService.php

namespace App\Service;

use Smalot\PdfParser\Parser;
use Psr\Log\LoggerInterface;

class CVParserService
{
    private LoggerInterface $logger;

    public function __construct(LoggerInterface $logger)
    {
        $this->logger = $logger;
    }

    /**
     * Extracts text from a PDF file.
     */
    public function extractTextFromPdf(string $filePath): string
    {
        if (!file_exists($filePath)) {
            $this->logger->error("CV File not found: " . $filePath);
            return "";
        }

        try {
            $parser = new Parser();
            $pdf = $parser->parseFile($filePath);
            return $pdf->getText();
        } catch (\Exception $e) {
            $this->logger->error("Error parsing PDF CV: " . $e->getMessage());
            return "";
        }
    }
}
