package com.example.trekkingapp.tracking;

import java.time.LocalDateTime;

public record TrackingSessionResponse(
        Long trackingSessionId,
        Long userId,
        Long tourId,
        Long routeId,
        Long bookingId,
        String status,
        String direction,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        LocalDateTime pausedAt,
        Double totalDistanceKm,
        Double lastLatitude,
        Double lastLongitude,
        LocalDateTime lastLocationAt
) {
}
