package com.example.activitybookingsystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("activity_rating")
public class ActivityRating {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long activityId;
    private Integer rating;
    private String content;
    private LocalDateTime createTime;
}