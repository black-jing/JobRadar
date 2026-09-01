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

public class JobMatcherEvaluationSandbox {

    public static void main(String[] args) {

        ConfigurableApplicationContext context =
                SpringApplication.run(
                        JobRadarApplication.class,
                        args
                );

        JobMatcher jobMatcher =
                context.getBean(JobMatcher.class);
        UserProfile javaAiProfile = new UserProfile(
                "Java后端 + AI应用开发",
                List.of(
                        "Java",
                        "Spring Boot",
                        "MySQL",
                        "大模型API调用"
                ),
                "正在开发JobRadar，使用Spring Boot、MySQL和DeepSeek API完成岗位分析和匹配。"
        );
        Job caseA = new Job(
                "Eval Company A",
                "Java Backend AI Intern",
                "Remote",
                """
                要求：
                Java、Spring Boot、MySQL；
                有大模型API或AI应用项目经验优先。
                """,
                LocalDate.now(),
                "Evaluation",
                "https://example.com/eval/a"
        );
        Job caseB = new Job(
                "Eval Company B",
                "Java Backend Intern",
                "Remote",
                """
                要求：
                Java、Spring Boot、Redis、Kafka。
                """,
                LocalDate.now(),
                "Evaluation",
                "https://example.com/eval/b"
        );
        Job caseC = new Job(
                "Eval Company C",
                "iOS Development Intern",
                "Remote",
                """
                要求：
                Swift、Objective-C、iOS开发经验。
                """,
                LocalDate.now(),
                "Evaluation",
                "https://example.com/eval/c"
        );
        Job caseD = new Job(
                "Eval Company D",
                "Senior Java Architect",
                "Remote",
                """
                要求：
                Java、Spring Boot、MySQL；
                8年以上后端开发经验；
                有系统架构和团队管理经验。
                """,
                LocalDate.now(),
                "Evaluation",
                "https://example.com/eval/d"
        );
        UserProfile limitedProfile = new UserProfile(
                "Java后端",
                List.of(
                        "Java"
                ),
                "暂无详细项目经历"
        );
        Job caseE = new Job(
                "Eval Company E",
                "Java Backend Intern",
                "Remote",
                """
                要求：
                Java、Spring Boot、MySQL。
                """,
                LocalDate.now(),
                "Evaluation",
                "https://example.com/eval/e"
        );
        Job caseF = new Job(
                "Eval Company F",
                "Backend Engineer Intern",
                "Remote",
                """
                技术栈：
                Go、Docker、Kubernetes；
                主要负责Go后端服务开发。
                """,
                LocalDate.now(),
                "Evaluation",
                "https://example.com/eval/f"
        );
        Job caseG = new Job(
                "Eval Company G",
                "Java Backend Intern",
                "Remote",
                """
                要求：
                Java；
                有关系型数据库使用经验；
                有REST API开发经验。
                """,
                LocalDate.now(),
                "Evaluation",
                "https://example.com/eval/g"
        );
        runCase(
                "Case A - 强匹配",
                "应该属于高匹配，并明显高于Case B和Case C",
                jobMatcher,
                caseA,
                javaAiProfile
        );

        runCase(
                "Case B - 部分匹配",
                "应该低于A，高于C；识别Redis和Kafka缺口",
                jobMatcher,
                caseB,
                javaAiProfile
        );

        runCase(
                "Case C - 明显不匹配",
                "应该明显低于A和B，不能错误进入高匹配",
                jobMatcher,
                caseC,
                javaAiProfile
        );

        runCase(
                "Case D - 经验要求过高",
                "技能虽匹配，也应因经验和架构要求明显降分",
                jobMatcher,
                caseD,
                javaAiProfile
        );

        runCase(
                "Case E - 信息不足",
                "谨慎处理未知信息，不得编造用户能力",
                jobMatcher,
                caseE,
                limitedProfile
        );

        runCase(
                "Case F - Backend标题诱导",
                "不能只因Backend高估，应识别Go技术栈差距",
                jobMatcher,
                caseF,
                javaAiProfile
        );

        runCase(
                "Case G - 语义匹配",
                "应识别MySQL/关系型数据库和Spring Boot/REST API的合理关联",
                jobMatcher,
                caseG,
                javaAiProfile
        );

        context.close();
        context.close();
    }
    private static void runCase(
            String caseName,
            String expectation,
            JobMatcher jobMatcher,
            Job job,
            UserProfile userProfile) {

        System.out.println();
        System.out.println("================================");
        System.out.println(caseName);
        System.out.println("预期趋势：" + expectation);
        System.out.println("岗位：" + job.getTitle());
        System.out.println("用户：" + userProfile.getTargetDirection());

        try {

            JobMatchResult result =
                    jobMatcher.match(job, userProfile);

            System.out.println("------ 实际结果 ------");
            System.out.println("score：" + result.getScore());
            System.out.println(
                    "matchedSkills：" + result.getMatchedSkills()
            );
            System.out.println(
                    "gaps：" + result.getGaps()
            );
            System.out.println(
                    "reason：" + result.getReason()
            );
            System.out.println(
                    "suggestion：" + result.getSuggestion()
            );

        } catch (Exception e) {

            System.out.println(
                    "本Case调用失败：" + e.getMessage()
            );
        }
    }
}