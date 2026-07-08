package com.example.trekkingapp.trekkerprofile;

import jakarta.validation.constraints.NotBlank;

public record TrekkerProfileRequest(
        @NotBlank(message = "trekkingExperience is required")
        String trekkingExperience,
        String citizenIdImageUrl
) {
}
