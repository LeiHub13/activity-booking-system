package com.example.activitybookingsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.activitybookingsystem.entity.Activity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ActivityMapper extends BaseMapper<Activity> {

    @Update("""
            update activity
            set current_count = current_count + 1,
                update_time = now()
            where id = #{activityId}
              and status = 'PUBLISHED'
              and current_count < max_count
            """)
    int increaseCurrentCount(@Param("activityId") Long activityId);

    @Update("""
            update activity
            set current_count = current_count - 1,
                update_time = now()
            where id = #{activityId}
              and current_count > 0
            """)
    int decreaseCurrentCount(@Param("activityId") Long activityId);
}
