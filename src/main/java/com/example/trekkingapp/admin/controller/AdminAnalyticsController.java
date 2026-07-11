package com.example.trekkingapp.admin.controller;

import com.example.trekkingapp.admin.dto.response.AdminAnalyticsOverviewResponse;
import com.example.trekkingapp.admin.dto.response.AdminDashboardSummaryResponse;
import com.example.trekkingapp.admin.dto.response.AdminRevenueChartItemResponse;
import com.example.trekkingapp.admin.dto.response.AdminTopTourResponse;
import com.example.trekkingapp.admin.service.AdminAnalyticsService;
import com.example.trekkingapp.common.ApiResponse;
import com.example.trekkingapp.common.ValidationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/analytics")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAnalyticsController {

    private final AdminAnalyticsService adminAnalyticsService;

    public AdminAnalyticsController(AdminAnalyticsService adminAnalyticsService) {
        this.adminAnalyticsService = adminAnalyticsService;
    }

    @GetMapping("/overview")
    public ApiResponse<AdminAnalyticsOverviewResponse> overview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
    ) {
        return new ApiResponse<>(true, "Analytics overview retrieved successfully", adminAnalyticsService.overview(dateFrom, dateTo));
    }

    @GetMapping("/revenue-bookings")
    public ApiResponse<List<AdminRevenueChartItemResponse>> revenueBookings(@RequestParam(defaultValue = "month") String period) {
        return new ApiResponse<>(true, "Revenue and bookings analytics retrieved successfully", adminAnalyticsService.revenueBookings(period));
    }

    @GetMapping("/top-tours")
    public ApiResponse<List<AdminTopTourResponse>> topTours() {
        return new ApiResponse<>(true, "Top tours analytics retrieved successfully", adminAnalyticsService.topTours());
    }

    @GetMapping("/new-users")
    public ApiResponse<List<AdminDashboardSummaryResponse.TrendPoint>> newUsers() {
        return new ApiResponse<>(true, "New users analytics retrieved successfully", adminAnalyticsService.newUsers());
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
    ) {
        if (!"csv".equalsIgnoreCase(format)) {
            throw new ValidationException("Only csv export is currently supported");
        }
        byte[] content = adminAnalyticsService.exportCsv(dateFrom, dateTo);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=admin-analytics.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(content);
    }
}
