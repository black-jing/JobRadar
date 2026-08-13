package com.jobradar.sandbox;

import com.jobradar.domain.Job;
import com.jobradar.source.RemotiveJobSource;

import java.util.List;

public class RemotiveSourceTest {

    public static void main(String[] args) {

        RemotiveJobSource source = new RemotiveJobSource();

        List<Job> jobs = source.fetchJobs();

        System.out.println("测试类收到岗位数量：" + jobs.size());

        if (!jobs.isEmpty()) {
            System.out.println("测试类收到第一条：" + jobs.get(0));
        }
    }
}