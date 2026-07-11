package com.example.trekkingapp.admin.dto.request;

import jakarta.validation.constraints.NotNull;

public record AdminReviewVisibilityRequest(
        @NotNull(message = "Visible is required")
        Boolean visible,
        String reason
) {
}
