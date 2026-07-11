package com.example.trekkingapp.admin.controller;

import com.example.trekkingapp.admin.dto.request.AdminSettingsUpdateRequest;
import com.example.trekkingapp.admin.dto.response.AdminSettingItemResponse;
import com.example.trekkingapp.admin.service.AdminSettingsService;
import com.example.trekkingapp.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/settings")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSettingsController {

    private final AdminSettingsService adminSettingsService;

    public AdminSettingsController(AdminSettingsService adminSettingsService) {
        this.adminSettingsService = adminSettingsService;
    }

    @GetMapping
    public ApiResponse<List<AdminSettingItemResponse>> getSettings() {
        return new ApiResponse<>(true, "Settings retrieved successfully", adminSettingsService.getSettings(null));
    }

    @GetMapping("/{section}")
    public ApiResponse<List<AdminSettingItemResponse>> getSettingsBySection(@PathVariable String section) {
        return new ApiResponse<>(true, "Settings retrieved successfully", adminSettingsService.getSettings(section));
    }

    @PutMapping("/{section}")
    public ApiResponse<List<AdminSettingItemResponse>> updateSettings(@PathVariable String section, @Valid @RequestBody AdminSettingsUpdateRequest request) {
        return new ApiResponse<>(true, "Settings updated successfully", adminSettingsService.updateSettings(section, request));
    }
}
