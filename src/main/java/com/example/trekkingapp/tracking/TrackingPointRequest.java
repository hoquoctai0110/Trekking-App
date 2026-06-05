package com.example.trekkingapp.tracking;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record TrackingPointRequest(
        @NotNull(message = "latitude is required")
        Double latitude,

        @NotNull(message = "longitude is required")
        Double longitude,

        Double altitude,
        Double speed,
        Double accuracy,
        LocalDateTime recordedAt
) {
}
