package com.khademni.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates the result of spam analysis on a gig submission.
 * Contains the computed spam score, a boolean verdict, and
 * human-readable reasons explaining why the content was flagged.
 */
public class SpamResult {

    private int spamScore;
    private boolean isSpam;
    private List<String> reasons;

    public SpamResult() {
        this.spamScore = 0;
        this.isSpam = false;
        this.reasons = new ArrayList<>();
    }

    public SpamResult(int spamScore, boolean isSpam, List<String> reasons) {
        this.spamScore = spamScore;
        this.isSpam = isSpam;
        this.reasons = reasons != null ? reasons : new ArrayList<>();
    }

    // Getters
    public int getSpamScore() {
        return spamScore;
    }

    public boolean isSpam() {
        return isSpam;
    }

    public List<String> getReasons() {
        return reasons;
    }

    // Setters
    public void setSpamScore(int spamScore) {
        this.spamScore = spamScore;
    }

    public void setSpam(boolean spam) {
        isSpam = spam;
    }

    public void setReasons(List<String> reasons) {
        this.reasons = reasons;
    }

    /**
     * Adds points to the spam score and records the reason for flagging.
     */
    public void addPenalty(int points, String reason) {
        this.spamScore += points;
        this.reasons.add(reason);
    }

    @Override
    public String toString() {
        return "SpamResult{" +
                "spamScore=" + spamScore +
                ", isSpam=" + isSpam +
                ", reasons=" + reasons +
                '}';
    }
}
