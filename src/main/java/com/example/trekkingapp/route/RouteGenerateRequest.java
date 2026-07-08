package com.example.trekkingapp.route;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record RouteGenerateRequest(
        @NotBlank(message = "routeName is required")
        String routeName,

        @NotNull(message = "start is required")
        @Valid
        RoutePointRequest start,

        @Valid
        RoutePointRequest end,

        List<@Valid RoutePointRequest> checkpoints,

        String routeType,

        String difficulty
) {
}
