package com.example.activitybookingsystem.service;

import com.example.activitybookingsystem.dto.ResetPasswordDTO;
import com.example.activitybookingsystem.dto.SendPasswordResetCodeDTO;

public interface MailService {
   void sendPasswordResetCode(SendPasswordResetCodeDTO dto);
   void resetPassword(ResetPasswordDTO dto);
}
