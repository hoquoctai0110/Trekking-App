package com.example.trekkingapp.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record RegisterTrekkerRequest(
        @NotBlank(message = "fullName is required")
        String fullName,

        LocalDate dateOfBirth,

        @NotBlank(message = "password is required")
        String password,

        @NotBlank(message = "confirmPassword is required")
        String confirmPassword,

        @NotBlank(message = "email is required")
        @Email(message = "email must be valid")
        String email,

        @NotBlank(message = "phone is required")
        String phone,

        @NotBlank(message = "trekkingExperience is required")
        String trekkingExperience,

        String citizenIdImageUrl
) {
}
