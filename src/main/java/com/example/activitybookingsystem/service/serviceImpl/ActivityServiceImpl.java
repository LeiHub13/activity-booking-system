package com.example.activitybookingsystem.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.activitybookingsystem.common.exception.BusinessException;
import com.example.activitybookingsystem.dto.CreateActivityDTO;
import com.example.activitybookingsystem.dto.UpdateActivityDTO;
import com.example.activitybookingsystem.entity.Activity;
import com.example.activitybookingsystem.entity.User;
import com.example.activitybookingsystem.mapper.ActivityMapper;
import com.example.activitybookingsystem.mapper.UserMapper;
import com.example.activitybookingsystem.service.ActivityService;
import com.example.activitybookingsystem.vo.ActivityVO;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ActivityServiceImpl extends ServiceImpl<ActivityMapper, Activity> implements ActivityService {

    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_CLOSED = "CLOSED";

    private final ActivityMapper activityMapper;
    private final UserMapper userMapper;

    public ActivityServiceImpl(ActivityMapper activityMapper, UserMapper userMapper) {
        this.activityMapper = activityMapper;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ActivityVO createActivity(CreateActivityDTO createActivityDTO) {
        validateActivityTime(
                createActivityDTO.getStartTime(),
                createActivityDTO.getEndTime(),
                createActivityDTO.getSignupStartTime(),
                createActivityDTO.getSignupEndTime()
        );

        User publisher = getCurrentUserEntity();
        LocalDateTime now = LocalDateTime.now();

        Activity activity = new Activity();
        BeanUtils.copyProperties(createActivityDTO, activity);
        activity.setCurrentCount(0);
        activity.setStatus(STATUS_PUBLISHED);
        activity.setPublisherId(publisher.getId());
        activity.setCreateTime(now);
        activity.setUpdateTime(now);

        activityMapper.insert(activity);
        return toActivityVO(activity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ActivityVO updateActivity(Long activityId, UpdateActivityDTO updateActivityDTO) {
        Activity activity = getActivityById(activityId);
        validateActivityTime(
                updateActivityDTO.getStartTime(),
                updateActivityDTO.getEndTime(),
                updateActivityDTO.getSignupStartTime(),
                updateActivityDTO.getSignupEndTime()
        );

        Integer currentCount = activity.getCurrentCount() == null ? 0 : activity.getCurrentCount();
        if (updateActivityDTO.getMaxCount() < currentCount) {
            throw new BusinessException("活动人数上限不能小于当前报名人数");
        }

        activity.setTitle(updateActivityDTO.getTitle());
        activity.setContent(updateActivityDTO.getContent());
        activity.setLocation(updateActivityDTO.getLocation());
        activity.setStartTime(updateActivityDTO.getStartTime());
        activity.setEndTime(updateActivityDTO.getEndTime());
        activity.setSignupStartTime(updateActivityDTO.getSignupStartTime());
        activity.setSignupEndTime(updateActivityDTO.getSignupEndTime());
        activity.setMaxCount(updateActivityDTO.getMaxCount());
        activity.setUpdateTime(LocalDateTime.now());

        activityMapper.updateById(activity);
        return toActivityVO(activity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ActivityVO offlineActivity(Long activityId) {
        Activity activity = getActivityById(activityId);
        if (STATUS_CLOSED.equals(activity.getStatus())) {
            throw new BusinessException("活动已下线");
        }

        activity.setStatus(STATUS_CLOSED);
        activity.setUpdateTime(LocalDateTime.now());
        activityMapper.updateById(activity);
        return toActivityVO(activity);
    }

    @Override
    public List<ActivityVO> listPublishedActivities() {
        LambdaQueryWrapper<Activity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Activity::getStatus, STATUS_PUBLISHED)
                .orderByDesc(Activity::getCreateTime);

        return activityMapper.selectList(queryWrapper)
                .stream()
                .map(this::toActivityVO)
                .toList();
    }

    @Override
    public ActivityVO getActivityDetail(Long activityId) {
        return toActivityVO(getActivityById(activityId));
    }

    private Activity getActivityById(Long activityId) {
        if (activityId == null) {
            throw new BusinessException("活动ID不能为空");
        }

        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }
        return activity;
    }

    private void validateActivityTime(LocalDateTime startTime,
                                      LocalDateTime endTime,
                                      LocalDateTime signupStartTime,
                                      LocalDateTime signupEndTime) {
        if (!startTime.isBefore(endTime)) {
            throw new BusinessException("活动开始时间必须早于活动结束时间");
        }
        if (!signupStartTime.isBefore(signupEndTime)) {
            throw new BusinessException("报名开始时间必须早于报名结束时间");
        }
        if (signupEndTime.isAfter(startTime)) {
            throw new BusinessException("报名结束时间不能晚于活动开始时间");
        }
    }

    private User getCurrentUserEntity() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new BusinessException("当前用户未登录");
        }

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, authentication.getName());
        User user = userMapper.selectOne(queryWrapper);
        if (user == null) {
            throw new BusinessException("当前用户不存在");
        }
        return user;
    }

    private ActivityVO toActivityVO(Activity activity) {
        ActivityVO activityVO = new ActivityVO();
        BeanUtils.copyProperties(activity, activityVO);
        return activityVO;
    }
}
