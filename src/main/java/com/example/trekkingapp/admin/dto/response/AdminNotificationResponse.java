package com.example.trekkingapp.admin.dto.response;

import java.time.LocalDateTime;

public record AdminNotificationResponse(
        Long id,
        String title,
        String body,
        String type,
        String status,
        String recipientType,
        String recipientIds,
        LocalDateTime scheduledAt,
        LocalDateTime sentAt,
        LocalDateTime createdAt
) {
}
