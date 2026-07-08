package com.example.trekkingapp.tourprovider;

import java.time.LocalDateTime;

public record TourProviderResponse(
        Long providerId,
        Long userId,
        String userEmail,
        String companyName,
        String description,
        String businessLicenseUrl,
        String citizenIdImageUrl,
        String phone,
        String email,
        String address,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
