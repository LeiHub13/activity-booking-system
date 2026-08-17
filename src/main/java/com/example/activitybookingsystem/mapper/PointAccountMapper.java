package com.example.activitybookingsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.activitybookingsystem.entity.PointAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PointAccountMapper extends BaseMapper<PointAccount> {

    @Update("""
            update point_account
            set balance = balance + #{points},
                total_earned = total_earned + #{points},
                update_time = now()
            where user_id = #{userId}
            """)
    int increaseBalance(@Param("userId") Long userId, @Param("points") int points);

    @Update("""
            update point_account
            set balance = balance - #{points},
                total_spent = total_spent + #{points},
                update_time = now()
            where user_id = #{userId}
              and balance >= #{points}
            """)
    int decreaseBalance(@Param("userId") Long userId, @Param("points") int points);
}