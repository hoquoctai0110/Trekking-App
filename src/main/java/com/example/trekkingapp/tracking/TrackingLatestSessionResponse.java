package com.example.trekkingapp.tracking;

import java.time.LocalDateTime;

public record TrackingLatestSessionResponse(
        Long trackingSessionId,
        Long bookingId,
        String direction,
        String status,
        LocalDateTime startedAt,
        LocalDateTime endedAt
) {
}
