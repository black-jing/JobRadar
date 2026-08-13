package com.jobradar.cleaning;

import com.jobradar.domain.Job;

public class JobCleaner {
    public Job clean(Job job) {
        if (job == null) {
            return null;
        }
        String company = job.getCompany();
        String title = job.getTitle();
        String location = job.getLocation();
        if (company == null || company.isBlank()) {
            return null;
        }
        if (title == null || title.isBlank()) {
            return null;
        }
        company = company.trim();
        title = title.trim();
        if (location == null) {
            location = "";
        } else {
            location = location.trim();
        }
        Job cleanedJob = new Job(company, title, location, job.getDescription(), job.getPublishDate(), job.getSource(), job.getSourceUrl());
        return cleanedJob;
    }
}