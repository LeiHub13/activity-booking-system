package com.example.activitybookingsystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.activitybookingsystem.dto.RegisterDTO;
import com.example.activitybookingsystem.entity.User;

public interface UserService extends IService<User> {

    void register(RegisterDTO registerDTO);
}
