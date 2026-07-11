package com.example.trekkingapp.admin.controller;

import com.example.trekkingapp.admin.dto.request.AdminReviewFlagRequest;
import com.example.trekkingapp.admin.dto.request.AdminReviewVisibilityRequest;
import com.example.trekkingapp.admin.dto.response.AdminReviewResponse;
import com.example.trekkingapp.admin.dto.response.PageResponse;
import com.example.trekkingapp.admin.service.AdminReviewService;
import com.example.trekkingapp.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/reviews")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReviewController {

    private final AdminReviewService adminReviewService;

    public AdminReviewController(AdminReviewService adminReviewService) {
        this.adminReviewService = adminReviewService;
    }

    @GetMapping
    public ApiResponse<PageResponse<AdminReviewResponse>> getReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) Boolean flagged,
            @RequestParam(required = false) Boolean visible,
            @RequestParam(required = false) Long tourId,
            @RequestParam(required = false) Long userId
    ) {
        return new ApiResponse<>(true, "Reviews retrieved successfully", adminReviewService.findReviews(page, size, sort, search, rating, flagged, visible, tourId, userId));
    }

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getReviewStats() {
        return new ApiResponse<>(true, "Review stats retrieved successfully", adminReviewService.stats());
    }

    @PatchMapping("/{id}/visibility")
    public ApiResponse<AdminReviewResponse> updateVisibility(@PathVariable Long id, @Valid @RequestBody AdminReviewVisibilityRequest request) {
        return new ApiResponse<>(true, "Review visibility updated successfully", adminReviewService.updateVisibility(id, request));
    }

    @PatchMapping("/{id}/flag")
    public ApiResponse<AdminReviewResponse> updateFlag(@PathVariable Long id, @Valid @RequestBody AdminReviewFlagRequest request) {
        return new ApiResponse<>(true, "Review flag updated successfully", adminReviewService.updateFlag(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteReview(@PathVariable Long id) {
        adminReviewService.delete(id);
        return new ApiResponse<>(true, "Review deleted successfully", null);
    }
}
