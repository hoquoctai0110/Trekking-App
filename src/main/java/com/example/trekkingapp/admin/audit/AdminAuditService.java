package com.example.trekkingapp.admin.audit;

import com.example.trekkingapp.auth.CurrentUserService;
import org.springframework.stereotype.Service;

@Service
public class AdminAuditService {

    private final AdminAuditLogRepository adminAuditLogRepository;
    private final CurrentUserService currentUserService;

    public AdminAuditService(AdminAuditLogRepository adminAuditLogRepository, CurrentUserService currentUserService) {
        this.adminAuditLogRepository = adminAuditLogRepository;
        this.currentUserService = currentUserService;
    }

    public void log(String action, String entityType, Object entityId, String oldValue, String newValue, String reason) {
        AdminAuditLog log = new AdminAuditLog();
        log.setAdminId(currentUserService.getCurrentUserId());
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(String.valueOf(entityId));
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        log.setReason(reason);
        adminAuditLogRepository.save(log);
    }
}
