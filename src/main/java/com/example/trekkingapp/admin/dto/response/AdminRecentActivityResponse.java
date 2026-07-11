package com.example.trekkingapp.admin.dto.response;

import java.time.LocalDateTime;

public record AdminRecentActivityResponse(
        Long id,
        String action,
        String entityType,
        String entityId,
        String reason,
        LocalDateTime createdAt
) {
}
