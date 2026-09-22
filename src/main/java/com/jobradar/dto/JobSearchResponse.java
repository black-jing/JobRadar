package com.jobradar.dto;

import com.jobradar.domain.Job;

import java.util.List;

public class JobSearchResponse {

    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private List<Job> jobs;

    public JobSearchResponse(
            int page,
            int size,
            long totalElements,
            int totalPages,
            List<Job> jobs) {

        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.jobs = jobs;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public List<Job> getJobs() {
        return jobs;
    }
}