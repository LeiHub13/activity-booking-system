package com.example.activitybookingsystem.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor // 无参和有参构造方法
public class LoginVO {
    private String token;
}
