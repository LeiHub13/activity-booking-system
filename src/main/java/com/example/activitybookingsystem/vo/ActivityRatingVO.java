package com.example.activitybookingsystem.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ActivityRatingVO {
    private Long id;
    private Long activityId;
    private String activityTitle;
    private Integer rating;
    private String content;
    private LocalDateTime createTime;
}