package com.example.trekkingapp.admin.dto.response;

import java.time.LocalDateTime;

public record AdminGeneratedQrResponse(
        Long tokenId,
        Long tourId,
        Long scheduleId,
        String token,
        Integer version,
        LocalDateTime expiresAt
) {
}
