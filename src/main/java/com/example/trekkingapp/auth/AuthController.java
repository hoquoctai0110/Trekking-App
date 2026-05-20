package com.example.trekkingapp.auth;

import com.example.trekkingapp.common.ApiResponse;
import com.example.trekkingapp.user.UserResponse;
import jakarta.validation.Valid;
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

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/google")
    public ApiResponse<GoogleLoginResponse> loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        System.out.println("Received Google idToken: " + request.idToken());
        return new ApiResponse<>(true, "Google login successful", authService.loginWithGoogle(request));
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> getCurrentUser() {
        return new ApiResponse<>(true, "Current user retrieved successfully", authService.getCurrentUser());
    }

    @PostMapping("/users/{userId}/select-role")
    public ResponseEntity<ApiResponse<UserResponse>> selectRole(
            @PathVariable Long userId,
            @Valid @RequestBody SelectRoleRequest request
    ) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Role selected successfully", authService.selectRole(userId, request)));
    }
}
