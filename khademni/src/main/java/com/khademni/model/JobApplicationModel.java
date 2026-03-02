package com.khademni.model;

import java.time.LocalDateTime;

public class JobApplicationModel {
    private int id;
    private int jobId;
    private String applicantName;
    private String applicantEmail;
    private String title;
    private String description;
    private String cvUrl;
    private String status;
    private LocalDateTime appliedDate;
    private String phoneNumber;

    public JobApplicationModel(int id, int jobId, String applicantName, String applicantEmail,
            String title, String description, String cvUrl, String status, LocalDateTime appliedDate,
            String phoneNumber) {
        this.id = id;
        this.jobId = jobId;
        this.applicantName = applicantName;
        this.applicantEmail = applicantEmail;
        this.title = title;
        this.description = description;
        this.cvUrl = cvUrl;
        this.status = status;
        this.appliedDate = appliedDate;
        this.phoneNumber = phoneNumber;
    }

    public JobApplicationModel(int jobId, String applicantName, String applicantEmail,
            String title, String description, String cvUrl, String status, LocalDateTime appliedDate,
            String phoneNumber) {
        this.jobId = jobId;
        this.applicantName = applicantName;
        this.applicantEmail = applicantEmail;
        this.title = title;
        this.description = description;
        this.cvUrl = cvUrl;
        this.status = status;
        this.appliedDate = appliedDate;
        this.phoneNumber = phoneNumber;
    }

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

    public LocalDateTime getAppliedDate() {
        return appliedDate;
    }

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

    public void setAppliedDate(LocalDateTime appliedDate) {
        this.appliedDate = appliedDate;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
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
                ", phoneNumber='" + phoneNumber + '\'' +
                '}';
    }
}
