package com.jobradar.source.mock;

import com.jobradar.domain.Job;
import com.jobradar.source.JobSource;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MockJobSourceB implements JobSource {

    @Override

    public List<Job> fetchJobs() {
        System.out.println("SourceB：开始获取岗位");
        throw new RuntimeException("SourceB 获取岗位失败");
    }
   /* public List<Job> fetchJobs() {

/*List<Job>jobs=new ArrayList<>();
        //下面你自己完成
        /*Job job1=new Job(
                "字节跳动",
                "Java后端开发实习生",
                "北京",
                "参与大模型应用平台开发",
                LocalDate.of(2026, 8, 12),
                "MockSourceB",
                "https://example.com/b/1"
        );
        Job job2=new Job(
                "美团",
                "Java研发实习生",
                "北京",
                "参与核心业务后端开发",
                LocalDate.of(2026, 8, 10),
                "MockSourceB",
                "https://example.com/b/2"
        );
jobs.add(job1);
        jobs.add(job2);
        return jobs;*/
    }
