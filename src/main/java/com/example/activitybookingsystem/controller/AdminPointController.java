package com.example.activitybookingsystem.controller;

import com.example.activitybookingsystem.common.result.Result;
import com.example.activitybookingsystem.dto.PointProductDTO;
import com.example.activitybookingsystem.service.PointService;
import com.example.activitybookingsystem.vo.PageVO;
import com.example.activitybookingsystem.vo.PointExchangeVO;
import com.example.activitybookingsystem.vo.PointProductVO;
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
@RequestMapping("/api/admin/points")
public class AdminPointController {

    private final PointService pointService;

    public AdminPointController(PointService pointService) {
        this.pointService = pointService;
    }

    @GetMapping("/products")
    public Result<PageVO<PointProductVO>> listAllProducts(@RequestParam(defaultValue = "1") Long pageNum,
                                                          @RequestParam(defaultValue = "10") Long pageSize) {
        return Result.success(pointService.listAllProducts(pageNum, pageSize));
    }

    @PostMapping("/products")
    public Result<PointProductVO> createProduct(@Valid @RequestBody PointProductDTO dto) {
        return Result.success(pointService.createProduct(dto));
    }

    @PutMapping("/products/{productId}")
    public Result<PointProductVO> updateProduct(@PathVariable Long productId,
                                                @Valid @RequestBody PointProductDTO dto) {
        return Result.success(pointService.updateProduct(productId, dto));
    }

    @PutMapping("/products/{productId}/status")
    public Result<PointProductVO> toggleProductStatus(@PathVariable Long productId) {
        return Result.success(pointService.toggleProductStatus(productId));
    }

    @GetMapping("/exchanges")
    public Result<PageVO<PointExchangeVO>> listAllExchanges(@RequestParam(defaultValue = "1") Long pageNum,
                                                            @RequestParam(defaultValue = "10") Long pageSize) {
        return Result.success(pointService.listAllExchanges(pageNum, pageSize));
    }
}