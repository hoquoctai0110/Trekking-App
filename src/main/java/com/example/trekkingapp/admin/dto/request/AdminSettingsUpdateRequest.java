package com.example.trekkingapp.admin.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record AdminSettingsUpdateRequest(
        @NotNull(message = "Values are required")
        Map<String, String> values
) {
}
