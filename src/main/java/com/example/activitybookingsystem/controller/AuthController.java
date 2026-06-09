package com.example.activitybookingsystem.controller;

import com.example.activitybookingsystem.common.result.Result;
import com.example.activitybookingsystem.dto.LoginDTO;
import com.example.activitybookingsystem.dto.RefreshTokenDTO;
import com.example.activitybookingsystem.dto.RegisterDTO;
import com.example.activitybookingsystem.dto.ResetPasswordDTO;
import com.example.activitybookingsystem.dto.SendPasswordResetCodeDTO;
import com.example.activitybookingsystem.service.MailService;
import com.example.activitybookingsystem.service.UserService;
import com.example.activitybookingsystem.vo.LoginVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final MailService mailService;

    public AuthController(UserService userService, MailService mailService) {
        this.userService = userService;
        this.mailService = mailService;
    }

    @PostMapping("/register")
    public Result<Void> register(@RequestBody @Valid RegisterDTO registerDTO) {
        userService.register(registerDTO);
        return Result.success();
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody @Valid LoginDTO loginDTO) {
        LoginVO token = userService.login(loginDTO);
        return Result.success(token);
    }

    @PostMapping("/refresh")
    public Result<LoginVO> refreshToken(@RequestBody @Valid RefreshTokenDTO refreshTokenDTO) {
        return Result.success(userService.refreshToken(refreshTokenDTO.getRefreshToken()));
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        String accessToken = extractBearerToken(request.getHeader("Authorization"));
        String refreshToken = request.getHeader("Refresh-Token");
        userService.logout(accessToken, refreshToken);
        return Result.success();
    }

    @PostMapping("/password/reset-code")
    public Result<Void> sendPasswordResetCode(@RequestBody @Valid SendPasswordResetCodeDTO dto) {
        mailService.sendPasswordResetCode(dto);
        return Result.success();
    }

    @PostMapping("/password/reset")
    public Result<Void> resetPassword(@RequestBody @Valid ResetPasswordDTO dto) {
        mailService.resetPassword(dto);
        return Result.success();
    }

    private String extractBearerToken(String header) {
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
