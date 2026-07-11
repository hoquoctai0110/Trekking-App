package com.example.trekkingapp.admin.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record AdminCheckInQrRequest(
        @NotNull(message = "Tour id is required")
        Long tourId,
        Long scheduleId,
        @NotNull(message = "Expires at is required")
        @Future(message = "Expiry must be in the future")
        LocalDateTime expiresAt
) {
}
