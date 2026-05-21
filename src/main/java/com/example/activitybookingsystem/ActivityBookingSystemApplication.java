package com.example.activitybookingsystem;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.activitybookingsystem.mapper")
public class ActivityBookingSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(ActivityBookingSystemApplication.class, args);
    }

}
