package com.jobradar.matching;

import com.jobradar.domain.Job;
import com.jobradar.domain.JobMatchResult;
import com.jobradar.domain.UserProfile;

public interface JobMatcher {

    JobMatchResult match(Job job, UserProfile userProfile);
}