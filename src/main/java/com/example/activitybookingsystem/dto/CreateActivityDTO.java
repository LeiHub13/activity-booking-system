package com.example.activitybookingsystem.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateActivityDTO {

    @NotBlank(message = "活动标题不能为空")
    @Size(max = 100, message = "活动标题不能超过100个字符")
    private String title;

    private String content;

    @Size(max = 200, message = "活动地点不能超过200个字符")
    private String location;

    @NotNull(message = "活动开始时间不能为空")
    @Future(message = "活动开始时间必须晚于当前时间")
    private LocalDateTime startTime;

    @NotNull(message = "活动结束时间不能为空")
    private LocalDateTime endTime;

    @NotNull(message = "报名开始时间不能为空")
    private LocalDateTime signupStartTime;

    @NotNull(message = "报名结束时间不能为空")
    private LocalDateTime signupEndTime;

    @NotNull(message = "活动人数上限不能为空")
    @Min(value = 1, message = "活动人数上限必须大于0")
    private Integer maxCount;
}
