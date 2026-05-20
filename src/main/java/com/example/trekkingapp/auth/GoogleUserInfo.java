package com.example.trekkingapp.auth;

public record GoogleUserInfo(
        String googleId,
        String email,
        String fullName,
        String avatarUrl
) {
}
