package com.example.trekkingapp.routewaypoint;

import java.time.LocalDateTime;

public record RouteWaypointResponse(
        Long waypointId,
        Long routeId,
        String name,
        Double latitude,
        Double longitude,
        String type,
        String description,
        Integer orderIndex,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
