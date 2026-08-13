package com.jobradar.source.mock;



import com.jobradar.domain.Job;
import com.jobradar.source.JobSource;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MockJobSourceC implements JobSource {

    @Override
    public List<Job> fetchJobs() {
        System.out.println("SourceC：开始获取岗位");
        List<Job> jobs = new ArrayList<>();

        Job job1 = new Job(
                "字节跳动",
                "Java后端开发实习生",
                "北京",
                "参与Java后端业务开发",
                LocalDate.of(2026, 8, 12),
                "SourceC",
                "https://example.com/c/1"
        );
        Job job2 = new Job(
                "美团",
                "后端开发实习生",
                "北京",
                "参与Java后端业务开发",
                LocalDate.of(2026, 8, 12),
                "SourceC",
                "https://example.com/c/1"
        );

        jobs.add(job1);
        System.out.println("SourceC：获取成功");
        return jobs;
    }
}