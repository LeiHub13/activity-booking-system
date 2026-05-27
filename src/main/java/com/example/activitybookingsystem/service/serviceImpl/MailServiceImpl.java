package com.example.activitybookingsystem.service.serviceImpl;

import com.example.activitybookingsystem.common.exception.BusinessException;
import com.example.activitybookingsystem.dto.ResetPasswordDTO;
import com.example.activitybookingsystem.dto.SendPasswordResetCodeDTO;
import com.example.activitybookingsystem.mapper.MailMapper;
import com.example.activitybookingsystem.service.MailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class MailServiceImpl implements MailService {
    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;
    @Autowired
    private MailMapper mailMapper;
    @Override
    public void sendPasswordResetCode(SendPasswordResetCodeDTO dto){
        String code = generateCode();
        String email = dto.getEmail();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("预约报名管理系统密码重置验证码");
        message.setText("你的验证码是：" + code + "，5分钟内有效。请勿泄露给他人。");

        mailSender.send(message);
        stringRedisTemplate.opsForValue().set( "password:reset:code:" + email,
                code,
                5,
                TimeUnit.MINUTES);
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordDTO dto) {
        String email = dto.getEmail();
        String code = dto.getCode();
        String newPassword = dto.getNewPassword();
        if (validateCode(email, code) == 1) {
            mailMapper.updatePassword(email, newPassword);
        }
    }

    private String generateCode() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
    }
    private int validateCode(String email, String code) {
        String redisCode = stringRedisTemplate.opsForValue().get("password:reset:code:" + email);
        log.info("redisCode:{}", redisCode);
        if (redisCode == null || !redisCode.equals(code)) {
            throw new BusinessException("验证码为空或不正确");
        }
        return 1;
    }
}
