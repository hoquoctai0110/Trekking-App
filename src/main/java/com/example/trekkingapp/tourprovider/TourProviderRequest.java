package com.example.trekkingapp.tourprovider;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record TourProviderRequest(
        String companyName,
        String description,
        String businessLicenseUrl,

        @NotBlank(message = "phone is required")
        String phone,

        @NotBlank(message = "email is required")
        @Email(message = "email must be valid")
        String email,

        String address,
        String status
) {
}
