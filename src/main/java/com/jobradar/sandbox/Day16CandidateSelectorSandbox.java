package com.jobradar.sandbox;

import com.jobradar.domain.Job;
import com.jobradar.domain.UserProfile;
import com.jobradar.selection.JobCandidateSelector;

import java.util.ArrayList;
import java.util.List;

public class Day16CandidateSelectorSandbox {

    public static void main(String[] args) {

        List<Job> jobs = new ArrayList<>();

        jobs.add(
                new Job(
                        "Company A",
                        "Java Backend Intern",
                        "Remote",
                        "Use Java, Spring Boot and MySQL to build backend services.",
                        null,
                        "Test",
                        "https://example.com/a"
                )
        );

        jobs.add(
                new Job(
                        "Company B",
                        "AI Application Intern",
                        "Remote",
                        "Build LLM applications and integrate LLM API with Java services.",
                        null,
                        "Test",
                        "https://example.com/b"
                )
        );

        jobs.add(
                new Job(
                        "Company C",
                        "Frontend Intern",
                        "Remote",
                        "React, TypeScript and CSS development.",
                        null,
                        "Test",
                        "https://example.com/c"
                )
        );

        jobs.add(
                new Job(
                        "Company D",
                        "Product Manager",
                        "Remote",
                        "Responsible for product strategy and business growth.",
                        null,
                        "Test",
                        "https://example.com/d"
                )
        );

        jobs.add(
                new Job(
                        "Company E",
                        "Backend Software Intern",
                        "Remote",
                        "Backend development experience and MySQL knowledge preferred.",
                        null,
                        "Test",
                        "https://example.com/e"
                )
        );

        UserProfile userProfile =
                new UserProfile(
                        "Java Backend",
                        List.of(
                                "Java",
                                "Spring Boot",
                                "MySQL",
                                "LLM API"
                        ),
                        "Built JobRadar with Spring Boot, MySQL and LLM APIs."
                );

        JobCandidateSelector selector =
                new JobCandidateSelector();

        List<Job> candidates =
                selector.selectCandidates(
                        jobs,
                        userProfile,
                        3
                );

        System.out.println(
                "原始岗位数量："
                        + jobs.size()
        );

        System.out.println(
                "候选岗位数量："
                        + candidates.size()
        );

        for (Job job : candidates) {
            System.out.println(
                    job.getTitle()
            );
        }
    }
}