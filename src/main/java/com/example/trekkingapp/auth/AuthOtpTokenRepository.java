package com.example.trekkingapp.auth;

import com.example.trekkingapp.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AuthOtpTokenRepository extends JpaRepository<AuthOtpToken, Long> {

    Optional<AuthOtpToken> findFirstByUserAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(User user, AuthOtpPurpose purpose);

    List<AuthOtpToken> findByUserAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(User user, AuthOtpPurpose purpose);
}
