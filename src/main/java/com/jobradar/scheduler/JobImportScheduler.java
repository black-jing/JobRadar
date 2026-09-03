package com.jobradar.scheduler;

import com.jobradar.service.JobService;
import org.springframework.stereotype.Component;
import com.jobradar.domain.Job;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;
@Component
public class JobImportScheduler {

    private final JobService jobService;

    public JobImportScheduler(JobService jobService) {
        this.jobService = jobService;
    }
    @Scheduled(
            initialDelay = 10_000,
            fixedDelay = 21_600_000
    )
    public void importJobs() {

        System.out.println(
                "===== 定时岗位同步开始 ====="
        );

        try {

            List<Job> savedJobs =
                    jobService.importAllRealJobs();

            System.out.println(
                    "定时岗位同步完成，本次新增岗位数量："
                            + savedJobs.size()
            );

        } catch (RuntimeException e) {

            System.out.println(
                    "定时岗位同步失败，原因："
                            + e.getMessage()
            );
        }

        System.out.println(
                "===== 定时岗位同步结束 ====="
        );
    }
}