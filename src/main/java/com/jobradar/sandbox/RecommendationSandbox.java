package com.jobradar.sandbox;

import com.jobradar.JobRadarApplication;
import com.jobradar.domain.JobRecommendation;
import com.jobradar.domain.UserProfile;
import com.jobradar.service.JobService;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.List;

public class RecommendationSandbox {

    public static void main(String[] args) {

        ConfigurableApplicationContext context =
                SpringApplication.run(
                        JobRadarApplication.class,
                        args
                );

        JobService jobService =
                context.getBean(JobService.class);

        UserProfile userProfile = new UserProfile(
                "Java后端 + AI应用开发",
                List.of(
                        "Java",
                        "Spring Boot",
                        "MySQL",
                        "大模型API调用"
                ),
                "正在开发JobRadar，使用Spring Boot、MySQL和DeepSeek API完成岗位分析和匹配。"
        );

        List<JobRecommendation> recommendations =
                jobService.recommendJobs(
                        List.of(1L, 2L, 3L),
                        userProfile,
                        3
                );

        int rank = 1;

        for (JobRecommendation recommendation : recommendations) {

            System.out.println(
                    "第" + rank + "名："
                            + recommendation.getCompany()
                            + " - "
                            + recommendation.getTitle()
                            + "，score="
                            + recommendation
                            .getMatchResult()
                            .getScore()
            );

            rank++;
        }

        context.close();
    }
}