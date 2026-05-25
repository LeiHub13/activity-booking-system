package com.example.activitybookingsystem.controller;

import com.example.activitybookingsystem.common.result.Result;
import com.example.activitybookingsystem.dto.CreateActivityDTO;
import com.example.activitybookingsystem.dto.UpdateActivityDTO;
import com.example.activitybookingsystem.service.ActivityService;
import com.example.activitybookingsystem.service.RegistrationService;
import com.example.activitybookingsystem.vo.AdminRegistrationVO;
import com.example.activitybookingsystem.vo.ActivityRegistrationStatsVO;
import com.example.activitybookingsystem.vo.ActivityVO;
import com.example.activitybookingsystem.vo.PageVO;
import com.example.activitybookingsystem.vo.RegistrationVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final ActivityService activityService;
    private final RegistrationService registrationService;

    public AdminController(ActivityService activityService, RegistrationService registrationService) {
        this.activityService = activityService;
        this.registrationService = registrationService;
    }

    @GetMapping("/test")
    public Result<String> test() {
        return Result.success("管理员接口访问成功");
    }

    @PostMapping("/activity")
    public Result<ActivityVO> createActivity(@RequestBody @Valid CreateActivityDTO createActivityDTO) {
        return Result.success(activityService.createActivity(createActivityDTO));
    }

    @PutMapping("/activity/{activityId}")
    public Result<ActivityVO> updateActivity(@PathVariable Long activityId,
                                             @RequestBody @Valid UpdateActivityDTO updateActivityDTO) {
        return Result.success(activityService.updateActivity(activityId, updateActivityDTO));
    }

    @PutMapping("/activity/{activityId}/offline")
    public Result<ActivityVO> offlineActivity(@PathVariable Long activityId) {
        return Result.success(activityService.offlineActivity(activityId));
    }

    @GetMapping("/activity/registration-stats")
    public Result<PageVO<ActivityRegistrationStatsVO>> listActivityRegistrationStats(
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize,
            @RequestParam(required = false) String keyword) {
        return Result.success(activityService.listActivityRegistrationStats(pageNum, pageSize, keyword));
    }

    @GetMapping("/activity/popular-ranking")
    public Result<List<ActivityRegistrationStatsVO>> listPopularActivityRanking(
            @RequestParam(required = false) Integer limit) {
        return Result.success(activityService.listPopularActivityRanking(limit));
    }

    @GetMapping("/registration/list")
    public Result<PageVO<AdminRegistrationVO>> listRegistrations(@RequestParam(defaultValue = "1") Long pageNum,
                                                                  @RequestParam(defaultValue = "10") Long pageSize,
                                                                  @RequestParam(required = false) String status,
                                                                  @RequestParam(required = false) Long activityId,
                                                                  @RequestParam(required = false) String keyword) {
        return Result.success(registrationService.listAdminRegistrations(
                pageNum,
                pageSize,
                status,
                activityId,
                keyword
        ));
    }

    @PutMapping("/registration/{registrationId}/approve")
    public Result<RegistrationVO> approveRegistration(@PathVariable Long registrationId) {
        return Result.success(registrationService.approveRegistration(registrationId));
    }

    @PutMapping("/registration/{registrationId}/reject")
    public Result<RegistrationVO> rejectRegistration(@PathVariable Long registrationId) {
        return Result.success(registrationService.rejectRegistration(registrationId));
    }
}
