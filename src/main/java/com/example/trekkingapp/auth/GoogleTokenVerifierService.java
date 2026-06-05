package com.example.trekkingapp.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Set;

@Service
public class GoogleTokenVerifierService {

    private static final String INVALID_GOOGLE_ID_TOKEN_MESSAGE = "Invalid Google ID token";
    private static final String MISSING_CLIENT_IDS_MESSAGE =
            "Google client IDs are missing. Configure app.google.client-id or app.google.client-ids";

    private final GoogleProperties googleProperties;

    public GoogleTokenVerifierService(GoogleProperties googleProperties) {
        this.googleProperties = googleProperties;
    }

    public GoogleUserInfo verify(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new IllegalArgumentException("idToken is required");
        }

        if (googleProperties.isMockEnabled()) {
            return verifyMockToken(idToken);
        }

        Set<String> clientIds = googleProperties.getResolvedClientIds();
        if (clientIds.isEmpty()) {
            throw new IllegalStateException(MISSING_CLIENT_IDS_MESSAGE);
        }

        GoogleIdToken googleIdToken = verifyGoogleIdToken(idToken, clientIds);
        GoogleIdToken.Payload payload = googleIdToken.getPayload();
        String email = payload.getEmail();
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Google account email is missing");
        }

        return new GoogleUserInfo(
                payload.getSubject(),
                email,
                getStringClaim(payload, "name"),
                getStringClaim(payload, "picture")
        );
    }

    private GoogleUserInfo verifyMockToken(String idToken) {
        if (!"fake_token".equals(idToken)) {
            throw new IllegalArgumentException(INVALID_GOOGLE_ID_TOKEN_MESSAGE);
        }

        return new GoogleUserInfo(
                "google_mock_001",
                "test@gmail.com",
                "Test User",
                "https://avatar.url"
        );
    }

    private GoogleIdToken verifyGoogleIdToken(String idToken, Set<String> clientIds) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance()
            )
                    .setAudience(clientIds)
                    .build();

            GoogleIdToken googleIdToken = verifier.verify(idToken);
            if (googleIdToken == null) {
                throw new IllegalArgumentException(INVALID_GOOGLE_ID_TOKEN_MESSAGE);
            }

            return googleIdToken;
        } catch (GeneralSecurityException | IOException exception) {
            throw new IllegalArgumentException(INVALID_GOOGLE_ID_TOKEN_MESSAGE, exception);
        }
    }

    private String getStringClaim(GoogleIdToken.Payload payload, String claimName) {
        Object value = payload.get(claimName);
        return value == null ? null : value.toString();
    }
}
