package com.example.trekkingapp.sos;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record SOSRequest(
        @NotNull(message = "Booking ID is required")
        Long bookingId,

        Long trackingSessionId,

        @NotNull(message = "Latitude is required")
        @DecimalMin(value = "-90.0", message = "Invalid latitude")
        @DecimalMax(value = "90.0", message = "Invalid latitude")
        Double latitude,

        @NotNull(message = "Longitude is required")
        @DecimalMin(value = "-180.0", message = "Invalid longitude")
        @DecimalMax(value = "180.0", message = "Invalid longitude")
        Double longitude,

        @Size(max = 2000, message = "Message must be at most 2000 characters")
        String message,

        SOSAlertSource source,

        LocalDateTime clientCreatedAt,

        @Size(max = 100, message = "Client request ID must be at most 100 characters")
        String clientRequestId
) {
}
