package com.jobradar.analysis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jobradar.domain.Job;
import com.jobradar.domain.JobAnalysis;

public interface JobAnalyzer {

    JobAnalysis analyze(Job job) throws JsonProcessingException;
}