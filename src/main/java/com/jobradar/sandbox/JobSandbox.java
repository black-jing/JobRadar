package com.jobradar.sandbox;

import com.jobradar.domain.Job;
import java.util.List;
import java.util.ArrayList;
import java.time.LocalDate;

public class JobSandbox {
    public static void main(String[] args) {
        Job job1 = new Job("字节跳动",
                "Java后端开发实习生",
                "北京",
                "负责后端服务开发，Java基础扎实者优先",
                LocalDate.of(2026, 8, 12),
                "字节跳动招聘官网",
                "https://example.com/job/1");
        Job job2 = new Job("腾讯",
                "后台开发实习生",
                "深圳",
                "参与业务后台服务开发",
                LocalDate.of(2026, 8, 11),
                "腾讯招聘官网",
                "https://example.com/job/2");
        Job job3= new Job("阿里巴巴",
                "AI应用开发实习生",
                "杭州",
                "参与大模型应用平台开发",
                LocalDate.of(2026, 8, 10),
                "阿里巴巴招聘官网",
                "https://example.com/job/3");
        List<Job> jobs = new ArrayList<>();
        jobs.add(job1);
        jobs.add(job2);
        jobs.add(job3);
        System.out.println(jobs.size());
        for (Job job : jobs)
        {
            System.out.println(job);
        }
    }
}
