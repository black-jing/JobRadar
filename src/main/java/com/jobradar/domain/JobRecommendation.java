package com.jobradar.domain;

public class JobRecommendation {
    private Long jobId;
    private String company;
    private String title;
    private String location;
    private JobMatchResult matchResult;
    public JobRecommendation(
            Job job,
            JobMatchResult matchResult) {

        this.jobId = job.getId();
        this.company = job.getCompany();
        this.title = job.getTitle();
        this.location = job.getLocation();
        this.matchResult = matchResult;
    }
    public Long getJobId() {
        return jobId;
    }

    public String getCompany() {
        return company;
    }

    public String getTitle() {
        return title;
    }

    public String getLocation() {
        return location;
    }

    public JobMatchResult getMatchResult() {
        return matchResult;
    }
}
