package com.example.activitybookingsystem.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginVO {
    // 兼容旧前端字段，值与 accessToken 相同。
    private String token;
    private String accessToken;
    private String refreshToken;
    private Long accessTokenExpireSeconds;
    private Long refreshTokenExpireSeconds;

    public static LoginVO of(String accessToken,
                             String refreshToken,
                             Long accessTokenExpireSeconds,
                             Long refreshTokenExpireSeconds) {
        return new LoginVO(
                accessToken,
                accessToken,
                refreshToken,
                accessTokenExpireSeconds,
                refreshTokenExpireSeconds
        );
    }
}
