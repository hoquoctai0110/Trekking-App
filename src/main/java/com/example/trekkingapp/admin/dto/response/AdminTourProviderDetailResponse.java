package com.example.trekkingapp.admin.dto.response;

import java.time.LocalDateTime;

public record AdminTourProviderDetailResponse(
        Long id,
        Long userId,
        String name,
        String email,
        String phone,
        String avatarUrl,
        String companyName,
        String description,
        String businessLicenseUrl,
        String citizenIdImageUrl,
        String address,
        String status,
        LocalDateTime joinedAt,
        LocalDateTime updatedAt,
        Double ratingAverage,
        long completedTours,
        long totalTours
) {
}
