package com.example.trekkingapp.tourprovider;

import com.example.trekkingapp.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tour-providers")
public class TourProviderController {

    private final TourProviderService tourProviderService;

    public TourProviderController(TourProviderService tourProviderService) {
        this.tourProviderService = tourProviderService;
    }

    @PostMapping("/me")
    @PreAuthorize("hasRole('TOUR_PROVIDER')")
    public ApiResponse<TourProviderResponse> createMyProfile(@Valid @RequestBody TourProviderRequest request) {
        return new ApiResponse<>(true, "Tour provider profile created successfully", tourProviderService.createMyProfile(request));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('TOUR_PROVIDER')")
    public ApiResponse<TourProviderResponse> getMyProfile() {
        return new ApiResponse<>(true, "Tour provider profile retrieved successfully", tourProviderService.getMyProfile());
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('TOUR_PROVIDER')")
    public ApiResponse<TourProviderResponse> updateMyProfile(@Valid @RequestBody TourProviderRequest request) {
        return new ApiResponse<>(true, "Tour provider profile updated successfully", tourProviderService.updateMyProfile(request));
    }

    @DeleteMapping("/me")
    @PreAuthorize("hasRole('TOUR_PROVIDER')")
    public ApiResponse<String> deleteMyProfile() {
        return new ApiResponse<>(true, "Tour provider profile deleted successfully", tourProviderService.deleteMyProfile());
    }

    @GetMapping("/{providerId}")
    public ApiResponse<TourProviderResponse> getProvider(@PathVariable Long providerId) {
        return new ApiResponse<>(true, "Tour provider retrieved successfully", tourProviderService.findById(providerId));
    }

    @GetMapping
    public ApiResponse<List<TourProviderResponse>> getProviders() {
        return new ApiResponse<>(true, "Tour providers retrieved successfully", tourProviderService.findAll());
    }
}
