package com.jobradar.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.Map;

@Configuration
public class JobAnalysisRabbitConfig {
    public static final String EXCHANGE = "job.exchange";
    public static final String ROUTING_KEY = "job.analysis";
    public static final String QUEUE = "job.analysis.queue";
    public static final String DEAD_LETTER_EXCHANGE = "job.dlx";
    public static final String DEAD_LETTER_ROUTING_KEY = "job.analysis.dead";
    public static final String DEAD_LETTER_QUEUE = "job.analysis.dlq";

    @Bean
    DirectExchange jobExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    DirectExchange jobDeadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    Queue jobAnalysisQueue() {
        return QueueBuilder.durable(QUEUE)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(DEAD_LETTER_ROUTING_KEY)
                .build();
    }

    @Bean
    Queue jobAnalysisDeadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    @Bean
    Binding jobAnalysisBinding(@Qualifier("jobAnalysisQueue") Queue jobAnalysisQueue,
                               @Qualifier("jobExchange") DirectExchange jobExchange) {
        return BindingBuilder.bind(jobAnalysisQueue).to(jobExchange).with(ROUTING_KEY);
    }

    @Bean
    Binding jobAnalysisDeadLetterBinding(
            @Qualifier("jobAnalysisDeadLetterQueue") Queue jobAnalysisDeadLetterQueue,
            @Qualifier("jobDeadLetterExchange") DirectExchange jobDeadLetterExchange) {
        return BindingBuilder.bind(jobAnalysisDeadLetterQueue)
                .to(jobDeadLetterExchange).with(DEAD_LETTER_ROUTING_KEY);
    }

    @Bean
    RetryTemplate jobAnalysisRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        RetryPolicy policy = new SimpleRetryPolicy(3,
                Map.of(RetryableJobAnalysisException.class, true), true, false);
        retryTemplate.setRetryPolicy(policy);
        FixedBackOffPolicy backOff = new FixedBackOffPolicy();
        backOff.setBackOffPeriod(1_000);
        retryTemplate.setBackOffPolicy(backOff);
        return retryTemplate;
    }
}
