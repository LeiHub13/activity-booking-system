package com.example.activitybookingsystem.mq;

import com.example.activitybookingsystem.config.RabbitMQConfig;
import com.example.activitybookingsystem.entity.Activity;
import com.example.activitybookingsystem.entity.Registration;
import com.example.activitybookingsystem.mapper.ActivityMapper;
import com.example.activitybookingsystem.mapper.RegistrationMapper;
import com.example.activitybookingsystem.message.AuditNoticeMessage;
import com.example.activitybookingsystem.service.NoticeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AuditNoticeConsumer {

    private final RegistrationMapper registrationMapper;
    private final ActivityMapper activityMapper;
    private final NoticeService noticeService;

    public AuditNoticeConsumer(RegistrationMapper registrationMapper,
                               ActivityMapper activityMapper,
                               NoticeService noticeService) {
        this.registrationMapper = registrationMapper;
        this.activityMapper = activityMapper;
        this.noticeService = noticeService;
    }

    @RabbitListener(queues = RabbitMQConfig.NOTICE_AUDIT_QUEUE)
    public void handle(AuditNoticeMessage message) {
        Registration registration = registrationMapper.selectById(message.getRegistrationId());
        Activity activity = activityMapper.selectById(message.getActivityId());
        noticeService.createRegistrationAuditNotice(
                registration,
                activity,
                Boolean.TRUE.equals(message.getApproved())
        );
        log.info("审核通知消息消费成功，registrationId={}", message.getRegistrationId());
    }
}
