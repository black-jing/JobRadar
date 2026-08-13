package com.jobradar.sandbox;

import com.jobradar.aggregation.JobAggregator;
import com.jobradar.domain.Job;
import com.jobradar.source.JobSource;
import com.jobradar.source.mock.MockJobSourceA;
import com.jobradar.source.mock.MockJobSourceB;
import com.jobradar.source.mock.MockJobSourceC;

import java.util.ArrayList;
import java.util.List;

public class AggregatorSandbox {

    public static void main(String[] args) {

        List<JobSource> sources = new ArrayList<>();

        sources.add(new MockJobSourceA());
        sources.add(new MockJobSourceB());
        sources.add(new MockJobSourceC());

        JobAggregator aggregator = new JobAggregator(sources);

        List<Job> jobs = aggregator.aggregateJobs();
        List<Job> javajob=jobs.stream().filter(job->job.getTitle().contains("后端")).toList();
   javajob.forEach(job->System.out.println(job));
    }
}