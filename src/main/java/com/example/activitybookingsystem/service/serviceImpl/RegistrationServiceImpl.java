package com.example.activitybookingsystem.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.activitybookingsystem.cache.ActivityCacheService;
import com.example.activitybookingsystem.common.exception.BusinessException;
import com.example.activitybookingsystem.dto.CreateRegistrationDTO;
import com.example.activitybookingsystem.entity.Activity;
import com.example.activitybookingsystem.entity.Registration;
import com.example.activitybookingsystem.entity.User;
import com.example.activitybookingsystem.mapper.ActivityMapper;
import com.example.activitybookingsystem.mapper.RegistrationMapper;
import com.example.activitybookingsystem.mapper.UserMapper;
import com.example.activitybookingsystem.message.AuditNoticeMessage;
import com.example.activitybookingsystem.mq.AuditNoticeProducer;
import com.example.activitybookingsystem.service.RegistrationService;
import com.example.activitybookingsystem.vo.AdminRegistrationVO;
import com.example.activitybookingsystem.vo.MyRegistrationVO;
import com.example.activitybookingsystem.vo.PageVO;
import com.example.activitybookingsystem.vo.RegistrationVO;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class RegistrationServiceImpl extends ServiceImpl<RegistrationMapper, Registration> implements RegistrationService {

    private static final String ACTIVITY_STATUS_PUBLISHED = "PUBLISHED";
    private static final String REGISTRATION_STATUS_PENDING = "PENDING";
    private static final String REGISTRATION_STATUS_APPROVED = "APPROVED";
    private static final String REGISTRATION_STATUS_REJECTED = "REJECTED";
    private static final String REGISTRATION_STATUS_CANCELED = "CANCELED";
    private static final String REGISTRATION_CATEGORY_ALL = "ALL";
    private static final String REGISTRATION_CATEGORY_ACTIVE = "ACTIVE";

    private final RegistrationMapper registrationMapper;
    private final ActivityMapper activityMapper;
    private final UserMapper userMapper;
    private final ActivityCacheService activityCacheService;
    private final AuditNoticeProducer auditNoticeProducer;

    public RegistrationServiceImpl(RegistrationMapper registrationMapper,
                                   ActivityMapper activityMapper,
                                   UserMapper userMapper,
                                   ActivityCacheService activityCacheService,
                                   AuditNoticeProducer auditNoticeProducer) {
        this.registrationMapper = registrationMapper;
        this.activityMapper = activityMapper;
        this.userMapper = userMapper;
        this.activityCacheService = activityCacheService;
        this.auditNoticeProducer = auditNoticeProducer;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RegistrationVO registerActivity(CreateRegistrationDTO createRegistrationDTO) {
        User currentUser = getCurrentUserEntity();
        Activity activity = activityMapper.selectById(createRegistrationDTO.getActivityId());
        validateActivityCanRegister(activity);

        Registration existingRegistration = getUserActivityRegistration(currentUser.getId(), activity.getId());
        if (existingRegistration != null) {
            if (REGISTRATION_STATUS_CANCELED.equals(existingRegistration.getStatus())) {
                return reactivateCanceledRegistration(existingRegistration, createRegistrationDTO);
            }
            throw new BusinessException("你已报名该活动");
        }

        int updated = activityMapper.increaseCurrentCount(activity.getId());
        if (updated != 1) {
            throw new BusinessException("活动人数已满或活动不可报名");
        }

        LocalDateTime now = LocalDateTime.now();
        Registration registration = new Registration();
        registration.setUserId(currentUser.getId());
        registration.setActivityId(activity.getId());
        registration.setStatus(REGISTRATION_STATUS_PENDING);
        registration.setRemark(trimToNull(createRegistrationDTO.getRemark()));
        registration.setCreateTime(now);
        registration.setUpdateTime(now);

        try {
            registrationMapper.insert(registration);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException("你已报名该活动");
        }

        activityCacheService.evictPopularRankingCache();
        return toRegistrationVO(registration);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RegistrationVO cancelRegistration(Long registrationId) {
        User currentUser = getCurrentUserEntity();
        Registration registration = registrationMapper.selectById(registrationId);
        validateRegistrationCanCancel(registration, currentUser.getId());

        // 先用条件更新锁住状态变更，只有真正取消成功才允许扣减活动人数。
        int updatedRegistration = registrationMapper.cancelRegistrationIfAllowed(registrationId, currentUser.getId());
        if (updatedRegistration != 1) {
            throw new BusinessException("报名状态已变化，不能重复取消");
        }

        int updatedCount = activityMapper.decreaseCurrentCount(registration.getActivityId());
        if (updatedCount != 1) {
            throw new BusinessException("活动报名人数更新失败");
        }

        activityCacheService.evictPopularRankingCache();
        return toRegistrationVO(registrationMapper.selectById(registrationId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RegistrationVO approveRegistration(Long registrationId) {
        User auditor = getCurrentUserEntity();
        Registration registration = registrationMapper.selectById(registrationId);
        validateRegistrationCanApprove(registration);

        int updated = registrationMapper.approveRegistrationIfPending(registrationId, auditor.getId());
        if (updated != 1) {
            throw new BusinessException("报名状态已变化，不能重复审核通过");
        }

        Registration updatedRegistration = registrationMapper.selectById(registrationId);
        sendAuditNoticeAfterCommit(updatedRegistration, true);
        return toRegistrationVO(updatedRegistration);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RegistrationVO rejectRegistration(Long registrationId) {
        User auditor = getCurrentUserEntity();
        Registration registration = registrationMapper.selectById(registrationId);
        validateRegistrationCanReject(registration);

        // 拒绝也先抢状态变更，避免管理员重复点击导致活动人数重复扣减。
        int updated = registrationMapper.rejectRegistrationIfPending(registrationId, auditor.getId());
        if (updated != 1) {
            throw new BusinessException("报名状态已变化，不能重复审核拒绝");
        }

        int updatedCount = activityMapper.decreaseCurrentCount(registration.getActivityId());
        if (updatedCount != 1) {
            throw new BusinessException("活动报名人数更新失败");
        }

        activityCacheService.evictPopularRankingCache();
        Registration updatedRegistration = registrationMapper.selectById(registrationId);
        sendAuditNoticeAfterCommit(updatedRegistration, false);
        return toRegistrationVO(updatedRegistration);
    }

    @Override
    public PageVO<MyRegistrationVO> listMyRegistrations(Long pageNum, Long pageSize, String category) {
        User currentUser = getCurrentUserEntity();

        LambdaQueryWrapper<Registration> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Registration::getUserId, currentUser.getId());
        String currentCategory = trimToNull(category);
        if (currentCategory == null || REGISTRATION_CATEGORY_ALL.equalsIgnoreCase(currentCategory)) {
            // 查询全部报名记录，不过滤状态。
        } else if (REGISTRATION_CATEGORY_ACTIVE.equalsIgnoreCase(currentCategory)) {
            queryWrapper.ne(Registration::getStatus, REGISTRATION_STATUS_CANCELED);
        } else if (REGISTRATION_STATUS_APPROVED.equalsIgnoreCase(currentCategory)) {
            queryWrapper.eq(Registration::getStatus, REGISTRATION_STATUS_APPROVED);
        } else if (REGISTRATION_STATUS_CANCELED.equalsIgnoreCase(currentCategory)) {
            queryWrapper.eq(Registration::getStatus, REGISTRATION_STATUS_CANCELED);
        } else if (REGISTRATION_STATUS_REJECTED.equalsIgnoreCase(currentCategory)) {
            queryWrapper.eq(Registration::getStatus, REGISTRATION_STATUS_REJECTED);
        } else {
            throw new BusinessException("报名分类不正确");
        }
        queryWrapper.orderByDesc(Registration::getUpdateTime);

        Page<Registration> result = registrationMapper.selectPage(new Page<>(pageNum, pageSize), queryWrapper);
        List<Registration> registrations = result.getRecords();
        if (registrations.isEmpty()) {
            return buildMyRegistrationPage(result, List.of());
        }

        List<Long> activityIds = registrations.stream()
                .map(Registration::getActivityId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, Activity> activityMap = activityIds.isEmpty()
                ? Map.of()
                : activityMapper.selectBatchIds(activityIds)
                        .stream()
                        .collect(Collectors.toMap(Activity::getId, activity -> activity));

        List<MyRegistrationVO> records = registrations.stream()
                .map(registration -> toMyRegistrationVO(registration, activityMap.get(registration.getActivityId())))
                .toList();

        return buildMyRegistrationPage(result, records);
    }

    @Override
    public PageVO<AdminRegistrationVO> listAdminRegistrations(Long pageNum, Long pageSize, String status,
                                                              Long activityId, String keyword) {
        IPage<AdminRegistrationVO> result = registrationMapper.selectAdminRegistrationPage(
                new Page<>(pageNum, pageSize),
                trimToNull(status),
                activityId,
                trimToNull(keyword)
        );

        PageVO<AdminRegistrationVO> pageVO = new PageVO<>();
        pageVO.setTotal(result.getTotal());
        pageVO.setPageNum(result.getCurrent());
        pageVO.setPageSize(result.getSize());
        pageVO.setRecords(result.getRecords());
        return pageVO;
    }

    private void validateActivityCanRegister(Activity activity) {
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }
        if (!ACTIVITY_STATUS_PUBLISHED.equals(activity.getStatus())) {
            throw new BusinessException("活动未发布或已下线，不能报名");
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activity.getSignupStartTime())) {
            throw new BusinessException("报名尚未开始");
        }
        if (now.isAfter(activity.getSignupEndTime())) {
            throw new BusinessException("报名已结束");
        }

        Integer currentCount = activity.getCurrentCount() == null ? 0 : activity.getCurrentCount();
        if (currentCount >= activity.getMaxCount()) {
            throw new BusinessException("活动人数已满");
        }
    }

    private Registration getUserActivityRegistration(Long userId, Long activityId) {
        LambdaQueryWrapper<Registration> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Registration::getUserId, userId)
                .eq(Registration::getActivityId, activityId);

        return registrationMapper.selectOne(queryWrapper);
    }

    private RegistrationVO reactivateCanceledRegistration(Registration registration,
                                                          CreateRegistrationDTO createRegistrationDTO) {
        int updated = activityMapper.increaseCurrentCount(registration.getActivityId());
        if (updated != 1) {
            throw new BusinessException("活动人数已满或活动不可报名");
        }

        int updatedRegistration = registrationMapper.reactivateCanceledRegistration(
                registration.getId(),
                trimToNull(createRegistrationDTO.getRemark())
        );
        if (updatedRegistration != 1) {
            throw new BusinessException("报名状态已变化，请刷新后重试");
        }

        activityCacheService.evictPopularRankingCache();
        return toRegistrationVO(registrationMapper.selectById(registration.getId()));
    }

    private void validateRegistrationCanCancel(Registration registration, Long currentUserId) {
        if (registration == null) {
            throw new BusinessException("报名记录不存在");
        }
        if (!Objects.equals(registration.getUserId(), currentUserId)) {
            throw new BusinessException("只能取消自己的报名");
        }
        if (REGISTRATION_STATUS_CANCELED.equals(registration.getStatus())) {
            throw new BusinessException("报名已取消");
        }
        if (!REGISTRATION_STATUS_PENDING.equals(registration.getStatus())
                && !REGISTRATION_STATUS_APPROVED.equals(registration.getStatus())) {
            throw new BusinessException("当前报名状态不能取消");
        }
    }

    private void validateRegistrationCanApprove(Registration registration) {
        if (registration == null) {
            throw new BusinessException("报名记录不存在");
        }
        if (REGISTRATION_STATUS_APPROVED.equals(registration.getStatus())) {
            throw new BusinessException("报名已审核通过");
        }
        if (REGISTRATION_STATUS_CANCELED.equals(registration.getStatus())) {
            throw new BusinessException("报名已取消，不能审核通过");
        }
        if (REGISTRATION_STATUS_REJECTED.equals(registration.getStatus())) {
            throw new BusinessException("报名已拒绝，不能审核通过");
        }
        if (!REGISTRATION_STATUS_PENDING.equals(registration.getStatus())) {
            throw new BusinessException("当前报名状态不能审核通过");
        }
    }

    private void sendAuditNoticeAfterCommit(Registration registration, boolean approved) {
        AuditNoticeMessage message = new AuditNoticeMessage(
                registration.getId(),
                registration.getActivityId(),
                approved
        );

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                auditNoticeProducer.send(message);
            }
        });
    }

    private void validateRegistrationCanReject(Registration registration) {
        if (registration == null) {
            throw new BusinessException("报名记录不存在");
        }
        if (REGISTRATION_STATUS_APPROVED.equals(registration.getStatus())) {
            throw new BusinessException("报名已审核通过，不能拒绝");
        }
        if (REGISTRATION_STATUS_CANCELED.equals(registration.getStatus())) {
            throw new BusinessException("报名已取消，不能拒绝");
        }
        if (REGISTRATION_STATUS_REJECTED.equals(registration.getStatus())) {
            throw new BusinessException("报名已拒绝");
        }
        if (!REGISTRATION_STATUS_PENDING.equals(registration.getStatus())) {
            throw new BusinessException("当前报名状态不能审核拒绝");
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

    private RegistrationVO toRegistrationVO(Registration registration) {
        RegistrationVO registrationVO = new RegistrationVO();
        BeanUtils.copyProperties(registration, registrationVO);
        return registrationVO;
    }

    private MyRegistrationVO toMyRegistrationVO(Registration registration, Activity activity) {
        MyRegistrationVO myRegistrationVO = new MyRegistrationVO();
        myRegistrationVO.setRegistrationId(registration.getId());
        myRegistrationVO.setActivityId(registration.getActivityId());
        myRegistrationVO.setRegistrationStatus(registration.getStatus());
        myRegistrationVO.setRemark(registration.getRemark());
        myRegistrationVO.setRegistrationTime(registration.getCreateTime());

        if (activity != null) {
            myRegistrationVO.setActivityTitle(activity.getTitle());
            myRegistrationVO.setActivityContent(activity.getContent());
            myRegistrationVO.setActivityLocation(activity.getLocation());
            myRegistrationVO.setActivityStartTime(activity.getStartTime());
            myRegistrationVO.setActivityEndTime(activity.getEndTime());
            myRegistrationVO.setSignupStartTime(activity.getSignupStartTime());
            myRegistrationVO.setSignupEndTime(activity.getSignupEndTime());
            myRegistrationVO.setMaxCount(activity.getMaxCount());
            myRegistrationVO.setCurrentCount(activity.getCurrentCount());
            myRegistrationVO.setActivityStatus(activity.getStatus());
        }
        return myRegistrationVO;
    }

    private PageVO<MyRegistrationVO> buildMyRegistrationPage(Page<Registration> result, List<MyRegistrationVO> records) {
        PageVO<MyRegistrationVO> pageVO = new PageVO<>();
        pageVO.setTotal(result.getTotal());
        pageVO.setPageNum(result.getCurrent());
        pageVO.setPageSize(result.getSize());
        pageVO.setRecords(records);
        return pageVO;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
