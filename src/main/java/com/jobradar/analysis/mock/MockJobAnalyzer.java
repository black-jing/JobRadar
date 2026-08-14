package com.jobradar.analysis.mock;

import com.jobradar.analysis.JobAnalyzer;
import com.jobradar.domain.Job;
import com.jobradar.domain.JobAnalysis;

import java.util.List;

public class MockJobAnalyzer implements JobAnalyzer {
    @Override
    public JobAnalysis analyze(Job job) {

        return new JobAnalysis(
                "Java后端",
                List.of("Java", "Spring Boot", "MySQL"),
                "这是一个Java后端岗位，主要负责后端服务开发。"
        );
    }

}