package com.example.activitybookingsystem.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MyRegistrationVO {

    private Long registrationId;
    private Long activityId;
    private String registrationStatus;
    private String remark;
    private LocalDateTime registrationTime;
    private String activityTitle;
    private String activityContent;
    private String activityLocation;
    private LocalDateTime activityStartTime;
    private LocalDateTime activityEndTime;
    private LocalDateTime signupStartTime;
    private LocalDateTime signupEndTime;
    private Integer maxCount;
    private Integer currentCount;
    private String activityStatus;
}
