package com.example.activitybookingsystem.controller;

import com.example.activitybookingsystem.common.result.Result;
import com.example.activitybookingsystem.dto.CreateRegistrationDTO;
import com.example.activitybookingsystem.service.RegistrationService;
import com.example.activitybookingsystem.vo.MyRegistrationVO;
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

@RestController
@RequestMapping("/api/registration")
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping
    public Result<RegistrationVO> registerActivity(@RequestBody @Valid CreateRegistrationDTO createRegistrationDTO) {
        return Result.success(registrationService.registerActivity(createRegistrationDTO));
    }

    @PutMapping("/{registrationId}/cancel")
    public Result<RegistrationVO> cancelRegistration(@PathVariable Long registrationId) {
        return Result.success(registrationService.cancelRegistration(registrationId));
    }

    @GetMapping("/my")
    public Result<PageVO<MyRegistrationVO>> listMyRegistrations(@RequestParam(defaultValue = "1") Long pageNum,
                                                                 @RequestParam(defaultValue = "5") Long pageSize,
                                                                 @RequestParam(defaultValue = "ALL") String category) {
        return Result.success(registrationService.listMyRegistrations(pageNum, pageSize, category));
    }
}
