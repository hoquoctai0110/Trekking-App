package com.example.trekkingapp.auth;

import com.example.trekkingapp.user.UserResponse;

public record GoogleLoginResponse(
        String accessToken,
        UserResponse user
) {
}
