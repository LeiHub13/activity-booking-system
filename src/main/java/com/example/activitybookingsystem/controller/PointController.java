package com.example.activitybookingsystem.controller;

import com.example.activitybookingsystem.common.result.Result;
import com.example.activitybookingsystem.dto.CreateRatingDTO;
import com.example.activitybookingsystem.service.PointService;
import com.example.activitybookingsystem.service.RatingService;
import com.example.activitybookingsystem.vo.ActivityRatingVO;
import com.example.activitybookingsystem.vo.PageVO;
import com.example.activitybookingsystem.vo.PointAccountVO;
import com.example.activitybookingsystem.vo.PointExchangeVO;
import com.example.activitybookingsystem.vo.PointProductVO;
import com.example.activitybookingsystem.vo.PointRecordVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/points")
public class PointController {

    private final PointService pointService;
    private final RatingService ratingService;

    public PointController(PointService pointService, RatingService ratingService) {
        this.pointService = pointService;
        this.ratingService = ratingService;
    }

    @GetMapping("/account")
    public Result<PointAccountVO> getMyAccount() {
        return Result.success(pointService.getMyAccount());
    }

    @GetMapping("/records")
    public Result<PageVO<PointRecordVO>> listMyRecords(@RequestParam(defaultValue = "1") Long pageNum,
                                                       @RequestParam(defaultValue = "10") Long pageSize) {
        return Result.success(pointService.listMyRecords(pageNum, pageSize));
    }

    @GetMapping("/products")
    public Result<PageVO<PointProductVO>> listProducts(@RequestParam(defaultValue = "1") Long pageNum,
                                                       @RequestParam(defaultValue = "10") Long pageSize) {
        return Result.success(pointService.listProducts(pageNum, pageSize));
    }

    @PostMapping("/products/{productId}/exchange")
    public Result<PointExchangeVO> exchangeProduct(@PathVariable Long productId) {
        return Result.success(pointService.exchangeProduct(productId));
    }

    @GetMapping("/exchanges")
    public Result<PageVO<PointExchangeVO>> listMyExchanges(@RequestParam(defaultValue = "1") Long pageNum,
                                                           @RequestParam(defaultValue = "10") Long pageSize) {
        return Result.success(pointService.listMyExchanges(pageNum, pageSize));
    }

    @PostMapping("/ratings")
    public Result<ActivityRatingVO> createRating(@Valid @RequestBody CreateRatingDTO dto) {
        return Result.success(ratingService.createRating(dto));
    }

    @GetMapping("/ratings/my")
    public Result<PageVO<ActivityRatingVO>> listMyRatings(@RequestParam(defaultValue = "1") Long pageNum,
                                                          @RequestParam(defaultValue = "10") Long pageSize) {
        return Result.success(ratingService.listMyRatings(pageNum, pageSize));
    }

    @GetMapping("/ratings/activity")
    public Result<PageVO<ActivityRatingVO>> listActivityRatings(@RequestParam Long activityId,
                                                                @RequestParam(defaultValue = "1") Long pageNum,
                                                                @RequestParam(defaultValue = "10") Long pageSize) {
        return Result.success(ratingService.listActivityRatings(activityId, pageNum, pageSize));
    }
}