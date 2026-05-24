package com.example.activitybookingsystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("activity")
public class Activity {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String title;
    private String content;
    private String location;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime signupStartTime;
    private LocalDateTime signupEndTime;
    private Integer maxCount;
    private Integer currentCount;
    private String status;
    private Long publisherId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
