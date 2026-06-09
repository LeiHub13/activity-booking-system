package com.example.activitybookingsystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.activitybookingsystem.entity.CheckIn;
import com.example.activitybookingsystem.vo.CheckInVO;
import org.springframework.web.multipart.MultipartFile;

public interface CheckInService extends IService<CheckIn> {

    CheckInVO createCheckIn(Long activityId, MultipartFile file);
}
