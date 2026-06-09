package com.example.activitybookingsystem.mq;

import com.example.activitybookingsystem.config.RabbitMQConfig;
import com.example.activitybookingsystem.message.AuditNoticeMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class AuditNoticeProducer {

    private final RabbitTemplate rabbitTemplate;

    public AuditNoticeProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void send(AuditNoticeMessage message) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ACTIVITY_EVENT_EXCHANGE,
                RabbitMQConfig.NOTICE_AUDIT_ROUTING_KEY,
                message
        );
    }
}
