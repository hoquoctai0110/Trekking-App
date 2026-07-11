package com.example.trekkingapp.admin.controller;

import com.example.trekkingapp.admin.dto.request.AdminActionReasonRequest;
import com.example.trekkingapp.admin.dto.response.AdminTourResponse;
import com.example.trekkingapp.admin.dto.response.PageResponse;
import com.example.trekkingapp.admin.service.AdminTourService;
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
@RequestMapping("/api/v1/admin/tours")
@PreAuthorize("hasRole('ADMIN')")
public class AdminTourController {

    private final AdminTourService adminTourService;

    public AdminTourController(AdminTourService adminTourService) {
        this.adminTourService = adminTourService;
    }

    @GetMapping
    public ApiResponse<PageResponse<AdminTourResponse>> getTours(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) Long tourProviderId
    ) {
        return new ApiResponse<>(true, "Tours retrieved successfully", adminTourService.findTours(page, size, sort, search, status, province, difficulty, tourProviderId));
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminTourResponse> getTour(@PathVariable Long id) {
        return new ApiResponse<>(true, "Tour retrieved successfully", adminTourService.getTour(id));
    }

    @PatchMapping("/{id}/approve")
    public ApiResponse<AdminTourResponse> approve(@PathVariable Long id) {
        return new ApiResponse<>(true, "Tour approved successfully", adminTourService.approve(id));
    }

    @PatchMapping("/{id}/reject")
    public ApiResponse<AdminTourResponse> reject(@PathVariable Long id, @Valid @RequestBody AdminActionReasonRequest request) {
        return new ApiResponse<>(true, "Tour rejected successfully", adminTourService.reject(id, request));
    }

    @PatchMapping("/{id}/publish")
    public ApiResponse<AdminTourResponse> publish(@PathVariable Long id) {
        return new ApiResponse<>(true, "Tour published successfully", adminTourService.publish(id));
    }

    @PatchMapping("/{id}/unpublish")
    public ApiResponse<AdminTourResponse> unpublish(@PathVariable Long id) {
        return new ApiResponse<>(true, "Tour unpublished successfully", adminTourService.unpublish(id));
    }

    @PatchMapping("/{id}/archive")
    public ApiResponse<AdminTourResponse> archive(@PathVariable Long id) {
        return new ApiResponse<>(true, "Tour archived successfully", adminTourService.archive(id));
    }
}
