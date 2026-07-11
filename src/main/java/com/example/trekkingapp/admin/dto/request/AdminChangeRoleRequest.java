package com.example.trekkingapp.admin.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AdminChangeRoleRequest(
        @NotBlank(message = "Role is required")
        String role
) {
}
