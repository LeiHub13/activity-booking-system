package com.example.activitybookingsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.activitybookingsystem.entity.Registration;
import com.example.activitybookingsystem.vo.AdminRegistrationVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface RegistrationMapper extends BaseMapper<Registration> {

    IPage<AdminRegistrationVO> selectAdminRegistrationPage(Page<AdminRegistrationVO> page,
                                                           @Param("status") String status,
                                                           @Param("activityId") Long activityId,
                                                           @Param("keyword") String keyword);

    @Update("""
            update registration
            set status = 'CANCELED',
                update_time = now()
            where id = #{registrationId}
              and user_id = #{userId}
              and status in ('PENDING', 'APPROVED')
            """)
    int cancelRegistrationIfAllowed(@Param("registrationId") Long registrationId,
                                    @Param("userId") Long userId);

    @Update("""
            update registration
            set status = 'APPROVED',
                audit_user_id = #{auditUserId},
                audit_time = now(),
                update_time = now()
            where id = #{registrationId}
              and status = 'PENDING'
            """)
    int approveRegistrationIfPending(@Param("registrationId") Long registrationId,
                                     @Param("auditUserId") Long auditUserId);

    @Update("""
            update registration
            set status = 'REJECTED',
                audit_user_id = #{auditUserId},
                audit_time = now(),
                update_time = now()
            where id = #{registrationId}
              and status = 'PENDING'
            """)
    int rejectRegistrationIfPending(@Param("registrationId") Long registrationId,
                                    @Param("auditUserId") Long auditUserId);

    @Update("""
            update registration
            set status = 'PENDING',
                remark = #{remark},
                audit_user_id = null,
                audit_time = null,
                update_time = now()
            where id = #{registrationId}
              and status = 'CANCELED'
            """)
    int reactivateCanceledRegistration(@Param("registrationId") Long registrationId,
                                       @Param("remark") String remark);
}
