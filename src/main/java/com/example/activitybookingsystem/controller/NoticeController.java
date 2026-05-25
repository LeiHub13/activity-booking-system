package com.example.activitybookingsystem.controller;

import com.example.activitybookingsystem.common.result.Result;
import com.example.activitybookingsystem.service.NoticeService;
import com.example.activitybookingsystem.vo.NoticeVO;
import com.example.activitybookingsystem.vo.PageVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notice")
public class NoticeController {

    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @GetMapping("/my")
    public Result<PageVO<NoticeVO>> listMyNotices(@RequestParam(defaultValue = "1") Long pageNum,
                                                   @RequestParam(defaultValue = "10") Long pageSize,
                                                   @RequestParam(defaultValue = "false") Boolean unreadOnly) {
        return Result.success(noticeService.listMyNotices(pageNum, pageSize, unreadOnly));
    }

    @GetMapping("/unread-count")
    public Result<Long> countMyUnreadNotices() {
        return Result.success(noticeService.countMyUnreadNotices());
    }

    @PutMapping("/{noticeId}/read")
    public Result<NoticeVO> markAsRead(@PathVariable Long noticeId) {
        return Result.success(noticeService.markAsRead(noticeId));
    }

    @PutMapping("/read-all")
    public Result<Void> markAllAsRead() {
        noticeService.markAllAsRead();
        return Result.success();
    }
}
