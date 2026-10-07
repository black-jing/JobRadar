package com.jobradar.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobradar.analysis.JobAnalyzer;
import com.jobradar.domain.Job;
import com.jobradar.domain.JobAnalysis;
import com.jobradar.matching.JobMatcher;
import com.jobradar.messaging.JobAnalysisProducer;
import com.jobradar.repository.JobApplicationRepository;
import com.jobradar.repository.JobRepository;
import com.jobradar.repository.JobAnalysisSubmissionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ImportedJobAnalysisTest {
    private final JobAnalyzer analyzer = mock(JobAnalyzer.class);
    private final JobRepository repository = mock(JobRepository.class);
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final JobService service = new JobService(analyzer, repository,
            mock(JobApplicationRepository.class), mock(UserProfileService.class),
            mock(JobMatcher.class), redis, mapper, mock(JobAnalysisProducer.class),
            mock(JobAnalysisSubmissionRepository.class), mock(TransactionTemplate.class));

    private Job job() {
        return new Job("Company", "Developer", "Remote", "Java development",
                LocalDate.now(), "Source", "https://example.com/job");
    }

    @Test
    void duplicateDeliveryUsesValidMysqlAnalysis() throws Exception {
        Job job = job();
        job.saveAnalysis(mapper.writeValueAsString(
                new JobAnalysis("Backend", List.of("Java"), "Build services")), "v2");
        when(repository.findByIdForAnalysis(1L)).thenReturn(Optional.of(job));
        when(analyzer.getPromptVersion()).thenReturn("v2");

        service.analyzeImportedJob(1L);

        verifyNoInteractions(redis);
        verify(analyzer, never()).analyze(any());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void newAnalysisIsSavedInExistingJsonFormat() throws Exception {
        Job job = job();
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(repository.findByIdForAnalysis(1L)).thenReturn(Optional.of(job));
        when(analyzer.getPromptVersion()).thenReturn("v2");
        when(redis.opsForValue()).thenReturn(values);
        when(analyzer.analyze(job)).thenReturn(
                new JobAnalysis("Backend", List.of("Java"), "Build services"));

        service.analyzeImportedJob(1L);

        assertEquals("v2", job.getAnalysisPromptVersion());
        assertEquals("Backend", mapper.readValue(job.getAnalysisJson(), JobAnalysis.class).getDirection());
        verify(repository).saveAndFlush(job);
    }

    @Test
    void missingJobFailsWithoutCallingAnalyzer() throws Exception {
        when(repository.findByIdForAnalysis(1L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> service.analyzeImportedJob(1L));
        verify(analyzer, never()).analyze(any());
    }
}
