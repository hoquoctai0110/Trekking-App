package com.example.trekkingapp.admin.controller;

import com.example.trekkingapp.admin.dto.request.AdminActionReasonRequest;
import com.example.trekkingapp.admin.dto.request.AdminChangeRoleRequest;
import com.example.trekkingapp.admin.dto.response.AdminUserDetailResponse;
import com.example.trekkingapp.admin.dto.response.AdminUserResponse;
import com.example.trekkingapp.admin.dto.response.PageResponse;
import com.example.trekkingapp.admin.service.AdminUserService;
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

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public ApiResponse<PageResponse<AdminUserResponse>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status
    ) {
        return new ApiResponse<>(true, "Users retrieved successfully", adminUserService.findUsers(page, size, sort, search, role, status));
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminUserDetailResponse> getUser(@PathVariable Long id) {
        return new ApiResponse<>(true, "User retrieved successfully", adminUserService.getUser(id));
    }

    @PatchMapping("/{id}/ban")
    public ApiResponse<AdminUserDetailResponse> banUser(@PathVariable Long id, @Valid @RequestBody AdminActionReasonRequest request) {
        return new ApiResponse<>(true, "User banned successfully", adminUserService.banUser(id, request));
    }

    @PatchMapping("/{id}/unban")
    public ApiResponse<AdminUserDetailResponse> unbanUser(@PathVariable Long id) {
        return new ApiResponse<>(true, "User unbanned successfully", adminUserService.unbanUser(id));
    }

    @PatchMapping("/{id}/role")
    public ApiResponse<AdminUserDetailResponse> changeRole(@PathVariable Long id, @Valid @RequestBody AdminChangeRoleRequest request) {
        return new ApiResponse<>(true, "User role updated successfully", adminUserService.changeRole(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteUser(@PathVariable Long id) {
        adminUserService.deleteUser(id);
        return new ApiResponse<>(true, "User deactivated successfully", null);
    }
}
