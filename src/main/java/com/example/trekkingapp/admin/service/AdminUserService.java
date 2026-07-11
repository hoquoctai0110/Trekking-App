package com.example.trekkingapp.admin.service;

import com.example.trekkingapp.admin.AdminSortUtils;
import com.example.trekkingapp.admin.audit.AdminAuditService;
import com.example.trekkingapp.admin.dto.request.AdminActionReasonRequest;
import com.example.trekkingapp.admin.dto.request.AdminChangeRoleRequest;
import com.example.trekkingapp.admin.dto.response.AdminUserDetailResponse;
import com.example.trekkingapp.admin.dto.response.AdminUserResponse;
import com.example.trekkingapp.admin.dto.response.PageResponse;
import com.example.trekkingapp.admin.specification.AdminUserSpecification;
import com.example.trekkingapp.auth.CurrentUserService;
import com.example.trekkingapp.booking.BookingRepository;
import com.example.trekkingapp.common.ConflictException;
import com.example.trekkingapp.common.ForbiddenOperationException;
import com.example.trekkingapp.common.ResourceNotFoundException;
import com.example.trekkingapp.common.ValidationException;
import com.example.trekkingapp.role.Role;
import com.example.trekkingapp.role.RoleRepository;
import com.example.trekkingapp.role.UserRole;
import com.example.trekkingapp.role.UserRoleRepository;
import com.example.trekkingapp.tour.TourRepository;
import com.example.trekkingapp.tourprovider.TourProviderRepository;
import com.example.trekkingapp.user.User;
import com.example.trekkingapp.user.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class AdminUserService {

    private static final Set<String> SORT_FIELDS = Set.of("createdAt", "updatedAt", "email", "fullName", "status");
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_BANNED = "BANNED";
    private static final String STATUS_DEACTIVATED = "DEACTIVATED";

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final TourRepository tourRepository;
    private final TourProviderRepository tourProviderRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final CurrentUserService currentUserService;
    private final AdminAuditService adminAuditService;

    public AdminUserService(
            UserRepository userRepository,
            BookingRepository bookingRepository,
            TourRepository tourRepository,
            TourProviderRepository tourProviderRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            CurrentUserService currentUserService,
            AdminAuditService adminAuditService
    ) {
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.tourRepository = tourRepository;
        this.tourProviderRepository = tourProviderRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.currentUserService = currentUserService;
        this.adminAuditService = adminAuditService;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> findUsers(int page, int size, String sort, String search, String role, String status) {
        Pageable pageable = AdminSortUtils.pageable(page, size, sort, "createdAt", SORT_FIELDS);
        Specification<User> specification = Specification
                .where(AdminUserSpecification.search(search))
                .and(AdminUserSpecification.role(role))
                .and(AdminUserSpecification.status(status));

        return PageResponse.from(userRepository.findAll(specification, pageable).map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public AdminUserDetailResponse getUser(Long id) {
        User user = findUser(id);
        Long providerId = tourProviderRepository.findByUser_UserId(id).map(provider -> provider.getProviderId()).orElse(null);
        return new AdminUserDetailResponse(
                user.getUserId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getAvatarUrl(),
                user.getStatus(),
                user.getAuthProvider(),
                user.getRoleSelected(),
                user.getDateOfBirth(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getUserRoles().stream().map(userRole -> userRole.getRole().getRoleName()).toList(),
                bookingRepository.countByTrekker_UserId(user.getUserId()),
                tourRepository.countByProvider_User_UserId(user.getUserId()),
                providerId
        );
    }

    @Transactional
    public AdminUserDetailResponse banUser(Long id, AdminActionReasonRequest request) {
        User current = findUser(currentUserService.getCurrentUserId());
        if (current.getUserId().equals(id)) {
            throw new ForbiddenOperationException("Admin cannot ban the current account");
        }

        User user = findUser(id);
        if (STATUS_BANNED.equalsIgnoreCase(user.getStatus())) {
            throw new ConflictException("User is already banned");
        }

        String oldStatus = user.getStatus();
        user.setStatus(STATUS_BANNED);
        userRepository.save(user);
        adminAuditService.log("BAN_USER", "USER", user.getUserId(), oldStatus, user.getStatus(), request.reason());
        return getUser(id);
    }

    @Transactional
    public AdminUserDetailResponse unbanUser(Long id) {
        User user = findUser(id);
        if (!STATUS_BANNED.equalsIgnoreCase(user.getStatus())) {
            throw new ConflictException("User is not banned");
        }

        String oldStatus = user.getStatus();
        user.setStatus(STATUS_ACTIVE);
        userRepository.save(user);
        adminAuditService.log("UNBAN_USER", "USER", user.getUserId(), oldStatus, user.getStatus(), null);
        return getUser(id);
    }

    @Transactional
    public AdminUserDetailResponse changeRole(Long id, AdminChangeRoleRequest request) {
        String roleName = request.role().trim().toUpperCase();
        Role role = roleRepository.findByRoleName(roleName)
                .orElseThrow(() -> new ValidationException("Role does not exist"));
        User user = findUser(id);

        String oldRoles = user.getUserRoles().stream().map(userRole -> userRole.getRole().getRoleName()).reduce((a, b) -> a + "," + b).orElse("");
        userRoleRepository.deleteByUser(user);
        user.getUserRoles().clear();

        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(role);
        userRoleRepository.save(userRole);
        user.getUserRoles().add(userRole);
        userRepository.save(user);

        adminAuditService.log("CHANGE_ROLE", "USER", user.getUserId(), oldRoles, roleName, null);
        return getUser(id);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = findUser(id);
        String oldStatus = user.getStatus();
        user.setStatus(STATUS_DEACTIVATED);
        userRepository.save(user);
        adminAuditService.log("DEACTIVATE_USER", "USER", user.getUserId(), oldStatus, STATUS_DEACTIVATED, null);
    }

    private User findUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private AdminUserResponse toResponse(User user) {
        String role = user.getUserRoles().stream()
                .map(userRole -> userRole.getRole().getRoleName())
                .findFirst()
                .orElse(null);

        return new AdminUserResponse(
                user.getUserId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getAvatarUrl(),
                role,
                user.getStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                bookingRepository.countByTrekker_UserId(user.getUserId()),
                tourRepository.countByProvider_User_UserId(user.getUserId())
        );
    }
}
