package com.example.activitybookingsystem.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CheckInVO {
    private Long id;
    private Long userId;
    private Long activityId;
    private Long registrationId;
    private String imageUrl;
    private LocalDateTime checkInTime;
}
