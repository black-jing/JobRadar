package com.jobradar.sandbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jobradar.analysis.JobAnalyzer;
import com.jobradar.analysis.deepseek.DeepSeekJobAnalyzer;
import com.jobradar.analysis.mock.MockJobAnalyzer;
import com.jobradar.domain.Job;
import com.jobradar.domain.JobAnalysis;

import java.time.LocalDate;

public class AnalyzerSandbox {

    public static void main(String[] args) throws JsonProcessingException {

        Job job = new Job(
                "示例公司",
                "Software Engineer",
                "Remote",
                "We are looking for a Java backend engineer with Spring Boot and MySQL experience.",
                LocalDate.now(),
                "Mock",
                "https://example.com/job/1"
        );

        JobAnalyzer analyzer =
                new DeepSeekJobAnalyzer();

        JobAnalysis analysis =
                analyzer.analyze(job);

        System.out.println("真实AI分析结果：");
        System.out.println(analysis);
    }
}