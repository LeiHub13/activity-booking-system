package com.example.activitybookingsystem.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface MailMapper {
    @Update("""
      update user
      set password = #{newPassword}
      where email = #{email}
             """)
    void updatePassword(@Param("email") String email, @Param("newPassword") String newPassword);
}
