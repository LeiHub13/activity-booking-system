package com.example.activitybookingsystem.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditNoticeMessage {

    private Long registrationId;
    private Long activityId;
    private Boolean approved;
}
