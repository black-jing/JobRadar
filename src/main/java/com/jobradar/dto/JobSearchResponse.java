package com.jobradar.dto;

import com.jobradar.domain.Job;

import java.util.List;

public class JobSearchResponse {

    private int count;
    private List<Job> jobs;
    public JobSearchResponse(int count, List<Job> jobs) {
        this.count = count;
        this.jobs = jobs;
    }
    public int getCount() {
        return count;
    }

    public List<Job> getJobs() {
        return jobs;
    }
}