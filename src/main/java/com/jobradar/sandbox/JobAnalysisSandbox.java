package com.jobradar.sandbox;

import com.jobradar.domain.JobAnalysis;

import java.util.List;

public class JobAnalysisSandbox {

    public static void main(String[] args) {

        JobAnalysis analysis = new JobAnalysis(
                "Java后端",
                List.of("Java", "Spring Boot", "MySQL", "Redis"),
                "这是一个偏Java后端开发的岗位，主要负责后端服务和接口开发。"
        );

        System.out.println(analysis);
    }
}