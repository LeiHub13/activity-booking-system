package com.example.activitybookingsystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshTokenDTO {

    @NotBlank(message = "刷新 token 不能为空")
    private String refreshToken;
}
