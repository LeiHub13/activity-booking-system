package com.example.activitybookingsystem.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.activitybookingsystem.common.exception.BusinessException;
import com.example.activitybookingsystem.dto.CreateRatingDTO;
import com.example.activitybookingsystem.entity.Activity;
import com.example.activitybookingsystem.entity.ActivityRating;
import com.example.activitybookingsystem.entity.Registration;
import com.example.activitybookingsystem.entity.User;
import com.example.activitybookingsystem.mapper.ActivityMapper;
import com.example.activitybookingsystem.mapper.ActivityRatingMapper;
import com.example.activitybookingsystem.mapper.RegistrationMapper;
import com.example.activitybookingsystem.mapper.UserMapper;
import com.example.activitybookingsystem.service.PointService;
import com.example.activitybookingsystem.service.RatingService;
import com.example.activitybookingsystem.vo.ActivityRatingVO;
import com.example.activitybookingsystem.vo.PageVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class RatingServiceImpl extends ServiceImpl<ActivityRatingMapper, ActivityRating> implements RatingService {

    private static final String REGISTRATION_STATUS_APPROVED = "APPROVED";
    private static final String POINT_TYPE_RATING = "RATING";
    private static final int RATING_POINTS = 5;

    private final ActivityRatingMapper activityRatingMapper;
    private final RegistrationMapper registrationMapper;
    private final ActivityMapper activityMapper;
    private final UserMapper userMapper;
    private final PointService pointService;

    public RatingServiceImpl(ActivityRatingMapper activityRatingMapper,
                             RegistrationMapper registrationMapper,
                             ActivityMapper activityMapper,
                             UserMapper userMapper,
                             PointService pointService) {
        this.activityRatingMapper = activityRatingMapper;
        this.registrationMapper = registrationMapper;
        this.activityMapper = activityMapper;
        this.userMapper = userMapper;
        this.pointService = pointService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ActivityRatingVO createRating(CreateRatingDTO dto) {
        User currentUser = getCurrentUserEntity();
        Activity activity = activityMapper.selectById(dto.getActivityId());
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }

        LambdaQueryWrapper<Registration> registrationQuery = new LambdaQueryWrapper<>();
        registrationQuery.eq(Registration::getUserId, currentUser.getId())
                .eq(Registration::getActivityId, dto.getActivityId())
                .eq(Registration::getStatus, REGISTRATION_STATUS_APPROVED);
        if (registrationMapper.selectCount(registrationQuery) == 0) {
            throw new BusinessException("只有报名并审核通过的用户才能评价");
        }

        LambdaQueryWrapper<ActivityRating> existsQuery = new LambdaQueryWrapper<>();
        existsQuery.eq(ActivityRating::getUserId, currentUser.getId())
                .eq(ActivityRating::getActivityId, dto.getActivityId());
        if (activityRatingMapper.selectCount(existsQuery) > 0) {
            throw new BusinessException("你已评价过该活动");
        }

        LocalDateTime now = LocalDateTime.now();
        ActivityRating rating = new ActivityRating();
        rating.setUserId(currentUser.getId());
        rating.setActivityId(dto.getActivityId());
        rating.setRating(dto.getRating());
        rating.setContent(trimToNull(dto.getContent()));
        rating.setCreateTime(now);
        try {
            activityRatingMapper.insert(rating);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException("你已评价过该活动");
        }

        pointService.awardPoints(currentUser.getId(), POINT_TYPE_RATING, rating.getId(),
                RATING_POINTS, "评价活动「" + activity.getTitle() + "」");
        return toRatingVO(rating, activity.getTitle());
    }

    @Override
    public PageVO<ActivityRatingVO> listMyRatings(Long pageNum, Long pageSize) {
        User currentUser = getCurrentUserEntity();
        LambdaQueryWrapper<ActivityRating> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ActivityRating::getUserId, currentUser.getId())
                .orderByDesc(ActivityRating::getId);
        Page<ActivityRating> result = activityRatingMapper.selectPage(new Page<>(pageNum, pageSize), queryWrapper);
        return buildRatingPage(result);
    }

    @Override
    public PageVO<ActivityRatingVO> listActivityRatings(Long activityId, Long pageNum, Long pageSize) {
        if (activityId == null) {
            throw new BusinessException("活动ID不能为空");
        }
        LambdaQueryWrapper<ActivityRating> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ActivityRating::getActivityId, activityId)
                .orderByDesc(ActivityRating::getId);
        Page<ActivityRating> result = activityRatingMapper.selectPage(new Page<>(pageNum, pageSize), queryWrapper);
        return buildRatingPage(result);
    }

    private PageVO<ActivityRatingVO> buildRatingPage(Page<ActivityRating> result) {
        List<Long> activityIds = result.getRecords().stream()
                .map(ActivityRating::getActivityId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, String> titleMap = activityIds.isEmpty()
                ? Map.of()
                : activityMapper.selectBatchIds(activityIds).stream()
                        .collect(Collectors.toMap(Activity::getId, Activity::getTitle));

        PageVO<ActivityRatingVO> pageVO = new PageVO<>();
        pageVO.setTotal(result.getTotal());
        pageVO.setPageNum(result.getCurrent());
        pageVO.setPageSize(result.getSize());
        pageVO.setRecords(result.getRecords().stream()
                .map(rating -> toRatingVO(rating, titleMap.get(rating.getActivityId())))
                .toList());
        return pageVO;
    }

    private ActivityRatingVO toRatingVO(ActivityRating rating, String activityTitle) {
        ActivityRatingVO vo = new ActivityRatingVO();
        vo.setId(rating.getId());
        vo.setActivityId(rating.getActivityId());
        vo.setActivityTitle(activityTitle);
        vo.setRating(rating.getRating());
        vo.setContent(rating.getContent());
        vo.setCreateTime(rating.getCreateTime());
        return vo;
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

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}