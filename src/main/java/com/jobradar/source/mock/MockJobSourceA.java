package com.jobradar.source.mock;

import com.jobradar.domain.Job;
import com.jobradar.source.JobSource;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MockJobSourceA implements JobSource {

    @Override
    public List<Job> fetchJobs() {
        System.out.println("SourceA：开始获取岗位");
        List<Job> jobs = new ArrayList<>();
        Job job1 = new Job(
                "字节跳动",
                "Java后端开发实习生",
                "北京",
                "参与后端服务开发",
                LocalDate.of(2026, 8, 12),
                "MockSourceA",
                "https://example.com/a/1"
        );
        Job job2 = new Job(
                "   腾讯   ",
                "    Java开发实习生   ",
                "   深圳   ",
                "参与后台服务开发",
                LocalDate.of(2026, 8, 11),
                "MockSourceA",
                "https://example.com/a/2"
        );
        Job job3 = new Job(
                "    ",
                "    Java开发实习生   ",
                "   深圳   ",
                "参与后台服务开发",
                LocalDate.of(2026, 8, 11),
                "MockSourceA",
                "https://example.com/a/2"
        );
        jobs.add(job1);
        jobs.add(job2);
        jobs.add(job3);
        System.out.println("SourceA：获取成功");
        return jobs;

    }
}