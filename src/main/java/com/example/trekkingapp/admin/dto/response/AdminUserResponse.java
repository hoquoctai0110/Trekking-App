package com.example.trekkingapp.admin.dto.response;

import java.time.LocalDateTime;

public record AdminUserResponse(
        Long id,
        String name,
        String email,
        String phone,
        String avatarUrl,
        String role,
        String status,
        LocalDateTime joinedAt,
        LocalDateTime lastActiveAt,
        long totalBookings,
        long totalTours
) {
}
