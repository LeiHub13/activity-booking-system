package com.example.activitybookingsystem.vo;

import lombok.Data;

@Data
public class ActivityRegistrationStatsVO {

    private Long activityId;
    private String activityTitle;
    private String activityStatus;
    private Integer maxCount;
    private Integer currentCount;
    private Long totalCount;
    private Long pendingCount;
    private Long approvedCount;
    private Long rejectedCount;
    private Long canceledCount;
}
