package com.example.trekkingapp.offlineroute;

import com.example.trekkingapp.routewaypoint.RouteWaypointResponse;

import java.time.LocalDateTime;
import java.util.List;

public record OfflineRouteResponse(
        Long offlineRouteId,
        Long routeId,
        String routeName,
        String polylineData,
        Double distanceKm,
        Integer estimatedDurationMin,
        String difficulty,
        Double startLatitude,
        Double startLongitude,
        Double endLatitude,
        Double endLongitude,
        Double elevationGain,
        List<RouteWaypointResponse> waypoints,
        LocalDateTime downloadedAt,
        LocalDateTime lastSyncedAt,
        String localVersion
) {
}
