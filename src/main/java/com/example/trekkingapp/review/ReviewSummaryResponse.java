package com.example.trekkingapp.review;

public record ReviewSummaryResponse(
        Long tourId,
        Double averageRating,
        Long reviewCount
) {
}
