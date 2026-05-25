package com.example.activitybookingsystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.activitybookingsystem.dto.CreateActivityDTO;
import com.example.activitybookingsystem.dto.UpdateActivityDTO;
import com.example.activitybookingsystem.entity.Activity;
import com.example.activitybookingsystem.vo.ActivityRegistrationStatsVO;
import com.example.activitybookingsystem.vo.ActivityVO;
import com.example.activitybookingsystem.vo.PageVO;

import java.util.List;

public interface ActivityService extends IService<Activity> {
    ActivityVO createActivity(CreateActivityDTO createActivityDTO);
    ActivityVO updateActivity(Long activityId, UpdateActivityDTO updateActivityDTO);
    ActivityVO offlineActivity(Long activityId);
    PageVO<ActivityVO> listPublishedActivities(Long pageNum, Long pageSize);
    PageVO<ActivityRegistrationStatsVO> listActivityRegistrationStats(Long pageNum, Long pageSize, String keyword);
    List<ActivityRegistrationStatsVO> listPopularActivityRanking(Integer limit);
    ActivityVO getActivityDetail(Long activityId);
}
