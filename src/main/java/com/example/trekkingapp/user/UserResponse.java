package com.example.trekkingapp.user;

import java.util.List;

public record UserResponse(
        Long userId,
        String email,
        String fullName,
        String avatarUrl,
        String status,
        Boolean roleSelected,
        List<String> roles
) {
}
