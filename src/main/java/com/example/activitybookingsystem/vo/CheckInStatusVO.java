package com.example.activitybookingsystem.vo;

import lombok.Data;

@Data
public class CheckInStatusVO {

    /** 当前用户对该活动的报名状态：PENDING/APPROVED/REJECTED/CANCELED，未报名为 null */
    private String registrationStatus;

    /** 当前用户对该活动的打卡记录，未打卡为 null */
    private CheckInVO checkIn;
}