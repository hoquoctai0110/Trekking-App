package com.example.trekkingapp.auth;

import com.example.trekkingapp.common.ApiResponse;
import com.example.trekkingapp.common.RequestTracing;
import com.example.trekkingapp.user.UserResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/google")
    public ApiResponse<GoogleLoginResponse> loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        return new ApiResponse<>(true, "Google login successful", authService.loginWithGoogle(request));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return new ApiResponse<>(true, "Login successful", authService.login(request));
    }

    @PostMapping("/register/trekker")
    public ApiResponse<OtpChallengeResponse> registerTrekker(@Valid @RequestBody RegisterTrekkerRequest request) {
        String requestId = RequestTracing.getRequestId();
        log.info("REGISTER_REQUEST_RECEIVED endpoint=/api/v1/auth/register/trekker email={} role=TREKKER requestId={} startTimestamp={}",
                request.email(),
                requestId,
                java.time.Instant.now());
        return new ApiResponse<>(true, "Registration successful. Please verify OTP sent to your email.", authService.registerTrekker(request));
    }

    @PostMapping({"/register/tour-provider", "/register/provider"})
    public ApiResponse<OtpChallengeResponse> registerTourProvider(@Valid @RequestBody RegisterTourProviderRequest request) {
        String requestId = RequestTracing.getRequestId();
        log.info("REGISTER_REQUEST_RECEIVED endpoint=/api/v1/auth/register/provider email={} role=TOUR_PROVIDER requestId={} startTimestamp={}",
                request.email(),
                requestId,
                java.time.Instant.now());
        return new ApiResponse<>(true, "Registration successful. Please verify OTP sent to your email.", authService.registerTourProvider(request));
    }

    @PostMapping("/verify-otp")
    public ApiResponse<AuthResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return new ApiResponse<>(true, "OTP verified successfully", authService.verifyOtp(request));
    }

    @PostMapping("/resend-otp")
    public ApiResponse<OtpChallengeResponse> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        return new ApiResponse<>(true, "OTP resent successfully", authService.resendOtp(request));
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return new ApiResponse<>(true, "If the account exists, a password reset OTP has been sent", null);
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return new ApiResponse<>(true, "Password reset successful", null);
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> getCurrentUser() {
        return new ApiResponse<>(true, "Current user retrieved successfully", authService.getCurrentUser());
    }

    @PostMapping("/me/select-role")
    public ApiResponse<UserResponse> selectMyRole(@Valid @RequestBody SelectRoleRequest request) {
        return new ApiResponse<>(true, "Current user role selected successfully", authService.selectMyRole(request));
    }

    @PostMapping("/users/{userId}/select-role")
    public ResponseEntity<ApiResponse<UserResponse>> selectRole(
            @PathVariable Long userId,
            @Valid @RequestBody SelectRoleRequest request
    ) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Current user role selected successfully", authService.selectRole(userId, request)));
    }
}
