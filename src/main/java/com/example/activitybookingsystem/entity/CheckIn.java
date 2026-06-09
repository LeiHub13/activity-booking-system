package com.example.activitybookingsystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("check_in")
public class CheckIn {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long activityId;
    private Long registrationId;
    private String imageUrl;
    private String objectName;
    private LocalDateTime checkInTime;
    private LocalDateTime createTime;
}
