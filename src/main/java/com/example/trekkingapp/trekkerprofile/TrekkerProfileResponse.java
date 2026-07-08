package com.example.trekkingapp.trekkerprofile;

import java.time.LocalDateTime;

public record TrekkerProfileResponse(
        Long profileId,
        Long userId,
        String trekkingExperience,
        String citizenIdImageUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
