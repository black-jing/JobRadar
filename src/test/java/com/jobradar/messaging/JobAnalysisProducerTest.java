package com.jobradar.messaging;

import com.jobradar.repository.JobAnalysisSubmissionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class JobAnalysisProducerTest {
    private final RabbitTemplate rabbit = mock(RabbitTemplate.class);
    private final JobAnalysisSubmissionRepository submissions =
            mock(JobAnalysisSubmissionRepository.class);
    private final JobAnalysisProducer producer = new JobAnalysisProducer(rabbit, submissions);

    @Test
    void confirmedMessageClearsPendingSubmission() {
        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(3);
            correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbit).convertAndSend(eq(JobAnalysisRabbitConfig.EXCHANGE),
                eq(JobAnalysisRabbitConfig.ROUTING_KEY), eq("7"), any(CorrelationData.class));
        when(submissions.markSubmitted(7L)).thenReturn(1);

        producer.submit(7L);

        verify(rabbit).convertAndSend(eq(JobAnalysisRabbitConfig.EXCHANGE),
                eq(JobAnalysisRabbitConfig.ROUTING_KEY), eq("7"), any(CorrelationData.class));
        verify(submissions).markSubmitted(7L);
    }

    @Test
    void rejectedMessageKeepsPendingSubmission() {
        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(3);
            correlation.getFuture().complete(new CorrelationData.Confirm(false, "broker rejected"));
            return null;
        }).when(rabbit).convertAndSend(eq(JobAnalysisRabbitConfig.EXCHANGE),
                eq(JobAnalysisRabbitConfig.ROUTING_KEY), eq("7"), any(CorrelationData.class));

        assertThrows(IllegalStateException.class, () -> producer.submit(7L));
        verify(submissions, never()).markSubmitted(7L);
    }
}
