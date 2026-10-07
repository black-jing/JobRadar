package com.jobradar.messaging;

import com.jobradar.service.JobService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

@Component
public class JobAnalysisConsumer {
    private final JobService jobService;
    private final RetryTemplate jobAnalysisRetryTemplate;

    public JobAnalysisConsumer(JobService jobService, RetryTemplate jobAnalysisRetryTemplate) {
        this.jobService = jobService;
        this.jobAnalysisRetryTemplate = jobAnalysisRetryTemplate;
    }

    @RabbitListener(queues = JobAnalysisRabbitConfig.QUEUE)
    public void consume(String jobId) {
        Long id = Long.valueOf(jobId);
        jobAnalysisRetryTemplate.execute(context -> {
            jobService.analyzeImportedJob(id);
            return null;
        });
    }
}
