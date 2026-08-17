package com.example.activitybookingsystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("point_exchange")
public class PointExchange {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long productId;
    private String productName;
    private Integer pointsCost;
    private LocalDateTime createTime;
}