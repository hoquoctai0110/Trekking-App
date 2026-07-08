package com.example.trekkingapp.auth;

import java.time.LocalDateTime;

public interface OtpNotificationService {

    void sendOtpEmail(String email, String otp, AuthOtpPurpose purpose, LocalDateTime expiresAt);
}
