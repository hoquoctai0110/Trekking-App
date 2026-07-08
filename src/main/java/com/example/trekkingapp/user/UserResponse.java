package com.example.trekkingapp.user;

import java.time.LocalDate;
import java.util.List;

public record UserResponse(
        Long userId,
        String email,
        String fullName,
        String avatarUrl,
        String phone,
        LocalDate dateOfBirth,
        String authProvider,
        String status,
        Boolean roleSelected,
        List<String> roles
) {
}
