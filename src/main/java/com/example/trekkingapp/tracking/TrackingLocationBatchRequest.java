package com.example.trekkingapp.tracking;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record TrackingLocationBatchRequest(
        @NotNull(message = "trackingSessionId is required")
        Long trackingSessionId,

        @NotEmpty(message = "points is required")
        List<@Valid TrackingLocationPointRequest> points
) {
}
