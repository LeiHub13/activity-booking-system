package com.example.activitybookingsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.activitybookingsystem.entity.UserRole;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {
    //注册是把用户角色关系插入user_role表
}
