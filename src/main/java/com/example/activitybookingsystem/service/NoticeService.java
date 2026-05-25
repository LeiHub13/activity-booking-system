package com.example.activitybookingsystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.activitybookingsystem.entity.Activity;
import com.example.activitybookingsystem.entity.Notice;
import com.example.activitybookingsystem.entity.Registration;
import com.example.activitybookingsystem.vo.NoticeVO;
import com.example.activitybookingsystem.vo.PageVO;

public interface NoticeService extends IService<Notice> {

    void createRegistrationAuditNotice(Registration registration, Activity activity, boolean approved);

    PageVO<NoticeVO> listMyNotices(Long pageNum, Long pageSize, Boolean unreadOnly);

    Long countMyUnreadNotices();

    NoticeVO markAsRead(Long noticeId);

    void markAllAsRead();
}
