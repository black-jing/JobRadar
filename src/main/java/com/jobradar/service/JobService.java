package com.jobradar.service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobradar.aggregation.JobAggregator;
import com.jobradar.analysis.JobAnalyzer;
import com.jobradar.cleaning.JobCleaner;
import com.jobradar.deduplication.JobDeduplicator;
import com.jobradar.domain.ApplicationStatus;
import com.jobradar.domain.Job;
import com.jobradar.domain.JobAnalysis;
import com.jobradar.domain.JobAnalysisSubmission;
import com.jobradar.domain.JobApplication;
import com.jobradar.domain.JobMatchResult;
import com.jobradar.domain.JobRecommendation;
import com.jobradar.domain.UserProfile;
import com.jobradar.dto.AnalyzeJobRequest;
import com.jobradar.matching.JobMatcher;
import com.jobradar.repository.JobApplicationRepository;
import com.jobradar.repository.JobRepository;
import com.jobradar.repository.JobAnalysisSubmissionRepository;
import com.jobradar.messaging.JobAnalysisProducer;
import com.jobradar.source.JobSource;
import com.jobradar.source.RemotiveJobSource;
import com.jobradar.source.XiaozhaoRadarJobSource;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
@Service
public class JobService {

    private final JobAnalyzer jobAnalyzer;
    private final JobRepository jobRepository;
    private final JobMatcher jobMatcher;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final JobApplicationRepository jobApplicationRepository;
    private final UserProfileService userProfileService;
    private final JobAnalysisProducer jobAnalysisProducer;
    private final JobAnalysisSubmissionRepository submissionRepository;
    private final TransactionTemplate transactionTemplate;

    private static final Duration JOB_ANALYSIS_CACHE_TTL =
            Duration.ofHours(24);

    public JobService(
            JobAnalyzer jobAnalyzer,
            JobRepository jobRepository,
            JobApplicationRepository jobApplicationRepository,
            UserProfileService userProfileService,
            JobMatcher jobMatcher,
            StringRedisTemplate stringRedisTemplate,
            ObjectMapper objectMapper,
            JobAnalysisProducer jobAnalysisProducer,
            JobAnalysisSubmissionRepository submissionRepository,
            TransactionTemplate transactionTemplate) {

        this.jobAnalyzer = jobAnalyzer;
        this.jobRepository = jobRepository;
        this.jobApplicationRepository = jobApplicationRepository;
        this.userProfileService = userProfileService;
        this.jobMatcher = jobMatcher;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.jobAnalysisProducer = jobAnalysisProducer;
        this.submissionRepository = submissionRepository;
        this.transactionTemplate = transactionTemplate;
    }


    // =========================
    // AI 岗位分析
    // =========================

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

