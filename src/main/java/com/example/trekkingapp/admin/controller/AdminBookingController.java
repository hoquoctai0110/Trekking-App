package com.example.trekkingapp.admin.controller;

import com.example.trekkingapp.admin.dto.request.AdminActionReasonRequest;
import com.example.trekkingapp.admin.dto.response.AdminBookingResponse;
import com.example.trekkingapp.admin.dto.response.PageResponse;
import com.example.trekkingapp.admin.service.AdminBookingService;
import com.example.trekkingapp.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/admin/bookings")
@PreAuthorize("hasRole('ADMIN')")
public class AdminBookingController {

    private final AdminBookingService adminBookingService;

    public AdminBookingController(AdminBookingService adminBookingService) {
        this.adminBookingService = adminBookingService;
    }

    @GetMapping
    public ApiResponse<PageResponse<AdminBookingResponse>> getBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) Long tourId,
            @RequestParam(required = false) Long tourProviderId,
            @RequestParam(required = false) Long userId
    ) {
        return new ApiResponse<>(true, "Bookings retrieved successfully", adminBookingService.findBookings(page, size, sort, search, status, paymentStatus, dateFrom, dateTo, tourId, tourProviderId, userId));
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminBookingResponse> getBooking(@PathVariable Long id) {
        return new ApiResponse<>(true, "Booking retrieved successfully", adminBookingService.getBooking(id));
    }

    @PatchMapping("/{id}/cancel")
    public ApiResponse<AdminBookingResponse> cancelBooking(@PathVariable Long id, @Valid @RequestBody AdminActionReasonRequest request) {
        return new ApiResponse<>(true, "Booking cancelled successfully", adminBookingService.cancelBooking(id, request));
    }

    @PatchMapping("/{id}/refund")
    public ApiResponse<AdminBookingResponse> refundBooking(@PathVariable Long id, @Valid @RequestBody AdminActionReasonRequest request) {
        return new ApiResponse<>(true, "Refund requested successfully", adminBookingService.refundBooking(id, request));
    }
}
