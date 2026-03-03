package com.khademni.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates the result of AI-powered bad word / profanity detection.
 * Returned by BadWordDetectionService after analyzing gig content via DeepSeek API.
 */
public class BadWordResult {

    /** Whether any bad words or inappropriate content was detected */
    private boolean hasBadWords;

    /** The specific words/phrases that were flagged */
    private List<String> detectedWords;

    /** Severity level: LOW, MEDIUM, or HIGH */
    private String severity;

    /** AI-generated explanation of why the content was flagged */
    private String explanation;

    public BadWordResult() {
        this.hasBadWords = false;
        this.detectedWords = new ArrayList<>();
        this.severity = "NONE";
        this.explanation = "";
    }

    public BadWordResult(boolean hasBadWords, List<String> detectedWords, String severity, String explanation) {
        this.hasBadWords = hasBadWords;
        this.detectedWords = detectedWords != null ? detectedWords : new ArrayList<>();
        this.severity = severity != null ? severity : "NONE";
        this.explanation = explanation != null ? explanation : "";
    }

    // Getters
    public boolean hasBadWords() {
        return hasBadWords;
    }

    public List<String> getDetectedWords() {
        return detectedWords;
    }

    public String getSeverity() {
        return severity;
    }

    public String getExplanation() {
        return explanation;
    }

    // Setters
    public void setHasBadWords(boolean hasBadWords) {
        this.hasBadWords = hasBadWords;
    }

    public void setDetectedWords(List<String> detectedWords) {
        this.detectedWords = detectedWords != null ? detectedWords : new ArrayList<>();
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    @Override
    public String toString() {
        return "BadWordResult{" +
                "hasBadWords=" + hasBadWords +
                ", detectedWords=" + detectedWords +
                ", severity='" + severity + '\'' +
                ", explanation='" + explanation + '\'' +
                '}';
    }
}
