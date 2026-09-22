package com.jobradar.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public class RecommendJobsRequest {

    @NotNull
    @Size(min = 3, max = 5)
    private List<Long> jobIds;

    @Min(1)
    @Max(5)
    private int topN;

    public List<Long> getJobIds() {
        return jobIds;
    }

    public void setJobIds(
            List<Long> jobIds) {

        this.jobIds = jobIds;
    }

    public int getTopN() {
        return topN;
    }

    public void setTopN(
            int topN) {

        this.topN = topN;
    }
}