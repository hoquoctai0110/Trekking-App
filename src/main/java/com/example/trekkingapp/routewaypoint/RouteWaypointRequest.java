package com.example.trekkingapp.routewaypoint;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record RouteWaypointRequest(
        @NotBlank(message = "name is required")
        String name,

        String description,

        @NotNull(message = "latitude is required")
        Double latitude,

        @NotNull(message = "longitude is required")
        Double longitude,

        @NotBlank(message = "category is required")
        String category,

        @NotNull(message = "orderIndex is required")
        @Min(value = 0, message = "orderIndex must be greater than or equal to 0")
        Integer orderIndex,

        Double elevation,

        @PositiveOrZero(message = "distanceFromStartKm must be greater than or equal to 0")
        Double distanceFromStartKm,

        @PositiveOrZero(message = "estimatedArrivalMinute must be greater than or equal to 0")
        Integer estimatedArrivalMinute,

        Boolean mandatory,
        String iconKey
) {
}
