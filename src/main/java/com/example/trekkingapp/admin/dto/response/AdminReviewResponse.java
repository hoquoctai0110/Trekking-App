package com.example.trekkingapp.admin.dto.response;

import java.time.LocalDateTime;

public record AdminReviewResponse(
        Long id,
        String user,
        String tour,
        String tourProvider,
        Integer rating,
        String content,
        Boolean visible,
        Boolean flagged,
        LocalDateTime createdAt
) {
}
