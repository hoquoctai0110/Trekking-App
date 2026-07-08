package com.example.trekkingapp.tracking;

import java.time.LocalDateTime;

public record TrackingPointResponse(
        Long trackingPointId,
        Long trackingSessionId,
        Double latitude,
        Double longitude,
        Double altitude,
        Double speed,
        Double accuracy,
        LocalDateTime recordedAt
) {
}
