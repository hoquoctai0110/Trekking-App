package com.example.trekkingapp.routewaypoint;

import java.time.LocalDateTime;

public record RouteWaypointResponse(
        Long waypointId,
        Long routeId,
        String name,
        String description,
        Double latitude,
        Double longitude,
        String category,
        Integer orderIndex,
        Double elevation,
        Double distanceFromStartKm,
        Integer estimatedArrivalMinute,
        Boolean mandatory,
        String iconKey,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
