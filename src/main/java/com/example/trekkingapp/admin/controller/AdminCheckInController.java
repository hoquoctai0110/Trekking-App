package com.example.trekkingapp.admin.controller;

import com.example.trekkingapp.admin.dto.request.AdminCheckInQrRequest;
import com.example.trekkingapp.admin.dto.response.AdminCheckInResponse;
import com.example.trekkingapp.admin.dto.response.AdminCheckInSummaryResponse;
import com.example.trekkingapp.admin.dto.response.AdminGeneratedQrResponse;
import com.example.trekkingapp.admin.dto.response.PageResponse;
import com.example.trekkingapp.admin.service.AdminCheckInService;
import com.example.trekkingapp.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/admin/checkins")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCheckInController {

    private final AdminCheckInService adminCheckInService;

    public AdminCheckInController(AdminCheckInService adminCheckInService) {
        this.adminCheckInService = adminCheckInService;
    }

    @GetMapping
    public ApiResponse<PageResponse<AdminCheckInResponse>> getCheckIns(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long tourId,
            @RequestParam(required = false) Long scheduleId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return new ApiResponse<>(true, "Check-ins retrieved successfully", adminCheckInService.findCheckIns(page, size, sort, search, tourId, scheduleId, status, date));
    }

    @GetMapping("/summary")
    public ApiResponse<AdminCheckInSummaryResponse> summary() {
        return new ApiResponse<>(true, "Check-in summary retrieved successfully", adminCheckInService.summary());
    }

    @PostMapping("/qr/generate")
    public ApiResponse<AdminGeneratedQrResponse> generate(@Valid @RequestBody AdminCheckInQrRequest request) {
        return new ApiResponse<>(true, "Check-in QR generated successfully", adminCheckInService.generate(request));
    }

    @PatchMapping("/qr/regenerate")
    public ApiResponse<AdminGeneratedQrResponse> regenerate(@Valid @RequestBody AdminCheckInQrRequest request) {
        return new ApiResponse<>(true, "Check-in QR regenerated successfully", adminCheckInService.regenerate(request));
    }
}
