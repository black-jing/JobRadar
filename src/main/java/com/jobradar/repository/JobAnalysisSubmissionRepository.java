package com.jobradar.repository;

import com.jobradar.domain.JobAnalysisSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface JobAnalysisSubmissionRepository extends JpaRepository<JobAnalysisSubmission, Long> {
    List<JobAnalysisSubmission> findByPendingTrue();

    @Modifying
    @Transactional
    @Query("UPDATE JobAnalysisSubmission s SET s.pending = false WHERE s.jobId = :jobId")
    int markSubmitted(@Param("jobId") Long jobId);
}
