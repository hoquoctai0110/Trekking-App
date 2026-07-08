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
        String routeType,
        Long createdBy,
        String createdType,
        Double startLatitude,
        Double startLongitude,
        Double endLatitude,
        Double endLongitude,
        Double elevationGain,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
