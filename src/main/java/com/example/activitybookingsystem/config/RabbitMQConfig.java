package com.example.activitybookingsystem.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String ACTIVITY_EVENT_EXCHANGE = "activity.event.exchange";
    public static final String NOTICE_AUDIT_QUEUE = "notice.audit.queue";
    public static final String NOTICE_AUDIT_ROUTING_KEY = "notice.audit";

    @Bean
    public DirectExchange activityEventExchange() {
        return new DirectExchange(ACTIVITY_EVENT_EXCHANGE, true, false);
    }

    @Bean
    public Queue noticeAuditQueue() {
        return QueueBuilder.durable(NOTICE_AUDIT_QUEUE).build();
    }

    @Bean
    public Binding noticeAuditBinding(Queue noticeAuditQueue, DirectExchange activityEventExchange) {
        return BindingBuilder.bind(noticeAuditQueue)
                .to(activityEventExchange)
                .with(NOTICE_AUDIT_ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
