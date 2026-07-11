package com.example.trekkingapp.admin.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminTourProviderResponse(
        Long id,
        Long userId,
        String name,
        String email,
        String phone,
        String avatarUrl,
        Integer experienceYears,
        Double ratingAverage,
        long completedTours,
        String status,
        String certifications,
        LocalDateTime joinedAt,
        BigDecimal earnings
) {
}
