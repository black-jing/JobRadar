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
@RestController
public class JobController {

    @PostMapping("/api/jobs/analyze")
    public JobAnalysis analyzeJob(
            @RequestBody AnalyzeJobRequest request) {

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
}