package com.example.trekkingapp.health;

import com.example.trekkingapp.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    @GetMapping
    public ApiResponse<String> health() {
        return new ApiResponse<>(true, "OK", "Trekking backend is running");
    }
}
