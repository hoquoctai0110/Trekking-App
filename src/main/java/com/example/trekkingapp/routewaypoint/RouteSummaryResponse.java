package com.example.trekkingapp.routewaypoint;

public record RouteSummaryResponse(
        Long routeId,
        String routeName,
        long totalWaypoints,
        long totalCampPoints,
        long totalWaterPoints,
        long totalDangerPoints,
        RouteWaypointResponse startWaypoint,
        RouteWaypointResponse endWaypoint,
        Integer estimatedDurationMin,
        Double distanceKm,
        Double elevationGain
) {
}
