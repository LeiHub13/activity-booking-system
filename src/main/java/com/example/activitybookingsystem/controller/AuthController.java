package com.example.activitybookingsystem.controller;

import com.example.activitybookingsystem.common.result.Result;
import com.example.activitybookingsystem.dto.LoginDTO;
import com.example.activitybookingsystem.dto.RegisterDTO;
import com.example.activitybookingsystem.dto.ResetPasswordDTO;
import com.example.activitybookingsystem.dto.SendPasswordResetCodeDTO;
import com.example.activitybookingsystem.service.MailService;
import com.example.activitybookingsystem.service.UserService;
import com.example.activitybookingsystem.vo.LoginVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = {
        "http://127.0.0.1:5173",
        "http://localhost:5173"
})
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

    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        String token = null;
        if (header != null && header.startsWith("Bearer ")) {
            token = header.substring(7);
        }
        userService.logout(token);
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
}
