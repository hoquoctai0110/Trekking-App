package com.example.trekkingapp.sos;

import com.example.trekkingapp.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sos")
public class SOSController {

    private final SOSService sosService;

    public SOSController(SOSService sosService) {
        this.sosService = sosService;
    }

    @PostMapping
    @PreAuthorize("hasRole('TREKKER')")
    public ApiResponse<SOSResponse> create(@Valid @RequestBody SOSRequest request) {
        return new ApiResponse<>(true, "SOS alert created successfully", sosService.create(request));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('TREKKER')")
    public ApiResponse<List<SOSResponse>> getMine() {
        return new ApiResponse<>(true, "SOS alerts retrieved successfully", sosService.findMine());
    }

    @GetMapping("/provider")
    @PreAuthorize("hasRole('TOUR_PROVIDER')")
    public ApiResponse<List<SOSResponse>> getProviderAlerts() {
        return new ApiResponse<>(true, "SOS alerts retrieved successfully", sosService.findForProvider());
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<SOSResponse>> getAllAlerts() {
        return new ApiResponse<>(true, "SOS alerts retrieved successfully", sosService.findAll());
    }

    @PutMapping("/{sosId}/acknowledge")
    @PreAuthorize("hasAnyRole('TOUR_PROVIDER', 'ADMIN')")
    public ApiResponse<SOSResponse> acknowledge(@PathVariable Long sosId) {
        return new ApiResponse<>(true, "SOS alert acknowledged successfully", sosService.acknowledge(sosId));
    }

    @PutMapping("/{sosId}/resolve")
    @PreAuthorize("hasAnyRole('TOUR_PROVIDER', 'ADMIN')")
    public ApiResponse<SOSResponse> resolve(@PathVariable Long sosId) {
        return new ApiResponse<>(true, "SOS alert resolved successfully", sosService.resolve(sosId));
    }
}
