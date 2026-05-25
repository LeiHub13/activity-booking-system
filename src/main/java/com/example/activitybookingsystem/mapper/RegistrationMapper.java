package com.example.activitybookingsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.activitybookingsystem.entity.Registration;
import com.example.activitybookingsystem.vo.AdminRegistrationVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RegistrationMapper extends BaseMapper<Registration> {

    IPage<AdminRegistrationVO> selectAdminRegistrationPage(Page<AdminRegistrationVO> page,
                                                           @Param("status") String status,
                                                           @Param("activityId") Long activityId,
                                                           @Param("keyword") String keyword);
}
