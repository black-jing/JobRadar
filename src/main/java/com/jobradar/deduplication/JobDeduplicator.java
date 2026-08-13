package com.jobradar.deduplication;

import java.util.ArrayList;

import com.jobradar.domain.Job;

import java.util.Set;
import java.util.HashSet;
import java.util.List;

public class JobDeduplicator {
    public List<Job> deduplicate(List<Job> jobs) {
        List<Job> uniqueJobs = new ArrayList<>();
        Set<String> seenKeys = new HashSet<>();
        for (Job job : jobs) {
            String key = job.getCompany()
                    + "|"
                    + job.getTitle()
                    + "|"
                    + job.getLocation();
            if (seenKeys.add(key)) {
                uniqueJobs.add(job);
            }


        }
        return uniqueJobs;
    }
}