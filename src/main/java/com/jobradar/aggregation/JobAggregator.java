package com.jobradar.aggregation;

import com.jobradar.domain.Job;
import com.jobradar.source.JobSource;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;

public class JobAggregator {

    private List<JobSource> sources;

    public JobAggregator(List<JobSource> sources) {
        this.sources = sources;
    }

    public List<Job> aggregateJobs() {
        List<Job> allJobs = new ArrayList<>();

        for (JobSource source : sources) {
           try {
               List<Job> jobs = source.fetchJobs();
               allJobs.addAll(jobs);
           }catch(Exception e)
           {
               System.out.println(
                       "获取岗位失败："
                               + source.getClass().getSimpleName()
                               + "，原因："
                               + e.getMessage());
           }
        }


        return allJobs;
    }
}
