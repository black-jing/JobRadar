package com.jobradar.sandbox;

import com.jobradar.JobRadarApplication;
import com.jobradar.domain.JobRecommendation;
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

        List<JobRecommendation> recommendations =
                jobService.recommendJobs(
                        List.of(1L, 2L, 3L),
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
