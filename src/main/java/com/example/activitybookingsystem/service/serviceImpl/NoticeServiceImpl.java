package com.example.activitybookingsystem.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.activitybookingsystem.common.exception.BusinessException;
import com.example.activitybookingsystem.entity.Activity;
import com.example.activitybookingsystem.entity.Notice;
import com.example.activitybookingsystem.entity.Registration;
import com.example.activitybookingsystem.entity.User;
import com.example.activitybookingsystem.mapper.NoticeMapper;
import com.example.activitybookingsystem.mapper.UserMapper;
import com.example.activitybookingsystem.service.NoticeService;
import com.example.activitybookingsystem.vo.NoticeVO;
import com.example.activitybookingsystem.vo.PageVO;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class NoticeServiceImpl extends ServiceImpl<NoticeMapper, Notice> implements NoticeService {

    private static final int NOTICE_UNREAD = 0;
    private static final int NOTICE_READ = 1;
    private static final String TYPE_REGISTRATION_APPROVED = "REGISTRATION_APPROVED";
    private static final String TYPE_REGISTRATION_REJECTED = "REGISTRATION_REJECTED";

    private final NoticeMapper noticeMapper;
    private final UserMapper userMapper;

    public NoticeServiceImpl(NoticeMapper noticeMapper, UserMapper userMapper) {
        this.noticeMapper = noticeMapper;
        this.userMapper = userMapper;
    }

    @Override
    public void createRegistrationAuditNotice(Registration registration, Activity activity, boolean approved) {
        if (registration == null || registration.getUserId() == null) {
            throw new BusinessException("报名记录不存在，无法生成通知");
        }

        String activityTitle = activity == null ? "相关活动" : activity.getTitle();
        Notice notice = new Notice();
        notice.setUserId(registration.getUserId());
        notice.setTitle(approved ? "报名审核通过" : "报名审核未通过");
        notice.setContent(approved
                ? "你报名的活动「" + activityTitle + "」已审核通过，请按时参加。"
                : "你报名的活动「" + activityTitle + "」未通过审核，活动名额已释放。");
        notice.setType(approved ? TYPE_REGISTRATION_APPROVED : TYPE_REGISTRATION_REJECTED);
        notice.setIsRead(NOTICE_UNREAD);
        notice.setCreateTime(LocalDateTime.now());
        noticeMapper.insert(notice);
    }

    @Override
    public PageVO<NoticeVO> listMyNotices(Long pageNum, Long pageSize, Boolean unreadOnly) {
        User currentUser = getCurrentUserEntity();

        LambdaQueryWrapper<Notice> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Notice::getUserId, currentUser.getId());
        if (Boolean.TRUE.equals(unreadOnly)) {
            queryWrapper.eq(Notice::getIsRead, NOTICE_UNREAD);
        }
        queryWrapper.orderByAsc(Notice::getIsRead)
                .orderByDesc(Notice::getCreateTime);

        Page<Notice> result = noticeMapper.selectPage(new Page<>(pageNum, pageSize), queryWrapper);
        List<NoticeVO> records = result.getRecords().stream().map(this::toNoticeVO).toList();

        PageVO<NoticeVO> pageVO = new PageVO<>();
        pageVO.setTotal(result.getTotal());
        pageVO.setPageNum(result.getCurrent());
        pageVO.setPageSize(result.getSize());
        pageVO.setRecords(records);
        return pageVO;
    }

    @Override
    public Long countMyUnreadNotices() {
        User currentUser = getCurrentUserEntity();

        LambdaQueryWrapper<Notice> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Notice::getUserId, currentUser.getId())
                .eq(Notice::getIsRead, NOTICE_UNREAD);
        return noticeMapper.selectCount(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NoticeVO markAsRead(Long noticeId) {
        User currentUser = getCurrentUserEntity();
        Notice notice = noticeMapper.selectById(noticeId);
        validateOwnNotice(notice, currentUser.getId());

        if (!Objects.equals(notice.getIsRead(), NOTICE_READ)) {
            notice.setIsRead(NOTICE_READ);
            int updated = noticeMapper.updateById(notice);
            if (updated != 1) {
                throw new BusinessException("通知标记已读失败");
            }
        }
        return toNoticeVO(notice);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllAsRead() {
        User currentUser = getCurrentUserEntity();

        Notice notice = new Notice();
        notice.setIsRead(NOTICE_READ);

        LambdaQueryWrapper<Notice> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Notice::getUserId, currentUser.getId())
                .eq(Notice::getIsRead, NOTICE_UNREAD);
        noticeMapper.update(notice, queryWrapper);
    }

    private void validateOwnNotice(Notice notice, Long currentUserId) {
        if (notice == null) {
            throw new BusinessException("通知不存在");
        }
        if (!Objects.equals(notice.getUserId(), currentUserId)) {
            throw new BusinessException("只能操作自己的通知");
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

    private NoticeVO toNoticeVO(Notice notice) {
        NoticeVO noticeVO = new NoticeVO();
        BeanUtils.copyProperties(notice, noticeVO);
        return noticeVO;
    }
}
