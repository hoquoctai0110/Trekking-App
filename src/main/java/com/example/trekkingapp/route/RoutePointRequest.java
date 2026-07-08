package com.example.trekkingapp.route;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RoutePointRequest(
        @NotNull(message = "latitude is required")
        Double latitude,

        @NotNull(message = "longitude is required")
        Double longitude,

        @NotBlank(message = "name is required")
        String name
) {
}
