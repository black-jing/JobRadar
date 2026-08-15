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

        System.out.println("=== 岗位基本信息 ===");
        System.out.println("公司：" + job.getCompany());
        System.out.println("岗位：" + job.getTitle());
        System.out.println("地点：" + job.getLocation());
        System.out.println("来源：" + job.getSource());

        JobAnalyzer analyzer = new DeepSeekJobAnalyzer();

        try {

            JobAnalysis analysis = analyzer.analyze(job);

            System.out.println("=== AI分析结果 ===");
            System.out.println("direction：" + analysis.getDirection());
            System.out.println("skills：" + analysis.getSkills());
            System.out.println("summary：" + analysis.getSummary());

        } catch (Exception e) {

            System.out.println(
                    "AI岗位分析失败：" + e.getMessage()
            );
        }
    }
}