package com.example.trekkingapp.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ResetPasswordRequest(
        @NotBlank(message = "email is required")
        @Email(message = "email must be valid")
        String email,

        @NotBlank(message = "otp is required")
        @Pattern(regexp = "\\d{6}", message = "otp must be 6 digits")
        String otp,

        @NotBlank(message = "newPassword is required")
        String newPassword,

        @NotBlank(message = "confirmPassword is required")
        String confirmPassword
) {
}
