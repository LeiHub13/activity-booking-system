package com.example.activitybookingsystem.controller;

import com.example.activitybookingsystem.common.result.Result;
import com.example.activitybookingsystem.service.ActivityService;
import com.example.activitybookingsystem.vo.ActivityVO;
import com.example.activitybookingsystem.vo.PageVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/activity")
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping("lists")
    public Result<PageVO<ActivityVO>> listPublishedActivities(@RequestParam(defaultValue = "1") Long pageNum,
                                                              @RequestParam(defaultValue = "10") Long pageSize) {
        return Result.success(activityService.listPublishedActivities(pageNum, pageSize));
    }

    @GetMapping("/{activityId}")
    public Result<ActivityVO> getActivityDetail(@PathVariable Long activityId) {
        return Result.success(activityService.getActivityDetail(activityId));
    }
}
