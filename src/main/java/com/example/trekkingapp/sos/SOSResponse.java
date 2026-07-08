package com.example.trekkingapp.sos;

import java.time.LocalDateTime;

public record SOSResponse(
        Long sosId,
        Long bookingId,
        Long tourId,
        String tourTitle,
        Long userId,
        String userName,
        String userEmail,
        Double latitude,
        Double longitude,
        String message,
        SOSAlertStatus status,
        SOSAlertSource source,
        LocalDateTime clientCreatedAt,
        LocalDateTime createdAt,
        LocalDateTime acknowledgedAt,
        LocalDateTime resolvedAt
) {
}
