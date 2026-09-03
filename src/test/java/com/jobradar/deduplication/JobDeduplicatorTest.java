package com.jobradar.deduplication;

import com.jobradar.domain.Job;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JobDeduplicatorTest {

    @Test
    void duplicateJobsShouldBeRemoved() {

        // Given
        Job job1 = new Job(
                "字节跳动",
                "Java后端实习生",
                "北京",
                "负责Java后端开发",
                LocalDate.of(2026, 9, 1),
                "SourceA",
                "https://example.com/job/1"
        );

        Job job2 = new Job(
                "字节跳动",
                "Java后端实习生",
                "北京",
                "这是另一份岗位描述",
                LocalDate.of(2026, 9, 2),
                "SourceB",
                "https://example.com/job/2"
        );

        List<Job> jobs = List.of(job1, job2);

        JobDeduplicator deduplicator =
                new JobDeduplicator();

        // When
        List<Job> result =
                deduplicator.deduplicate(jobs);

        // Then
        assertEquals(1, result.size());
    }
}