package com.jobradar.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobradar.analysis.JobAnalyzer;
import com.jobradar.domain.ApplicationStatus;
import com.jobradar.domain.Job;
import com.jobradar.domain.JobApplication;
import com.jobradar.matching.JobMatcher;
import com.jobradar.repository.JobApplicationRepository;
import com.jobradar.repository.JobRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock
    private JobAnalyzer jobAnalyzer;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JobApplicationRepository jobApplicationRepository;

    @Mock
    private JobMatcher jobMatcher;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private JobService jobService;

    @Test
    void createApplicationShouldSucceedWhenJobExists() {

        // Given
        Long jobId = 1L;

        Job job = new Job(
                "字节跳动",
                "Java后端实习生",
                "北京",
                "负责Java后端开发",
                LocalDate.of(2026, 9, 1),
                "Remotive",
                "https://example.com/job/1"
        );

        when(jobRepository.findById(jobId))
                .thenReturn(Optional.of(job));

        when(jobApplicationRepository.findByJob_Id(jobId))
                .thenReturn(Optional.empty());

        when(jobApplicationRepository.save(
                org.mockito.ArgumentMatchers.any(JobApplication.class)
        )).thenAnswer(invocation ->
                invocation.getArgument(0)
        );

        // When
        JobApplication result =
                jobService.createApplication(
                        jobId,
                        ApplicationStatus.SAVED
                );

        // Then
        assertEquals(
                ApplicationStatus.SAVED,
                result.getStatus()
        );

        assertEquals(
                job,
                result.getJob()
        );

        verify(jobRepository)
                .findById(jobId);

        verify(jobApplicationRepository)
                .save(
                        org.mockito.ArgumentMatchers.any(
                                JobApplication.class
                        )
                );
    }
    @Test
    void createApplicationShouldFailWhenJobDoesNotExist() {

        // Given
        Long jobId = 1L;

        when(jobRepository.findById(jobId))
                .thenReturn(Optional.empty());

        // When + Then
        assertThrows(
                NoSuchElementException.class,
                () -> jobService.createApplication(
                        jobId,
                        ApplicationStatus.SAVED
                )
        );
    }
    @Test
    void updateApplicationStatusShouldSaveWhenTransitionIsValid() {

        // Given
        Long jobId = 1L;

        Job job = new Job(
                "字节跳动",
                "Java后端实习生",
                "北京",
                "负责Java后端开发",
                LocalDate.of(2026, 9, 1),
                "Remotive",
                "https://example.com/job/1"
        );

        JobApplication application =
                new JobApplication(
                        job,
                        ApplicationStatus.SAVED
                );

        when(jobApplicationRepository.findByJob_Id(jobId))
                .thenReturn(Optional.of(application));

        when(jobApplicationRepository.save(application))
                .thenReturn(application);

        // When
        JobApplication result =
                jobService.updateApplicationStatus(
                        jobId,
                        ApplicationStatus.APPLIED
                );

        // Then
        assertEquals(
                ApplicationStatus.APPLIED,
                result.getStatus()
        );

        verify(jobApplicationRepository)
                .save(application);
    }
}