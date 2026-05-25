package com.example.activitybookingsystem.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.activitybookingsystem.entity.Activity;
import com.example.activitybookingsystem.vo.ActivityRegistrationStatsVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

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

    IPage<ActivityRegistrationStatsVO> selectActivityRegistrationStatsPage(
            Page<ActivityRegistrationStatsVO> page,
            @Param("keyword") String keyword
    );

    List<ActivityRegistrationStatsVO> selectPopularActivityRanking(@Param("limit") Integer limit);
}
