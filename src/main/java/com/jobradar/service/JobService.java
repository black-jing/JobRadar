package com.jobradar.service;

import com.jobradar.domain.Job;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import com.jobradar.aggregation.JobAggregator;
import com.jobradar.cleaning.JobCleaner;
import com.jobradar.deduplication.JobDeduplicator;
import com.jobradar.source.JobSource;
import com.jobradar.source.RemotiveJobSource;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;
@Service
public class JobService {

    public Job getSampleJob() {
        return new Job(
                "字节跳动",
                "Java后端开发实习生",
                "北京",
                "负责后端服务开发",
                LocalDate.of(2026, 8, 14),
                "JobRadar Test",
                "https://example.com/job/1"
        );
    }
    public List<Job> getJobs(String keyword, String location ) {
        System.out.println("Service收到的keyword：" + keyword);
        System.out.println("Service收到的location：" + location);

        List<JobSource> sources = new ArrayList<>();
        sources.add(new RemotiveJobSource());

        JobAggregator aggregator = new JobAggregator(sources);
        List<Job> jobs = aggregator.aggregateJobs();

        JobCleaner cleaner = new JobCleaner();
        List<Job> cleanedJobs = new ArrayList<>();

        for (Job job : jobs) {
            Job cleanedJob = cleaner.clean(job);

            if (cleanedJob != null) {
                cleanedJobs.add(cleanedJob);
            }
        }

        JobDeduplicator deduplicator = new JobDeduplicator();

        List<Job> finalJobs = deduplicator.deduplicate(cleanedJobs);
        List<Job> resultJobs = finalJobs;
        if (keyword != null && !keyword.isBlank()) {

            List<Job> filteredJobs = new ArrayList<>();

            for (Job job : resultJobs) {
                if (job.getTitle() != null
                        && job.getTitle().toLowerCase().contains(keyword.toLowerCase())) {
                    filteredJobs.add(job);
                }
            }

            resultJobs = filteredJobs;
        }
        if (location != null && !location.isBlank()) {

            List<Job> locationFilteredJobs = new ArrayList<>();

            for (Job job : resultJobs) {
                if (job.getLocation() != null
                        && job.getLocation().toLowerCase().contains(location.toLowerCase())) {
                    locationFilteredJobs.add(job);
                }
            }

            resultJobs = locationFilteredJobs;
        }
        System.out.println("关键词筛选后数量：" + resultJobs.size());
        return resultJobs;
    }
}