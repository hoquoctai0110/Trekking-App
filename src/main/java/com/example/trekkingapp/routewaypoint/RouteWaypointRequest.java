package com.example.trekkingapp.routewaypoint;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RouteWaypointRequest(
        @NotBlank(message = "name is required")
        String name,

        @NotNull(message = "latitude is required")
        Double latitude,

        @NotNull(message = "longitude is required")
        Double longitude,

        String type,
        String description,

        @NotNull(message = "orderIndex is required")
        @Min(value = 0, message = "orderIndex must be greater than or equal to 0")
        Integer orderIndex
) {
}
