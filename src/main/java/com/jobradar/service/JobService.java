package com.jobradar.service;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.jobradar.domain.JobAnalysis;
import com.jobradar.dto.AnalyzeJobRequest;
import com.jobradar.domain.Job;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import com.jobradar.aggregation.JobAggregator;
import com.jobradar.cleaning.JobCleaner;
import com.jobradar.deduplication.JobDeduplicator;
import com.jobradar.source.JobSource;
import com.jobradar.source.RemotiveJobSource;
import org.springframework.web.bind.annotation.RequestParam;
import com.jobradar.analysis.JobAnalyzer;
import java.util.ArrayList;
import java.util.List;
import com.jobradar.repository.JobRepository;
@Service
public class JobService {
    public JobAnalysis analyzeJob(AnalyzeJobRequest request) {

        Job job = new Job(
                request.getCompany(),
                request.getTitle(),
                request.getLocation(),
                request.getDescription(),
                null,
                "API Request",
                null
        );

        try {
            return jobAnalyzer.analyze(job);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("岗位分析失败", e);
        }
    }
    private final JobAnalyzer jobAnalyzer;
    private final JobRepository jobRepository;
    public JobService(
            JobAnalyzer jobAnalyzer,
            JobRepository jobRepository) {

        this.jobAnalyzer = jobAnalyzer;
        this.jobRepository = jobRepository;
    }
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
    public Job saveSampleJob() {

        Job job = new Job(
                "JobRadar Test Company",
                "Java Backend Intern",
                "Remote",
                "用于测试JobRadar数据库持久化",
                LocalDate.now(),
                "Database Test",
                "https://example.com/database-test"
        );

        return jobRepository.save(job);
    }
    public List<Job> getSavedJobs() {
        return jobRepository.findAll();
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