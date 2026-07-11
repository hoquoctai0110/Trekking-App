package com.example.trekkingapp.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public record AdminNotificationCreateRequest(
        @NotBlank(message = "Title is required")
        String title,
        @NotBlank(message = "Body is required")
        String body,
        @NotBlank(message = "Type is required")
        String type,
        @NotBlank(message = "Recipient type is required")
        String recipientType,
        @NotNull(message = "Recipient ids must be provided")
        List<Long> recipientIds,
        LocalDateTime scheduledAt,
        String idempotencyKey
) {
}
