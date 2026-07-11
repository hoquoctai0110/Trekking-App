package com.example.trekkingapp.admin.dto.request;

import jakarta.validation.constraints.NotNull;

public record AdminReviewFlagRequest(
        @NotNull(message = "Flagged is required")
        Boolean flagged,
        String reason
) {
}
