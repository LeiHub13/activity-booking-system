package com.example.activitybookingsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.activitybookingsystem.entity.Role;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface RoleMapper extends BaseMapper<Role> {
    //角色编码查角色
    Role selectByRoleCode(String roleCode);
    //用户id查角色编码
    List<String> selectRoleCodesByUserId(Long userId);
}
