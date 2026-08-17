package com.example.activitybookingsystem.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PointExchangeVO {
    private Long id;
    private Long userId;
    private String username;
    private Long productId;
    private String productName;
    private Integer pointsCost;
    private LocalDateTime createTime;
}