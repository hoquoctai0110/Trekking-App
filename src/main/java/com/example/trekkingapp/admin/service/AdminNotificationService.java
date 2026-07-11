package com.example.trekkingapp.admin.service;

import com.example.trekkingapp.admin.AdminSortUtils;
import com.example.trekkingapp.admin.audit.AdminAuditService;
import com.example.trekkingapp.admin.dto.request.AdminNotificationCreateRequest;
import com.example.trekkingapp.admin.dto.response.AdminNotificationResponse;
import com.example.trekkingapp.admin.dto.response.PageResponse;
import com.example.trekkingapp.admin.notification.AdminNotification;
import com.example.trekkingapp.admin.notification.AdminNotificationRepository;
import com.example.trekkingapp.admin.specification.AdminNotificationSpecification;
import com.example.trekkingapp.auth.CurrentUserService;
import com.example.trekkingapp.common.ConflictException;
import com.example.trekkingapp.common.ResourceNotFoundException;
import com.example.trekkingapp.common.ValidationException;
import com.example.trekkingapp.user.User;
import com.example.trekkingapp.user.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Service
public class AdminNotificationService {

    private static final Set<String> SORT_FIELDS = Set.of("createdAt", "scheduledAt", "sentAt", "title", "status");
    private static final Set<String> RECIPIENT_TYPES = Set.of("ALL_USERS", "TREKKERS", "TOUR_PROVIDERS", "SPECIFIC_USERS");

    private final AdminNotificationRepository adminNotificationRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final AdminAuditService adminAuditService;

    public AdminNotificationService(
            AdminNotificationRepository adminNotificationRepository,
            UserRepository userRepository,
            CurrentUserService currentUserService,
            AdminAuditService adminAuditService
    ) {
        this.adminNotificationRepository = adminNotificationRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.adminAuditService = adminAuditService;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminNotificationResponse> findNotifications(int page, int size, String sort, String search, String type, String status, String recipientType) {
        Pageable pageable = AdminSortUtils.pageable(page, size, sort, "createdAt", SORT_FIELDS);
        Specification<AdminNotification> specification = Specification
                .where(AdminNotificationSpecification.search(search))
                .and(AdminNotificationSpecification.type(type))
                .and(AdminNotificationSpecification.status(status))
                .and(AdminNotificationSpecification.recipientType(recipientType));
        return PageResponse.from(adminNotificationRepository.findAll(specification, pageable).map(this::toResponse));
    }

    @Transactional
    public AdminNotificationResponse create(AdminNotificationCreateRequest request) {
        String recipientType = request.recipientType().trim().toUpperCase();
        if (!RECIPIENT_TYPES.contains(recipientType)) {
            throw new ValidationException("Invalid recipient type");
        }
        if ("SPECIFIC_USERS".equals(recipientType) && request.recipientIds().isEmpty()) {
            throw new ValidationException("recipientIds are required for SPECIFIC_USERS");
        }

        String idempotencyKey = request.idempotencyKey() == null || request.idempotencyKey().isBlank()
                ? UUID.randomUUID().toString()
                : request.idempotencyKey().trim();
        if (adminNotificationRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            throw new ConflictException("Notification request has already been processed");
        }

        User admin = userRepository.findById(currentUserService.getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        AdminNotification notification = new AdminNotification();
        notification.setTitle(request.title().trim());
        notification.setBody(request.body().trim());
        notification.setType(request.type().trim().toUpperCase());
        notification.setRecipientType(recipientType);
        notification.setRecipientIds(request.recipientIds().stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse(""));
        notification.setScheduledAt(request.scheduledAt());
        notification.setIdempotencyKey(idempotencyKey);
        notification.setCreatedBy(admin);
        notification.setStatus(request.scheduledAt() == null ? "SAVED" : "SCHEDULED");
        notification.setSentAt(request.scheduledAt() == null ? LocalDateTime.now() : null);
        AdminNotification saved = adminNotificationRepository.save(notification);
        adminAuditService.log("SEND_NOTIFICATION", "ADMIN_NOTIFICATION", saved.getId(), null, saved.getStatus(), request.title());
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        AdminNotification notification = adminNotificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        adminNotificationRepository.delete(notification);
        adminAuditService.log("DELETE_NOTIFICATION", "ADMIN_NOTIFICATION", id, notification.getStatus(), "DELETED", null);
    }

    private AdminNotificationResponse toResponse(AdminNotification notification) {
        return new AdminNotificationResponse(
                notification.getId(),
                notification.getTitle(),
                notification.getBody(),
                notification.getType(),
                notification.getStatus(),
                notification.getRecipientType(),
                notification.getRecipientIds(),
                notification.getScheduledAt(),
                notification.getSentAt(),
                notification.getCreatedAt()
        );
    }
}
