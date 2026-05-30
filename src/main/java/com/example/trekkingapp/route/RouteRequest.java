package com.example.trekkingapp.route;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record RouteRequest(
        @NotBlank(message = "routeName is required")
        String routeName,

        @NotBlank(message = "polylineData is required")
        String polylineData,

        @PositiveOrZero(message = "distanceKm must be greater than or equal to 0")
        Double distanceKm,

        @PositiveOrZero(message = "estimatedDurationMin must be greater than or equal to 0")
        Integer estimatedDurationMin,

        String difficulty,
        String status,
        Long createdBy,
        String createdType,
        Double startLatitude,
        Double startLongitude,
        Double endLatitude,
        Double endLongitude,

        @PositiveOrZero(message = "elevationGain must be greater than or equal to 0")
        Double elevationGain
) {
}
