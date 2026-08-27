package com.jobradar.dto;

import com.jobradar.domain.UserProfile;

import java.util.List;

public class RecommendJobsRequest {

    private List<Long> jobIds;
    private UserProfile userProfile;
    private int topN;

    public List<Long> getJobIds() {
        return jobIds;
    }

    public void setJobIds(List<Long> jobIds) {
        this.jobIds = jobIds;
    }

    public UserProfile getUserProfile() {
        return userProfile;
    }

    public void setUserProfile(UserProfile userProfile) {
        this.userProfile = userProfile;
    }

    public int getTopN() {
        return topN;
    }

    public void setTopN(int topN) {
        this.topN = topN;
    }
}