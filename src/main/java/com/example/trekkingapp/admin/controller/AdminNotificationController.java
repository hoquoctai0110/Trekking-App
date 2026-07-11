package com.example.trekkingapp.admin.controller;

import com.example.trekkingapp.admin.dto.request.AdminNotificationCreateRequest;
import com.example.trekkingapp.admin.dto.response.AdminNotificationResponse;
import com.example.trekkingapp.admin.dto.response.PageResponse;
import com.example.trekkingapp.admin.service.AdminNotificationService;
import com.example.trekkingapp.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/notifications")
@PreAuthorize("hasRole('ADMIN')")
public class AdminNotificationController {

    private final AdminNotificationService adminNotificationService;

    public AdminNotificationController(AdminNotificationService adminNotificationService) {
        this.adminNotificationService = adminNotificationService;
    }

    @GetMapping
    public ApiResponse<PageResponse<AdminNotificationResponse>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String recipientType
    ) {
        return new ApiResponse<>(true, "Notifications retrieved successfully", adminNotificationService.findNotifications(page, size, sort, search, type, status, recipientType));
    }

    @PostMapping
    public ApiResponse<AdminNotificationResponse> createNotification(@Valid @RequestBody AdminNotificationCreateRequest request) {
        return new ApiResponse<>(true, "Notification created successfully", adminNotificationService.create(request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteNotification(@PathVariable Long id) {
        adminNotificationService.delete(id);
        return new ApiResponse<>(true, "Notification deleted successfully", null);
    }
}
