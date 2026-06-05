package com.example.trekkingapp.tracking;

import java.time.LocalDateTime;

public record TrackingSessionResponse(
        Long sessionId,
        Long userId,
        Long tourId,
        Long routeId,
        Long bookingId,
        String status,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        Double totalDistanceKm,
        Double lastLatitude,
        Double lastLongitude,
        LocalDateTime lastLocationAt
) {
}
