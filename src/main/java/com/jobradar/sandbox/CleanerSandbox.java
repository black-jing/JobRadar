package com.jobradar.sandbox;

import com.jobradar.cleaning.JobCleaner;
import com.jobradar.domain.Job;

import java.time.LocalDate;

public class CleanerSandbox {

    public static void main(String[] args) {
        Job dirtyJob = new Job(
                "   字节跳动   ",
                "   Java后端开发实习生   ",
                "   北京   ",
                "参与后端开发",
                LocalDate.of(2026, 8, 12),
                "Mock",
                "https://example.com/dirty"
        );
        Job invalidJob = new Job(
                "   adsfsdgn",
                "",
                "上海",
                "...",
                LocalDate.of(2026, 8, 12),
                "Mock",
                "..."
        );
        JobCleaner cleaner = new JobCleaner();
        Job cleanedJob = cleaner.clean(dirtyJob);
        Job result = cleaner.clean(invalidJob);
        System.out.println(result);
    }
}