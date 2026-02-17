package com.khademni.model;

import java.time.LocalDate;

public class JobApplicationModel {
    private int id;
    private int jobId;
    private String applicantName;
    private String applicantEmail;
    private String title;
    private String description;
    private String cvUrl;
    private String status; // PENDING, ACCEPTED, REJECTED
    private LocalDate appliedDate;

    // Constructor with all fields
    public JobApplicationModel(int id, int jobId, String applicantName, String applicantEmail,
                               String title, String description, String cvUrl, String status, LocalDate appliedDate) {
        this.id = id;
        this.jobId = jobId;
        this.applicantName = applicantName;
        this.applicantEmail = applicantEmail;
        this.title = title;
        this.description = description;
        this.cvUrl = cvUrl;
        this.status = status;
        this.appliedDate = appliedDate;
    }

    // Constructor without id (for new applications)
    public JobApplicationModel(int jobId, String applicantName, String applicantEmail,
                               String title, String description, String cvUrl, String status, LocalDate appliedDate) {
        this.jobId = jobId;
        this.applicantName = applicantName;
        this.applicantEmail = applicantEmail;
        this.title = title;
        this.description = description;
        this.cvUrl = cvUrl;
        this.status = status;
        this.appliedDate = appliedDate;
    }

    // Getters
    public int getId() {
        return id;
    }

    public int getJobId() {
        return jobId;
    }

    public String getApplicantName() {
        return applicantName;
    }

    public String getApplicantEmail() {
        return applicantEmail;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getCvUrl() {
        return cvUrl;
    }

    public String getStatus() {
        return status;
    }

    public LocalDate getAppliedDate() {
        return appliedDate;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setJobId(int jobId) {
        this.jobId = jobId;
    }

    public void setApplicantName(String applicantName) {
        this.applicantName = applicantName;
    }

    public void setApplicantEmail(String applicantEmail) {
        this.applicantEmail = applicantEmail;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCvUrl(String cvUrl) {
        this.cvUrl = cvUrl;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setAppliedDate(LocalDate appliedDate) {
        this.appliedDate = appliedDate;
    }

    @Override
    public String toString() {
        return "JobApplicationModel{" +
                "id=" + id +
                ", jobId=" + jobId +
                ", applicantName='" + applicantName + '\'' +
                ", applicantEmail='" + applicantEmail + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", cvUrl='" + cvUrl + '\'' +
                ", status='" + status + '\'' +
                ", appliedDate=" + appliedDate +
                '}';
    }
}
