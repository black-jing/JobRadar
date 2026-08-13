package com.jobradar.sandbox;
import com.jobradar.source.RemotiveJobSource;
import com.jobradar.aggregation.JobAggregator;
import com.jobradar.cleaning.JobCleaner;
import com.jobradar.deduplication.JobDeduplicator;
import com.jobradar.domain.Job;
import com.jobradar.source.JobSource;
import com.jobradar.source.mock.MockJobSourceA;
import com.jobradar.source.mock.MockJobSourceB;
import com.jobradar.source.mock.MockJobSourceC;

import java.util.ArrayList;
import java.util.List;

public class Day6IntegrationSandbox {

    public static void main(String[] args) {
        List<JobSource> sources = new ArrayList<>();

        sources.add(new MockJobSourceA());
        sources.add(new MockJobSourceB());
        sources.add(new MockJobSourceC());
        sources.add(new RemotiveJobSource());
        JobAggregator aggregator = new JobAggregator(sources);

        List<Job> jobs = aggregator.aggregateJobs();
        JobDeduplicator deduplicator = new JobDeduplicator();
        JobCleaner cleaner = new JobCleaner();

        List<Job> cleanedJobs = new ArrayList<>();
        for (Job job : jobs) {

            Job cleanedJob = cleaner.clean(job);

            if (cleanedJob != null) {
                cleanedJobs.add(cleanedJob);
            }
        }
        List<Job> finalJobs = deduplicator.deduplicate(cleanedJobs);
        System.out.println("===== 聚合后的岗位 =====");
        System.out.println("岗位数量：" + jobs.size());

        for (Job job : jobs) {
            System.out.println(job);
        }
        System.out.println("===== 清洗后的岗位 =====");
        System.out.println("岗位数量：" + cleanedJobs.size());

        for (Job job : cleanedJobs) {
            System.out.println(job);
        }
        System.out.println("===== 最终岗位 =====");
        System.out.println("最终岗位数量：" + finalJobs.size());

        for (Job job : finalJobs) {
            System.out.println(job);
        }
    }
}