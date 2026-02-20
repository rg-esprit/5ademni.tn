package com.khademni.model;

import java.time.LocalDateTime;

public class JobModel {
    private int id;
    private String title;
    private String company;
    private String location;
    private String description;
    private String category;
    private String salaryRange;
    private String jobType; // tnejem tkoun (Full-time,part-time, .....).
    private LocalDateTime postedDate;
    private String[] requirements;
    private int userId;
    private String userName;
    private String userEmail;

    public JobModel(int id, String title, String company, String location,
            String description, String category, String salaryRange,
            String jobType, LocalDateTime postedDate, String[] requirements, int userId) {
        this(id, title, company, location, description, category, salaryRange, jobType, postedDate, requirements,
                userId, "", "");
    }

    public JobModel(int id, String title, String company, String location,
            String description, String category, String salaryRange,
            String jobType, LocalDateTime postedDate, String[] requirements, int userId, String userName) {
        this(id, title, company, location, description, category, salaryRange, jobType, postedDate, requirements,
                userId, userName, "");
    }

    public JobModel(int id, String title, String company, String location,
            String description, String category, String salaryRange,
            String jobType, LocalDateTime postedDate, String[] requirements, int userId, String userName,
            String userEmail) {
        this.id = id;
        this.title = title;
        this.company = company;
        this.location = location;
        this.description = description;
        this.category = category;
        this.salaryRange = salaryRange;
        this.jobType = jobType;
        this.postedDate = postedDate;
        this.requirements = requirements;
        this.userId = userId;
        this.userName = userName;
        this.userEmail = userEmail;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getCompany() {
        return company;
    }

    public String getLocation() {
        return location;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public String getSalaryRange() {
        return salaryRange;
    }

    public String getJobType() {
        return jobType;
    }

    public LocalDateTime getPostedDate() {
        return postedDate;
    }

    public String[] getRequirements() {
        return requirements;
    }

    public int getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setSalaryRange(String salaryRange) {
        this.salaryRange = salaryRange;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    public void setPostedDate(LocalDateTime postedDate) {
        this.postedDate = postedDate;
    }

    public void setRequirements(String[] requirements) {
        this.requirements = requirements;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }
}
