package com.jobradar.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "job_analysis_submission")
public class JobAnalysisSubmission {
    @Id
    private Long jobId;

    private boolean pending;

    protected JobAnalysisSubmission() {
    }

    public JobAnalysisSubmission(Long jobId) {
        this.jobId = jobId;
        this.pending = true;
    }

    public Long getJobId() {
        return jobId;
    }

    public boolean isPending() {
        return pending;
    }
}
