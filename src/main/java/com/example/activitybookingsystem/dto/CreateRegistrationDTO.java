package com.example.activitybookingsystem.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateRegistrationDTO {

    @NotNull(message = "活动ID不能为空")
    private Long activityId;

    @Size(max = 255, message = "报名备注不能超过255个字符")
    private String remark;
}
