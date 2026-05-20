package com.example.trekkingapp.auth;

import org.springframework.stereotype.Service;

@Service
public class GoogleTokenVerifierService {

    public GoogleUserInfo verify(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new IllegalArgumentException("idToken is required");
        }

        return new GoogleUserInfo(
                "google123",
                "test@gmail.com",
                "Test User",
                "https://avatar.url"
        );
    }
}
