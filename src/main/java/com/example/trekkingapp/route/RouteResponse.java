package com.example.trekkingapp.route;

import java.time.LocalDateTime;

public record RouteResponse(
        Long routeId,
        String routeName,
        String polylineData,
        Double distanceKm,
        Integer estimatedDurationMin,
        String difficulty,
        String status,
        Long createdBy,
        String createdType,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
