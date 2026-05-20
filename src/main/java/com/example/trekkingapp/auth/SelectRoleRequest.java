package com.example.trekkingapp.auth;

import jakarta.validation.constraints.NotBlank;

public record SelectRoleRequest(
        @NotBlank(message = "role is required")
        String role
) {
}
