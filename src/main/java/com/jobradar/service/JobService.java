package com.jobradar.service;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.jobradar.domain.JobAnalysis;
import com.jobradar.domain.JobMatchResult;
import com.jobradar.domain.UserProfile;
import com.jobradar.dto.AnalyzeJobRequest;
import com.jobradar.domain.Job;
import java.time.LocalDate;
import com.jobradar.domain.JobRecommendation;
import java.util.Comparator;
import com.jobradar.matching.JobMatcher;
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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
@Service
public class JobService {
    public JobAnalysis analyzeJob(AnalyzeJobRequest request) {
        System.out.println("进入 JobService.analyzeJob()");
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
    private final JobMatcher jobMatcher;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private static final Duration JOB_ANALYSIS_CACHE_TTL =
            Duration.ofHours(24);
    public JobService(
            JobAnalyzer jobAnalyzer,
            JobRepository jobRepository,
            JobMatcher jobMatcher,
            StringRedisTemplate stringRedisTemplate,
            ObjectMapper objectMapper) {

        this.jobAnalyzer = jobAnalyzer;
        this.jobRepository = jobRepository;
        this.jobMatcher = jobMatcher;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
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
    public Job importOneRealJob() {

        List<Job> jobs = getJobs(null, null);

        if (jobs.isEmpty()) {
            return null;
        }

        Job job = jobs.get(0);

        Job existingJob =
                jobRepository.findBySourceAndSourceUrl(
                        job.getSource(),
                        job.getSourceUrl()
                );

        if (existingJob != null) {
            return existingJob;
        }

        return jobRepository.save(job);
    }
    public List<Job> importAllRealJobs() {

        List<Job> jobs = getJobs(null, null);

        List<Job> savedJobs = new ArrayList<>();

        for (Job job : jobs) {

            Job existingJob =
                    jobRepository.findBySourceAndSourceUrl(
                            job.getSource(),
                            job.getSourceUrl()
                    );

            if (existingJob == null) {
                Job savedJob = jobRepository.save(job);
                savedJobs.add(savedJob);
            }
        }

        return savedJobs;
    }
    public JobMatchResult matchJob(
            Long id,
            UserProfile userProfile) {

        Job job = jobRepository
                .findById(id)
                .orElse(null);

        if (job == null) {
            return null;
        }

        return jobMatcher.match(
                job,
                userProfile
        );
    }
    public List<JobRecommendation> recommendJobs(
            List<Long> jobIds,
            UserProfile userProfile,
            int topN) {
        if (jobIds == null
                || jobIds.size() < 3
                || jobIds.size() > 5) {

            throw new IllegalArgumentException(
                    "第一版推荐只允许选择3到5个岗位"
            );
        }
        List<Job> jobs =
                jobRepository.findAllById(jobIds);
        List<JobRecommendation> recommendations =
                new ArrayList<>();
        for (Job job : jobs) {

            try {

                JobMatchResult matchResult =
                        jobMatcher.match(
                                job,
                                userProfile
                        );

                JobRecommendation recommendation =
                        new JobRecommendation(
                                job,
                                matchResult
                        );

                recommendations.add(
                        recommendation
                );

            } catch (RuntimeException e) {

                System.out.println(
                        "岗位匹配失败，jobId="
                                + job.getId()
                                + "，跳过该岗位"
                );
            }
        }
        recommendations.sort(
                Comparator.comparingInt(
                        (JobRecommendation recommendation) ->
                                recommendation
                                        .getMatchResult()
                                        .getScore()
                ).reversed()
        );
        int limit =
                Math.min(
                        topN,
                        recommendations.size()
                );
        return new ArrayList<>(
                recommendations.subList(
                        0,
                        limit
                )
        );
    }
}