        return analyzeWithCache(job);
    }

    @Transactional
    public void analyzeImportedJob(Long jobId) {
        Job job = jobRepository.findByIdForAnalysis(jobId)
                .orElseThrow(() -> new NoSuchElementException("岗位不存在，jobId=" + jobId));

        if (jobAnalyzer.getPromptVersion().equals(job.getAnalysisPromptVersion())
                && job.getAnalysisJson() != null
                && !job.getAnalysisJson().isBlank()) {
            try {
                JobAnalysis saved = objectMapper.readValue(job.getAnalysisJson(), JobAnalysis.class);
                if (isValidAnalysis(saved)) {
                    return;
                }
            } catch (JsonProcessingException ignored) {
                // Invalid stored JSON is regenerated.
            }
        }

        JobAnalysis analysis = analyzeWithCache(job);
        if (!isValidAnalysis(analysis)) {
            throw new IllegalStateException("岗位分析结果无效，jobId=" + jobId);
        }
        try {
            job.saveAnalysis(objectMapper.writeValueAsString(analysis), jobAnalyzer.getPromptVersion());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("岗位分析结果序列化失败，jobId=" + jobId, e);
        }
        jobRepository.saveAndFlush(job);
    }

    private boolean isValidAnalysis(JobAnalysis analysis) {
        return analysis != null
                && analysis.getDirection() != null && !analysis.getDirection().isBlank()
                && analysis.getSkills() != null
                && analysis.getSummary() != null && !analysis.getSummary().isBlank();
    }

    private JobAnalysis analyzeWithCache(Job job) {
        String cacheKey = buildJobAnalysisCacheKey(job);

        System.out.println(
                "JobAnalysis cache key: " + cacheKey
        );

        String cachedValue =
                stringRedisTemplate
                        .opsForValue()
                        .get(cacheKey);

        try {

            if (cachedValue != null) {

                System.out.println("CACHE HIT");

                try {
                    JobAnalysis cached = objectMapper.readValue(
                            cachedValue,
                            JobAnalysis.class
                    );
                    if (isValidAnalysis(cached)) {
                        return cached;
                    }
                } catch (JsonProcessingException ignored) {
                    // A malformed cache entry is treated as a miss.
                }
            }

            System.out.println("CACHE MISS");

            JobAnalysis analysis =
                    jobAnalyzer.analyze(job);

            String analysisJson =
                    objectMapper.writeValueAsString(
                            analysis
                    );

            stringRedisTemplate
                    .opsForValue()
                    .set(
                            cacheKey,
                            analysisJson,
                            JOB_ANALYSIS_CACHE_TTL
                    );

            return analysis;

        } catch (JsonProcessingException e) {

            throw new RuntimeException(
                    "岗位分析或缓存JSON处理失败",
                    e
            );
        }
    }

    private String buildJobAnalysisCacheKey(Job job) {

        String rawKey =
                String.valueOf(job.getCompany())
                        + "\n"
                        + String.valueOf(job.getTitle())
                        + "\n"
                        + String.valueOf(job.getLocation())
                        + "\n"
                        + String.valueOf(job.getDescription());

        try {

            MessageDigest digest =
                    MessageDigest.getInstance(
                            "SHA-256"
                    );

            byte[] hash =
                    digest.digest(
                            rawKey.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            return "job:analysis:"
                    + jobAnalyzer.getPromptVersion()
                    + ":"
                    + HexFormat.of()
                    .formatHex(hash);

        } catch (NoSuchAlgorithmException e) {

            throw new IllegalStateException(
                    "无法生成JobAnalysis缓存Key",
                    e
            );
        }
    }


    // =========================
    // 测试岗位
    // =========================

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


    // =========================
    // 数据库岗位查询
    // =========================

    public List<Job> getSavedJobs() {

        return jobRepository.findAll();
    }
    public Page<Job> searchSavedJobs(
            String keyword,
            String location,
            String source,
            int page,
            int size) {

        if (page < 0) {
            page = 0;
        }

        if (size <= 0) {
            size = 10;
        }

        if (size > 100) {
            size = 100;
        }

        Pageable pageable =
                PageRequest.of(
                        page,
                        size
                );

        return jobRepository.searchJobs(
                keyword,
                location,
                source,
                pageable
        );
    }

    // =========================
    // 外部岗位来源采集
    // =========================

    private List<Job> fetchJobsFromSources() {

        List<JobSource> sources =
                new ArrayList<>();

        sources.add(
                new RemotiveJobSource()
        );

        sources.add(
                new XiaozhaoRadarJobSource()
        );

        JobAggregator aggregator =
                new JobAggregator(sources);

        List<Job> jobs =
                aggregator.aggregateJobs();

        JobCleaner cleaner =
                new JobCleaner();

        List<Job> cleanedJobs =
                new ArrayList<>();

        for (Job job : jobs) {

            Job cleanedJob =
                    cleaner.clean(job);

            if (cleanedJob != null) {
                cleanedJobs.add(cleanedJob);
            }
        }

        JobDeduplicator deduplicator =
                new JobDeduplicator();

        List<Job> finalJobs =
                deduplicator.deduplicate(
                        cleanedJobs
                );

        System.out.println(
                "Source获取并处理后岗位数量："
                        + finalJobs.size()
        );

        return finalJobs;
    }


    // =========================
    // 岗位导入
    // =========================

    public Job importOneRealJob() {

        submitPendingAnalyses();

        List<Job> jobs =
                fetchJobsFromSources();

        if (jobs.isEmpty()) {
            return null;
        }

        Job job =
                jobs.get(0);

        Job savedJob = jobRepository
                .findBySourceAndSourceUrl(
                        job.getSource(),
                        job.getSourceUrl()
                )
                .orElseGet(() -> saveNewImportedJob(job));
        submitPendingAnalysis(savedJob);
        return savedJob;
    }

    public List<Job> importAllRealJobs() {

        submitPendingAnalyses();

        List<Job> jobs =
                fetchJobsFromSources();

        List<Job> savedJobs =
                new ArrayList<>();

        for (Job job : jobs) {

            Job existing =
                    jobRepository
                            .findBySourceAndSourceUrl(
                                    job.getSource(),
                                    job.getSourceUrl()
                            )
                            .orElse(null);

            if (existing == null) {

                Job savedJob = saveNewImportedJob(job);
                submitPendingAnalysis(savedJob);

                savedJobs.add(savedJob);
            } else {
                submitPendingAnalysis(existing);
            }
        }

        return savedJobs;
    }

    private Job saveNewImportedJob(Job job) {
        return transactionTemplate.execute(status -> {
            Job saved = jobRepository.saveAndFlush(job);
            submissionRepository.saveAndFlush(new JobAnalysisSubmission(saved.getId()));
            return saved;
        });
    }

    private void submitPendingAnalysis(Job job) {
        if (submissionRepository.findById(job.getId())
                .map(JobAnalysisSubmission::isPending).orElse(false)) {
            jobAnalysisProducer.submit(job.getId());
        }
    }

    private void submitPendingAnalyses() {
        for (JobAnalysisSubmission submission : submissionRepository.findByPendingTrue()) {
            jobAnalysisProducer.submit(submission.getJobId());
        }
    }


    // =========================
    // 单岗位匹配
    // =========================

    public JobMatchResult matchJob(
            Long id) {

        Job job =
                jobRepository
                        .findById(id)
                        .orElse(null);

        if (job == null) {
            return null;
        }

        UserProfile userProfile =
                userProfileService.getRequiredProfile();

        return jobMatcher.match(
                job,
                userProfile
        );
    }


    // =========================
    // 多岗位推荐
    // =========================

    public List<JobRecommendation> recommendJobs(
            List<Long> jobIds,
            int topN) {

        if (jobIds == null
                || jobIds.size() < 3
                || jobIds.size() > 5) {

            throw new IllegalArgumentException(
                    "第一版推荐只允许选择3到5个岗位"
            );
        }

        UserProfile userProfile =
                userProfileService.getRequiredProfile();

        List<Job> jobs =
                jobRepository.findAllById(
                        jobIds
                );

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


    // =========================
    // 投递记录
    // =========================
    @Transactional
    public JobApplication createApplication(
            Long jobId,
            ApplicationStatus status) {

        Job job =
                jobRepository
                        .findById(jobId)
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "岗位不存在，jobId="
                                                        + jobId
                                        )
                        );

        if (jobApplicationRepository
                .findByJob_Id(jobId)
                .isPresent()) {

            throw new IllegalStateException(
                    "该岗位已经存在投递记录"
            );
        }

        if (status != ApplicationStatus.SAVED
                && status != ApplicationStatus.APPLIED) {

            throw new IllegalArgumentException(
                    "新建投递记录时，初始状态只能是SAVED或APPLIED"
            );
        }

        JobApplication application =
                new JobApplication(
                        job,
                        status
                );

        return jobApplicationRepository
                .save(application);
    }

    public JobApplication getApplication(
            Long jobId) {

        return jobApplicationRepository
                .findByJob_Id(jobId)
                .orElseThrow(
                        () ->
                                new NoSuchElementException(
                                        "该岗位暂无投递记录，jobId="
                                                + jobId
                                )
                );
    }
    @Transactional
    public JobApplication updateApplicationStatus(
            Long jobId,
            ApplicationStatus newStatus) {

        JobApplication application =
                jobApplicationRepository
                        .findByJob_Id(jobId)
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "该岗位暂无投递记录，jobId="
                                                        + jobId
                                        )
                        );

        ApplicationStatus currentStatus =
                application.getStatus();

        if (currentStatus == newStatus) {
            return application;
        }

        if (!isValidStatusTransition(
                currentStatus,
                newStatus)) {

            throw new IllegalArgumentException(
                    "不允许的状态变化："
                            + currentStatus
                            + " -> "
                            + newStatus
            );
        }

        application.updateStatus(
                newStatus
        );

        return jobApplicationRepository
                .save(application);
    }

    private boolean isValidStatusTransition(
            ApplicationStatus currentStatus,
            ApplicationStatus newStatus) {

        return switch (currentStatus) {

            case SAVED ->
                    newStatus
                            == ApplicationStatus.APPLIED;

            case APPLIED ->
                    newStatus
                            == ApplicationStatus.INTERVIEW
                            || newStatus
                            == ApplicationStatus.OFFER
                            || newStatus
                            == ApplicationStatus.REJECTED;

            case INTERVIEW ->
                    newStatus
                            == ApplicationStatus.OFFER
                            || newStatus
                            == ApplicationStatus.REJECTED;

            case OFFER, REJECTED -> false;
        };
    }
}
