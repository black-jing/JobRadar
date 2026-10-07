package com.jobradar.messaging;

import com.jobradar.repository.JobAnalysisSubmissionRepository;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class JobAnalysisProducer {
    private final RabbitTemplate rabbitTemplate;
    private final JobAnalysisSubmissionRepository submissionRepository;

    public JobAnalysisProducer(RabbitTemplate rabbitTemplate,
                               JobAnalysisSubmissionRepository submissionRepository) {
        this.rabbitTemplate = rabbitTemplate;
        this.submissionRepository = submissionRepository;
    }

    public void submit(Long jobId) {
        CorrelationData correlation = new CorrelationData(String.valueOf(jobId));
        rabbitTemplate.convertAndSend(JobAnalysisRabbitConfig.EXCHANGE,
                JobAnalysisRabbitConfig.ROUTING_KEY, String.valueOf(jobId), correlation);
        try {
            CorrelationData.Confirm confirm = correlation.getFuture().get(5, TimeUnit.SECONDS);
            if (!confirm.isAck() || correlation.getReturned() != null) {
                throw new IllegalStateException("岗位分析消息未被 RabbitMQ 接收或路由，jobId=" + jobId);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("等待 RabbitMQ 发布确认被中断，jobId=" + jobId, e);
        } catch (java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException e) {
            throw new IllegalStateException("RabbitMQ 发布确认失败，jobId=" + jobId, e);
        }
        if (submissionRepository.markSubmitted(jobId) != 1) {
            throw new IllegalStateException("岗位分析提交状态不存在，jobId=" + jobId);
        }
    }
}
