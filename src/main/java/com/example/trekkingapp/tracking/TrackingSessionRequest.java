package com.example.trekkingapp.tracking;

import jakarta.validation.constraints.NotNull;

public record TrackingSessionRequest(
        @NotNull(message = "bookingId is required")
        Long bookingId,

        String direction
) {
}
