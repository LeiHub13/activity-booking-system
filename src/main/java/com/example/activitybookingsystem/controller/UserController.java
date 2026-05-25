package com.example.activitybookingsystem.controller;

import com.example.activitybookingsystem.common.result.Result;
import com.example.activitybookingsystem.dto.UpdateUserInfoDTO;
import com.example.activitybookingsystem.service.UserService;
import com.example.activitybookingsystem.vo.UserInfoVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/test")
    public Result<String> test() {
        return Result.success("用户接口访问成功");
    }

    @GetMapping("/me")
    public Result<UserInfoVO> me() {
        return Result.success(userService.getCurrentUserInfo());
    }

    @PutMapping("/me")
    public Result<UserInfoVO> updateMe(@RequestBody @Valid UpdateUserInfoDTO updateUserInfoDTO) {
        return Result.success(userService.updateCurrentUserInfo(updateUserInfoDTO));
    }
}
