package com.example.activitybookingsystem.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PointProductDTO {

    @NotBlank(message = "商品名称不能为空")
    @Size(max = 100, message = "商品名称不能超过100个字符")
    private String name;

    @Size(max = 500, message = "商品描述不能超过500个字符")
    private String description;

    @NotNull(message = "兑换积分不能为空")
    @Min(value = 1, message = "兑换积分必须大于0")
    private Integer pointsRequired;

    @NotNull(message = "库存不能为空")
    @Min(value = -1, message = "库存不能小于-1")
    private Integer stock;
}