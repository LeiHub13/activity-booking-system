package com.example.activitybookingsystem.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PointRecordVO {
    private Long id;
    private Integer points;
    private String type;
    private String remark;
    private LocalDateTime createTime;
}