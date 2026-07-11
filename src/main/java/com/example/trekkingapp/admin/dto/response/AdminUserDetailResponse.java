package com.example.trekkingapp.admin.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AdminUserDetailResponse(
        Long id,
        String name,
        String email,
        String phone,
        String avatarUrl,
        String status,
        String authProvider,
        Boolean roleSelected,
        LocalDate dateOfBirth,
        LocalDateTime joinedAt,
        LocalDateTime updatedAt,
        List<String> roles,
        long totalBookings,
        long totalTours,
        Long providerId
) {
}
