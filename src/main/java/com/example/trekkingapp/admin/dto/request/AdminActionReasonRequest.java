package com.example.trekkingapp.admin.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AdminActionReasonRequest(
        @NotBlank(message = "Reason is required")
        String reason
) {
}
