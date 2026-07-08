package com.example.trekkingapp.tracking;

public record TrackingStartResponse(
        Long trackingSessionId,
        String direction
) {
}
