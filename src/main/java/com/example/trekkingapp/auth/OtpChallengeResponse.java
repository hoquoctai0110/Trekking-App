package com.example.trekkingapp.auth;

import java.time.LocalDateTime;

public record OtpChallengeResponse(
        String email,
        AuthOtpPurpose purpose,
        LocalDateTime expiresAt,
        LocalDateTime resendAvailableAt
) {
}
