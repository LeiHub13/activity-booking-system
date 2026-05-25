package com.example.activitybookingsystem.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminRegistrationVO {

    private Long registrationId;
    private Long userId;
    private String username;
    private String realName;
    private String phone;
    private String email;
    private Long activityId;
    private String activityTitle;
    private String activityStatus;
    private String registrationStatus;
    private String remark;
    private Long auditUserId;
    private String auditUsername;
    private LocalDateTime auditTime;
    private LocalDateTime registrationTime;
    private LocalDateTime updateTime;
}
