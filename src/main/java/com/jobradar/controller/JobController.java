package com.jobradar.controller;
import com.jobradar.domain.Job;
import org.springframework.web.bind.annotation.GetMapping;
import com.jobradar.service.JobService;
import java.time.LocalDate;
import com.jobradar.dto.JobSearchResponse;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
@RestController
public class JobController {

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
}