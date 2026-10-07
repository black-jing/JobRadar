package com.jobradar.messaging;

import com.jobradar.service.JobService;
import org.junit.jupiter.api.Test;
import org.springframework.retry.support.RetryTemplate;

import java.net.http.HttpTimeoutException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class JobAnalysisConsumerTest {
    private final JobService jobService = mock(JobService.class);
    private final RetryTemplate retryTemplate = new JobAnalysisRabbitConfig().jobAnalysisRetryTemplate();
    private final JobAnalysisConsumer consumer = new JobAnalysisConsumer(jobService, retryTemplate);

    @Test
    void transientFailureRetriesAtMostThreeTimes() {
        doThrow(new RetryableJobAnalysisException("timeout", new HttpTimeoutException("timeout")))
                .when(jobService).analyzeImportedJob(7L);

        assertThrows(RetryableJobAnalysisException.class, () -> consumer.consume("7"));
        verify(jobService, times(3)).analyzeImportedJob(7L);
    }

    @Test
    void permanentFailureIsNotRetried() {
        doThrow(new IllegalStateException("invalid result"))
                .when(jobService).analyzeImportedJob(7L);

        assertThrows(IllegalStateException.class, () -> consumer.consume("7"));
        verify(jobService, times(1)).analyzeImportedJob(7L);
    }
}
