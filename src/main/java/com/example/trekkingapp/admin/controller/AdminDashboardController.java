package com.example.trekkingapp.admin.controller;

import com.example.trekkingapp.admin.dto.response.AdminBookingStatusSummaryResponse;
import com.example.trekkingapp.admin.dto.response.AdminDashboardSummaryResponse;
import com.example.trekkingapp.admin.dto.response.AdminRecentActivityResponse;
import com.example.trekkingapp.admin.dto.response.AdminRevenueChartItemResponse;
import com.example.trekkingapp.admin.dto.response.AdminTopTourResponse;
import com.example.trekkingapp.admin.service.AdminDashboardService;
import com.example.trekkingapp.common.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    public AdminDashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping("/summary")
    public ApiResponse<AdminDashboardSummaryResponse> summary() {
        return new ApiResponse<>(true, "Dashboard summary retrieved successfully", adminDashboardService.summary());
    }

    @GetMapping("/revenue")
    public ApiResponse<List<AdminRevenueChartItemResponse>> revenue(@RequestParam(defaultValue = "month") String period) {
        return new ApiResponse<>(true, "Revenue chart retrieved successfully", adminDashboardService.revenue(period));
    }

    @GetMapping("/booking-status")
    public ApiResponse<AdminBookingStatusSummaryResponse> bookingStatus() {
        return new ApiResponse<>(true, "Booking status summary retrieved successfully", adminDashboardService.bookingStatus());
    }

    @GetMapping("/top-tours")
    public ApiResponse<List<AdminTopTourResponse>> topTours(@RequestParam(defaultValue = "5") int limit) {
        return new ApiResponse<>(true, "Top tours retrieved successfully", adminDashboardService.topTours(limit));
    }

    @GetMapping("/recent-activities")
    public ApiResponse<List<AdminRecentActivityResponse>> recentActivities() {
        return new ApiResponse<>(true, "Recent activities retrieved successfully", adminDashboardService.recentActivities());
    }
}
