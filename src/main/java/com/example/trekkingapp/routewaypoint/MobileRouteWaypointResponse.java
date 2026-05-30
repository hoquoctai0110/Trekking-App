package com.example.trekkingapp.routewaypoint;

public record MobileRouteWaypointResponse(
        Long waypointId,
        String name,
        Double latitude,
        Double longitude,
        String category,
        String iconKey,
        Integer orderIndex
) {
}
