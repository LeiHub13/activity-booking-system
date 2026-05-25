package com.example.activitybookingsystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.activitybookingsystem.dto.CreateRegistrationDTO;
import com.example.activitybookingsystem.entity.Registration;
import com.example.activitybookingsystem.vo.AdminRegistrationVO;
import com.example.activitybookingsystem.vo.MyRegistrationVO;
import com.example.activitybookingsystem.vo.PageVO;
import com.example.activitybookingsystem.vo.RegistrationVO;

public interface RegistrationService extends IService<Registration> {

    RegistrationVO registerActivity(CreateRegistrationDTO createRegistrationDTO);
    RegistrationVO cancelRegistration(Long registrationId);
    RegistrationVO approveRegistration(Long registrationId);
    RegistrationVO rejectRegistration(Long registrationId);
    PageVO<MyRegistrationVO> listMyRegistrations(Long pageNum, Long pageSize, String category);
    PageVO<AdminRegistrationVO> listAdminRegistrations(Long pageNum, Long pageSize, String status,
                                                       Long activityId, String keyword);
}
