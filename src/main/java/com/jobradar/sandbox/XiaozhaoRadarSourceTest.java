package com.jobradar.sandbox;

import com.jobradar.domain.Job;
import com.jobradar.source.JobSource;
import com.jobradar.source.XiaozhaoRadarJobSource;

import java.util.List;

public class XiaozhaoRadarSourceTest {

    public static void main(String[] args) {

        JobSource source =
                new XiaozhaoRadarJobSource();

        List<Job> jobs =
                source.fetchJobs();

        System.out.println(
                "测试获得岗位数量：" + jobs.size()
        );

        for (int i = 0;
             i < Math.min(5, jobs.size());
             i++) {

            System.out.println(jobs.get(i));
        }
    }
}