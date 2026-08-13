package com.jobradar.sandbox;

import com.jobradar.domain.Job;
import com.jobradar.source.JobSource;
import com.jobradar.source.mock.MockJobSourceA;
import com.jobradar.source.mock.MockJobSourceB;

import java.util.List;

public class SourceSandbox {

    public static void main(String[] args) {
        JobSource sourceA = new MockJobSourceA();
        JobSource sourceB = new MockJobSourceB();
        List<Job> jobsA = sourceA.fetchJobs();
        List<Job> jobsB = sourceB.fetchJobs();
        System.out.println("=== Source A ===");

        for (Job job : jobsA) {
            System.out.println(job);
        }
        for (Job job : jobsB) {
            System.out.println(job);
        }

    }
}