package com.example.trekkingapp.tracking;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record TrackingLocationPointRequest(
        @NotNull(message = "latitude is required")
        Double latitude,

        @NotNull(message = "longitude is required")
        Double longitude,

        Double accuracy,
        Double altitude,
        Double speed,
        LocalDateTime recordedAt
) {
}
