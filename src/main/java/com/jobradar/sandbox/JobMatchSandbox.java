package com.jobradar.sandbox;

import com.jobradar.JobRadarApplication;
import com.jobradar.domain.Job;
import com.jobradar.domain.JobMatchResult;
import com.jobradar.domain.UserProfile;
import com.jobradar.matching.JobMatcher;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.time.LocalDate;
import java.util.List;

public class JobMatchSandbox {

    public static void main(String[] args) {

        ConfigurableApplicationContext context =
                SpringApplication.run(
                        JobRadarApplication.class,
                        args
                );

        JobMatcher jobMatcher =
                context.getBean(JobMatcher.class);

        Job job = new Job(
                "测试公司",
                "Java AI应用开发实习生",
                "Remote",
                """
                岗位要求：
                1. 熟悉Java基础；
                2. 了解Spring Boot；
                3. 熟悉MySQL；
                4. 有Redis使用经验；
                5. 有大模型API或AI应用开发经验优先。
                """,
                LocalDate.now(),
                "Test",
                "https://example.com/job"
        );

        UserProfile userProfile = new UserProfile(
                "Java后端 + AI应用开发",
                List.of(
                        "Java",
                        "Spring Boot",
                        "MySQL",
                        "大模型API调用"
                ),
                "正在开发JobRadar项目，使用Spring Boot、MySQL和DeepSeek API完成岗位分析与岗位匹配。"
        );

        JobMatchResult result =
                jobMatcher.match(job, userProfile);

        System.out.println("====== 岗位匹配结果 ======");

        System.out.println(
                "匹配分数：" + result.getScore()
        );

        System.out.println(
                "已匹配能力：" + result.getMatchedSkills()
        );

        System.out.println(
                "主要缺口：" + result.getGaps()
        );

        System.out.println(
                "原因：" + result.getReason()
        );

        System.out.println(
                "建议：" + result.getSuggestion()
        );

        context.close();
    }
}