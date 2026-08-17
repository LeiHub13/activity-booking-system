package com.example.activitybookingsystem.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PointProductVO {
    private Long id;
    private String name;
    private String description;
    private Integer pointsRequired;
    private Integer stock;
    private Integer status;
    private LocalDateTime createTime;
}