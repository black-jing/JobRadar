package com.jobradar.sandbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jobradar.analysis.JobAnalyzer;
import com.jobradar.analysis.deepseek.DeepSeekJobAnalyzer;
import com.jobradar.domain.Job;
import com.jobradar.domain.JobAnalysis;
import com.jobradar.source.RemotiveJobSource;

import java.util.List;

public class RealJobAnalysisSandbox {

    public static void main(String[] args) throws JsonProcessingException {

        RemotiveJobSource source = new RemotiveJobSource();

        List<Job> jobs = source.fetchJobs();

        if (jobs.isEmpty()) {
            System.out.println("没有获取到真实岗位");
            return;
        }

        Job job = jobs.get(0);

        System.out.println("准备分析岗位：");
        System.out.println(job.getTitle());
        System.out.println(job.getCompany());

        JobAnalyzer analyzer = new DeepSeekJobAnalyzer();

        JobAnalysis analysis = analyzer.analyze(job);

        System.out.println("=== AI分析结果 ===");
        System.out.println(analysis);
    }
}