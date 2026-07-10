package com.example.trekkingapp.tour;

import java.time.LocalDateTime;

public record TourImageResponse(
        Long imageId,
        String imageUrl,
        Integer displayOrder,
        boolean isCover,
        LocalDateTime createdAt
) {
}
