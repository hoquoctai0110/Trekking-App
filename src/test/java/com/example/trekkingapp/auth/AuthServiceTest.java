package com.example.trekkingapp.auth;

import com.example.trekkingapp.role.Role;
import com.example.trekkingapp.role.RoleRepository;
import com.example.trekkingapp.role.UserRoleRepository;
import com.example.trekkingapp.tourprovider.TourProviderRepository;
import com.example.trekkingapp.trekkerprofile.TrekkerProfileService;
import com.example.trekkingapp.user.User;
import com.example.trekkingapp.user.UserRepository;
import com.example.trekkingapp.user.UserResponse;
import com.example.trekkingapp.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private GoogleTokenVerifierService googleTokenVerifierService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private UserService userService;

    @Mock
    private JwtService jwtService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private TrekkerProfileService trekkerProfileService;

    @Mock
    private TourProviderRepository tourProviderRepository;

    @Mock
    private AuthOtpTokenRepository authOtpTokenRepository;

    @Mock
    private OtpNotificationService otpNotificationService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                googleTokenVerifierService,
                userRepository,
                roleRepository,
                userRoleRepository,
                userService,
                jwtService,
                currentUserService,
                passwordEncoder,
                trekkerProfileService,
                tourProviderRepository,
                authOtpTokenRepository,
                otpNotificationService
        );
    }

    @Test
    void validOtpActivatesUserAndReturnsJwt() {
        User user = createLocalUser("PENDING_VERIFICATION", "secret");
        AuthOtpToken token = createToken(user, AuthOtpPurpose.REGISTER_VERIFY, "123456", 0);

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(authOtpTokenRepository.save(any(AuthOtpToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userService.toResponse(any(User.class))).thenAnswer(invocation -> toResponse(invocation.getArgument(0)));
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(authOtpTokenRepository.findFirstByUserAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(user, AuthOtpPurpose.REGISTER_VERIFY))
                .thenReturn(Optional.of(token));
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        AuthResponse response = authService.verifyOtp(new VerifyOtpRequest(user.getEmail(), AuthOtpPurpose.REGISTER_VERIFY, "123456"));

        assertEquals("jwt-token", response.accessToken());
        assertEquals("ACTIVE", user.getStatus());
        assertNotNull(token.getUsedAt());
        verify(userRepository).save(user);
        verify(authOtpTokenRepository).save(token);
    }

    @Test
    void wrongOtpIncrementsAttemptCount() {
        User user = createLocalUser("PENDING_VERIFICATION", "secret");
        AuthOtpToken token = createToken(user, AuthOtpPurpose.REGISTER_VERIFY, "123456", 0);

        when(authOtpTokenRepository.save(any(AuthOtpToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(authOtpTokenRepository.findFirstByUserAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(user, AuthOtpPurpose.REGISTER_VERIFY))
                .thenReturn(Optional.of(token));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.verifyOtp(new VerifyOtpRequest(user.getEmail(), AuthOtpPurpose.REGISTER_VERIFY, "000000"))
        );

        assertEquals("OTP is incorrect", exception.getMessage());
        assertEquals(1, token.getAttemptCount());
        assertNull(token.getUsedAt());
    }

    @Test
    void expiredOtpIsRejected() {
        User user = createLocalUser("PENDING_VERIFICATION", "secret");
        AuthOtpToken token = createToken(user, AuthOtpPurpose.REGISTER_VERIFY, "123456", 0);
        token.setExpiresAt(java.time.LocalDateTime.now().minusSeconds(1));

        when(authOtpTokenRepository.save(any(AuthOtpToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(authOtpTokenRepository.findFirstByUserAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(user, AuthOtpPurpose.REGISTER_VERIFY))
                .thenReturn(Optional.of(token));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.verifyOtp(new VerifyOtpRequest(user.getEmail(), AuthOtpPurpose.REGISTER_VERIFY, "123456"))
        );

        assertEquals("OTP has expired", exception.getMessage());
        assertNotNull(token.getUsedAt());
    }

    @Test
    void reusedOtpIsRejected() {
        User user = createLocalUser("PENDING_VERIFICATION", "secret");

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(authOtpTokenRepository.findFirstByUserAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(user, AuthOtpPurpose.REGISTER_VERIFY))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.verifyOtp(new VerifyOtpRequest(user.getEmail(), AuthOtpPurpose.REGISTER_VERIFY, "123456"))
        );

        assertEquals("OTP is invalid or already used", exception.getMessage());
    }

    @Test
    void resendCooldownIsEnforced() {
        User user = createLocalUser("PENDING_VERIFICATION", "secret");
        AuthOtpToken token = createToken(user, AuthOtpPurpose.REGISTER_VERIFY, "123456", 0);
        token.setCreatedAt(java.time.LocalDateTime.now().minusSeconds(10));

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(authOtpTokenRepository.findFirstByUserAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(user, AuthOtpPurpose.REGISTER_VERIFY))
                .thenReturn(Optional.of(token));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.resendOtp(new ResendOtpRequest(user.getEmail(), AuthOtpPurpose.REGISTER_VERIFY))
        );

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exception.getStatusCode());
        assertTrue(exception.getReason().contains("cooldown"));
    }

    @Test
    void resetPasswordSucceedsWithValidOtp() {
        User user = createLocalUser("ACTIVE", "old-password");
        AuthOtpToken token = createToken(user, AuthOtpPurpose.PASSWORD_RESET, "654321", 0);

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(authOtpTokenRepository.save(any(AuthOtpToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(authOtpTokenRepository.findFirstByUserAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(user, AuthOtpPurpose.PASSWORD_RESET))
                .thenReturn(Optional.of(token));

        authService.resetPassword(new ResetPasswordRequest(user.getEmail(), "654321", "new-password", "new-password"));

        assertTrue(passwordEncoder.matches("new-password", user.getPasswordHash()));
        assertNotNull(token.getUsedAt());
        verify(userRepository).save(user);
    }

    @Test
    void loginBeforeVerificationIsRejected() {
        User user = createLocalUser("PENDING_VERIFICATION", "secret");

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.login(new LoginRequest(user.getEmail(), "secret"))
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("Email verification required before login", exception.getReason());
        verify(jwtService, never()).generateToken(any(User.class));
    }

    @Test
    void registerTrekkerSendsOtpAndKeepsUserUnverified() {
        RegisterTrekkerRequest request = new RegisterTrekkerRequest(
                "Alice Trekker",
                java.time.LocalDate.of(1998, 5, 10),
                "secret123",
                "secret123",
                "alice@example.com",
                "0812345678",
                "BEGINNER",
                "citizen-id.jpg"
        );
        Role role = new Role();
        role.setRoleName("TREKKER");

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(userRepository.existsByPhone("0812345678")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(roleRepository.findByRoleName("TREKKER")).thenReturn(Optional.of(role));
        when(userRoleRepository.existsByUserAndRole(any(User.class), eq(role))).thenReturn(false);
        when(authOtpTokenRepository.findFirstByUserAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(any(User.class), eq(AuthOtpPurpose.REGISTER_VERIFY)))
                .thenReturn(Optional.empty());
        when(authOtpTokenRepository.save(any(AuthOtpToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OtpChallengeResponse response = authService.registerTrekker(request);

        assertEquals("alice@example.com", response.email());
        assertEquals(AuthOtpPurpose.REGISTER_VERIFY, response.purpose());
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("PENDING_VERIFICATION", userCaptor.getValue().getStatus());
        verify(otpNotificationService).sendOtpEmail(eq("alice@example.com"), any(String.class), eq(AuthOtpPurpose.REGISTER_VERIFY), any());
        verify(trekkerProfileService).createProfile(any(User.class), eq("BEGINNER"), eq("citizen-id.jpg"));
    }

    @Test
    void registerRejectsDuplicatePhone() {
        RegisterTrekkerRequest request = new RegisterTrekkerRequest(
                "Alice Trekker",
                java.time.LocalDate.of(1998, 5, 10),
                "secret123",
                "secret123",
                "alice@example.com",
                "0812345678",
                "BEGINNER",
                "citizen-id.jpg"
        );

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(userRepository.existsByPhone("0812345678")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.registerTrekker(request)
        );

        assertEquals("Phone already exists", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void forgotPasswordPropagatesEmailDeliveryFailure() {
        User user = createLocalUser("ACTIVE", "secret");
        EmailDeliveryException exceptionToThrow = new EmailDeliveryException("Failed to send OTP email. Please try again later.");

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(authOtpTokenRepository.findFirstByUserAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(user, AuthOtpPurpose.PASSWORD_RESET))
                .thenReturn(Optional.empty());
        when(authOtpTokenRepository.save(any(AuthOtpToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        org.mockito.Mockito.doThrow(exceptionToThrow)
                .when(otpNotificationService)
                .sendOtpEmail(eq(user.getEmail()), any(String.class), eq(AuthOtpPurpose.PASSWORD_RESET), any());

        EmailDeliveryException thrown = assertThrows(
                EmailDeliveryException.class,
                () -> authService.forgotPassword(new ForgotPasswordRequest(user.getEmail()))
        );

        assertSame(exceptionToThrow, thrown);
    }

    private User createLocalUser(String status, String rawPassword) {
        User user = new User();
        user.setUserId(1L);
        user.setEmail("user@example.com");
        user.setAuthProvider("LOCAL");
        user.setStatus(status);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        return user;
    }

    private AuthOtpToken createToken(User user, AuthOtpPurpose purpose, String rawOtp, int resendCount) {
        AuthOtpToken token = new AuthOtpToken();
        token.setId(100L);
        token.setUser(user);
        token.setPurpose(purpose);
        token.setTokenHash(passwordEncoder.encode(rawOtp));
        token.setCreatedAt(java.time.LocalDateTime.now().minusMinutes(1));
        token.setExpiresAt(java.time.LocalDateTime.now().plusMinutes(5));
        token.setAttemptCount(0);
        token.setResendCount(resendCount);
        return token;
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getUserId(),
                user.getEmail(),
                user.getFullName(),
                user.getAvatarUrl(),
                user.getPhone(),
                user.getDateOfBirth(),
                user.getAuthProvider(),
                user.getStatus(),
                user.getRoleSelected(),
                java.util.List.of()
        );
    }

}
