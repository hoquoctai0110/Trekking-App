package com.example.trekkingapp.auth;

import com.example.trekkingapp.common.ApiResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("dev")
@RestController
@RequestMapping("/api/v1/security-test")
public class TestSecurityController {

    @GetMapping("/authenticated")
    public ApiResponse<String> authenticated() {
        return new ApiResponse<>(true, "OK", "Authenticated OK");
    }

    @GetMapping("/trekker")
    @PreAuthorize("hasRole('TREKKER')")
    public ApiResponse<String> trekker() {
        return new ApiResponse<>(true, "OK", "TREKKER OK");
    }

    @GetMapping("/provider")
    @PreAuthorize("hasRole('TOUR_PROVIDER')")
    public ApiResponse<String> provider() {
        return new ApiResponse<>(true, "OK", "TOUR_PROVIDER OK");
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> admin() {
        return new ApiResponse<>(true, "OK", "ADMIN OK");
    }
}
