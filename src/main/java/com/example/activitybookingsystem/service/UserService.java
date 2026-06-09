package com.example.activitybookingsystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.activitybookingsystem.dto.LoginDTO;
import com.example.activitybookingsystem.dto.RegisterDTO;
import com.example.activitybookingsystem.dto.UpdateUserInfoDTO;
import com.example.activitybookingsystem.entity.User;
import com.example.activitybookingsystem.vo.LoginVO;
import com.example.activitybookingsystem.vo.UserInfoVO;

public interface UserService extends IService<User> {

    void register(RegisterDTO registerDTO);
    LoginVO login(LoginDTO loginDTO);
    LoginVO refreshToken(String refreshToken);
    void logout(String accessToken, String refreshToken);
    UserInfoVO getCurrentUserInfo();
    UserInfoVO updateCurrentUserInfo(UpdateUserInfoDTO updateUserInfoDTO);
}
