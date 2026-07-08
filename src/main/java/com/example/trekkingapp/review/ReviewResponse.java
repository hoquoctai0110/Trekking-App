package com.example.trekkingapp.review;

import java.time.LocalDateTime;

public record ReviewResponse(
        Long reviewId,
        Long bookingId,
        Long tourId,
        Long userId,
        String userName,
        Integer rating,
        String comment,
        LocalDateTime createdAt
) {
}
