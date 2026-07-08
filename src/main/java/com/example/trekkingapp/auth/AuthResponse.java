package com.example.trekkingapp.auth;

import com.example.trekkingapp.user.UserResponse;

public record AuthResponse(
        String accessToken,
        UserResponse user
) {
}
