package com.example.activitybookingsystem.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.activitybookingsystem.common.exception.BusinessException;
import com.example.activitybookingsystem.config.MinioProperties;
import com.example.activitybookingsystem.entity.Activity;
import com.example.activitybookingsystem.entity.CheckIn;
import com.example.activitybookingsystem.entity.Registration;
import com.example.activitybookingsystem.entity.User;
import com.example.activitybookingsystem.mapper.ActivityMapper;
import com.example.activitybookingsystem.mapper.CheckInMapper;
import com.example.activitybookingsystem.mapper.RegistrationMapper;
import com.example.activitybookingsystem.mapper.UserMapper;
import com.example.activitybookingsystem.service.CheckInService;
import com.example.activitybookingsystem.service.FileUploadService;
import com.example.activitybookingsystem.service.PointService;
import com.example.activitybookingsystem.vo.CheckInStatusVO;
import com.example.activitybookingsystem.vo.CheckInVO;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Service
public class CheckInServiceImpl extends ServiceImpl<CheckInMapper, CheckIn> implements CheckInService {

    private static final String REGISTRATION_STATUS_APPROVED = "APPROVED";
    private static final String POINT_TYPE_CHECK_IN = "CHECK_IN";
    private static final int CHECK_IN_POINTS = 20;

    private final CheckInMapper checkInMapper;
    private final ActivityMapper activityMapper;
    private final RegistrationMapper registrationMapper;
    private final UserMapper userMapper;
    private final FileUploadService fileUploadService;
    private final MinioProperties minioProperties;
    private final PointService pointService;

    public CheckInServiceImpl(CheckInMapper checkInMapper,
                              ActivityMapper activityMapper,
                              RegistrationMapper registrationMapper,
                              UserMapper userMapper,
                              FileUploadService fileUploadService,
                              MinioProperties minioProperties,
                              PointService pointService) {
        this.checkInMapper = checkInMapper;
        this.activityMapper = activityMapper;
        this.registrationMapper = registrationMapper;
        this.userMapper = userMapper;
        this.fileUploadService = fileUploadService;
        this.minioProperties = minioProperties;
        this.pointService = pointService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CheckInVO createCheckIn(Long activityId, MultipartFile file) {
        if (activityId == null) {
            throw new BusinessException("活动ID不能为空");
        }

        User currentUser = getCurrentUserEntity();
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }

        Registration registration = getApprovedRegistration(currentUser.getId(), activityId);
        if (registration == null) {
            throw new BusinessException("只有审核通过的报名才能打卡");
        }

        CheckIn existingCheckIn = getUserActivityCheckIn(currentUser.getId(), activityId);
        if (existingCheckIn != null) {
            throw new BusinessException("该活动已打卡，不能重复打卡");
        }

        FileUploadService.UploadResult uploadResult = fileUploadService.uploadCheckImage(file);
        LocalDateTime now = LocalDateTime.now();

        CheckIn checkIn = new CheckIn();
        checkIn.setUserId(currentUser.getId());
        checkIn.setActivityId(activityId);
        checkIn.setRegistrationId(registration.getId());
        checkIn.setObjectName(uploadResult.objectName());
        checkIn.setImageUrl(uploadResult.url());
        checkIn.setCheckInTime(now);
        checkIn.setCreateTime(now);

        try {
            checkInMapper.insert(checkIn);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("该活动已打卡，不能重复打卡");
        }

        pointService.awardPoints(currentUser.getId(), POINT_TYPE_CHECK_IN, checkIn.getId(),
                CHECK_IN_POINTS, "活动打卡「" + activity.getTitle() + "」");
        return toCheckInVO(checkIn);
    }

    @Override
    public CheckInVO getMyCheckIn(Long activityId) {
        if (activityId == null) {
            throw new BusinessException("活动ID不能为空");
        }

        User currentUser = getCurrentUserEntity();
        CheckIn checkIn = getUserActivityCheckIn(currentUser.getId(), activityId);
        if (checkIn == null) {
            return null;
        }
        return toCheckInVO(checkIn);
    }

    @Override
    public CheckInStatusVO getCheckInStatus(Long activityId) {
        if (activityId == null) {
            throw new BusinessException("活动ID不能为空");
        }

        User currentUser = getCurrentUserEntity();
        CheckInStatusVO statusVO = new CheckInStatusVO();

        LambdaQueryWrapper<Registration> registrationQuery = new LambdaQueryWrapper<>();
        registrationQuery.eq(Registration::getUserId, currentUser.getId())
                .eq(Registration::getActivityId, activityId)
                .orderByDesc(Registration::getCreateTime)
                .last("LIMIT 1");
        Registration registration = registrationMapper.selectOne(registrationQuery);
        if (registration != null) {
            statusVO.setRegistrationStatus(registration.getStatus());
        }

        CheckIn checkIn = getUserActivityCheckIn(currentUser.getId(), activityId);
        if (checkIn != null) {
            statusVO.setCheckIn(toCheckInVO(checkIn));
        }

        return statusVO;
    }

    private Registration getApprovedRegistration(Long userId, Long activityId) {
        LambdaQueryWrapper<Registration> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Registration::getUserId, userId)
                .eq(Registration::getActivityId, activityId)
                .eq(Registration::getStatus, REGISTRATION_STATUS_APPROVED);
        return registrationMapper.selectOne(queryWrapper);
    }

    private CheckIn getUserActivityCheckIn(Long userId, Long activityId) {
        LambdaQueryWrapper<CheckIn> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CheckIn::getUserId, userId)
                .eq(CheckIn::getActivityId, activityId);
        return checkInMapper.selectOne(queryWrapper);
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

    private CheckInVO toCheckInVO(CheckIn checkIn) {
        CheckInVO checkInVO = new CheckInVO();
        BeanUtils.copyProperties(checkIn, checkInVO);
        String objectName = resolveObjectName(checkIn);
        if (objectName != null) {
            // 桶内对象默认私有，统一返回预签名 URL 才能访问。
            checkInVO.setImageUrl(fileUploadService.getCheckImagePresignedUrl(objectName));
        }
        return checkInVO;
    }

    private String resolveObjectName(CheckIn checkIn) {
        String objectName = checkIn.getObjectName();
        if (objectName != null && !objectName.isBlank()) {
            return objectName;
        }
        // 兼容历史数据：从存的完整 URL 里截取对象名。
        String imageUrl = checkIn.getImageUrl();
        if (imageUrl == null) {
            return null;
        }
        String prefix = "/" + minioProperties.getBucketName() + "/";
        int index = imageUrl.indexOf(prefix);
        if (index < 0) {
            return null;
        }
        String extracted = imageUrl.substring(index + prefix.length());
        return extracted.isBlank() ? null : extracted;
    }
}
