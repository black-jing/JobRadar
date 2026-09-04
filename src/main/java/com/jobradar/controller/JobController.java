package com.jobradar.controller;
import com.jobradar.domain.Job;
import com.jobradar.domain.JobAnalysis;
import org.springframework.web.bind.annotation.GetMapping;
import com.jobradar.service.JobService;
import java.time.LocalDate;
import com.jobradar.dto.JobSearchResponse;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import com.jobradar.dto.AnalyzeJobRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;
import com.jobradar.domain.JobMatchResult;
import com.jobradar.domain.UserProfile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import com.jobradar.dto.RecommendJobsRequest;
import com.jobradar.domain.JobRecommendation;
import com.jobradar.domain.ApplicationStatus;
import com.jobradar.domain.JobApplication;
import com.jobradar.domain.ApplicationStatus;
import com.jobradar.domain.JobApplication;
import org.springframework.http.HttpStatus;

import java.util.NoSuchElementException;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.CrossOrigin;
@RestController

@CrossOrigin(origins = "http://localhost:5173")

public class JobController {

    @PostMapping("/api/jobs/analyze")
    public JobAnalysis analyzeJob(
            @Valid @RequestBody AnalyzeJobRequest request) {

        return jobService.analyzeJob(request);
    }
    @PostMapping("/api/jobs/save-sample")
    public Job saveSampleJob() {
        return jobService.saveSampleJob();
    }
    @GetMapping("/api/jobs/saved")
    public List<Job> getSavedJobs() {
        return jobService.getSavedJobs();
    }
    @GetMapping("/jobs/sample")
    public Job getSampleJob() {
        return jobService.getSampleJob();
    }
    private final JobService jobService;
    public JobController(JobService jobService) {
        this.jobService = jobService;
    }
    @GetMapping("/api/jobs")
    public JobSearchResponse getJobs(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "location", required = false) String location) {

        System.out.println("Controller收到的keyword：" + keyword);
        System.out.println("Controller收到的location：" + location);

        List<Job> jobs = jobService.getJobs(keyword, location);

        return new JobSearchResponse(jobs.size(), jobs);
    }
    @PostMapping("/api/jobs/import-one")
    public Job importOneRealJob() {
        return jobService.importOneRealJob();
    }
    @PostMapping("/api/jobs/import-all")
    public List<Job> importAllRealJobs() {
        return jobService.importAllRealJobs();
    }
    @PostMapping("/api/jobs/{id}/match")
    public ResponseEntity<JobMatchResult> matchJob(
            @PathVariable Long id,
            @RequestBody UserProfile userProfile) {

        JobMatchResult result =
                jobService.matchJob(
                        id,
                        userProfile
                );

        if (result == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(result);
    }
    @PostMapping("/api/jobs/recommend")
    public List<JobRecommendation> recommendJobs(
            @RequestBody RecommendJobsRequest request) {

        return jobService.recommendJobs(
                request.getJobIds(),
                request.getUserProfile(),
                request.getTopN()
        );
    }

    @PostMapping("/api/jobs/{id}/application")
    public ResponseEntity<JobApplication> createApplication(
            @PathVariable Long id,
            @RequestParam ApplicationStatus status) {

        try {

            JobApplication application =
                    jobService.createApplication(
                            id,
                            status
                    );

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(application);

        } catch (NoSuchElementException e) {

            return ResponseEntity
                    .notFound()
                    .build();

        } catch (IllegalStateException
                 | IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .build();
        }
    }
    @GetMapping("/api/jobs/{id}/application")
    public ResponseEntity<JobApplication> getApplication(
            @PathVariable Long id) {

        try {

            return ResponseEntity.ok(
                    jobService.getApplication(id)
            );

        } catch (NoSuchElementException e) {

            return ResponseEntity
                    .notFound()
                    .build();
        }
    }
    @PatchMapping("/api/jobs/{id}/application/status")
    public ResponseEntity<JobApplication> updateApplicationStatus(
            @PathVariable Long id,
            @RequestParam ApplicationStatus status) {

        try {

            return ResponseEntity.ok(
                    jobService.updateApplicationStatus(
                            id,
                            status
                    )
            );

        } catch (NoSuchElementException e) {

            return ResponseEntity
                    .notFound()
                    .build();

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .build();
        }
    }
}