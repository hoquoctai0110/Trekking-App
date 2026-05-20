package com.example.trekkingapp.auth;

import com.example.trekkingapp.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String jwtSecret,
            @Value("${app.jwt.expiration-ms}") long expirationMs
    ) {
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(User user) {
        Date now = new Date();
        Date expiresAt = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(String.valueOf(user.getUserId()))
                .claim("email", user.getEmail())
                .claim("roles", getRoleNames(user))
                .issuedAt(now)
                .expiration(expiresAt)
                .signWith(signingKey)
                .compact();
    }

    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    public String extractSubject(String token) {
        return parseClaims(token).getSubject();
    }

    public Long extractUserId(String token) {
        return Long.valueOf(extractSubject(token));
    }

    public List<String> extractRoles(String token) {
        Object roles = parseClaims(token).get("roles");
        if (!(roles instanceof List<?> roleList)) {
            return List.of();
        }

        return roleList.stream()
                .map(String::valueOf)
                .toList();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private List<String> getRoleNames(User user) {
        return user.getUserRoles()
                .stream()
                .map(userRole -> userRole.getRole().getRoleName())
                .toList();
    }
}
