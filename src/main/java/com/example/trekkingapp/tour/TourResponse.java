package com.example.trekkingapp.tour;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TourResponse(
        Long tourId,
        Long providerId,
        String providerName,
        Long routeId,
        String routeName,
        String title,
        String description,
        BigDecimal price,
        Integer maxParticipants,
        String difficulty,
        String duration,
        String meetingPoint,
        String status,
        LocalDateTime startDate,
        LocalDateTime endDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
