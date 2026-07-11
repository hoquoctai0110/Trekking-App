package com.example.trekkingapp.admin.controller;

import com.example.trekkingapp.admin.dto.request.AdminActionReasonRequest;
import com.example.trekkingapp.admin.dto.response.AdminTourProviderDetailResponse;
import com.example.trekkingapp.admin.dto.response.AdminTourProviderResponse;
import com.example.trekkingapp.admin.dto.response.PageResponse;
import com.example.trekkingapp.admin.service.AdminTourProviderService;
import com.example.trekkingapp.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/tour-providers")
@PreAuthorize("hasRole('ADMIN')")
public class AdminTourProviderController {

    private final AdminTourProviderService adminTourProviderService;

    public AdminTourProviderController(AdminTourProviderService adminTourProviderService) {
        this.adminTourProviderService = adminTourProviderService;
    }

    @GetMapping
    public ApiResponse<PageResponse<AdminTourProviderResponse>> getProviders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status
    ) {
        return new ApiResponse<>(true, "Tour providers retrieved successfully", adminTourProviderService.findProviders(page, size, sort, search, status));
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminTourProviderDetailResponse> getProvider(@PathVariable Long id) {
        return new ApiResponse<>(true, "Tour provider retrieved successfully", adminTourProviderService.getProvider(id));
    }

    @PatchMapping("/{id}/approve")
    public ApiResponse<AdminTourProviderDetailResponse> approve(@PathVariable Long id) {
        return new ApiResponse<>(true, "Tour provider approved successfully", adminTourProviderService.approve(id));
    }

    @PatchMapping("/{id}/reject")
    public ApiResponse<AdminTourProviderDetailResponse> reject(@PathVariable Long id, @Valid @RequestBody AdminActionReasonRequest request) {
        return new ApiResponse<>(true, "Tour provider rejected successfully", adminTourProviderService.reject(id, request));
    }

    @PatchMapping("/{id}/suspend")
    public ApiResponse<AdminTourProviderDetailResponse> suspend(@PathVariable Long id, @Valid @RequestBody AdminActionReasonRequest request) {
        return new ApiResponse<>(true, "Tour provider suspended successfully", adminTourProviderService.suspend(id, request));
    }

    @PatchMapping("/{id}/reactivate")
    public ApiResponse<AdminTourProviderDetailResponse> reactivate(@PathVariable Long id) {
        return new ApiResponse<>(true, "Tour provider reactivated successfully", adminTourProviderService.reactivate(id));
    }
}
