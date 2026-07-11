package com.example.trekkingapp.admin.service;

import com.example.trekkingapp.admin.audit.AdminAuditService;
import com.example.trekkingapp.admin.dto.request.AdminSettingsUpdateRequest;
import com.example.trekkingapp.admin.dto.response.AdminSettingItemResponse;
import com.example.trekkingapp.admin.settings.SystemSetting;
import com.example.trekkingapp.admin.settings.SystemSettingRepository;
import com.example.trekkingapp.common.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class AdminSettingsService {

    private static final Set<String> ALLOWED_SECTIONS = Set.of("general", "security", "notifications", "email", "payment");

    private final SystemSettingRepository systemSettingRepository;
    private final AdminAuditService adminAuditService;

    public AdminSettingsService(SystemSettingRepository systemSettingRepository, AdminAuditService adminAuditService) {
        this.systemSettingRepository = systemSettingRepository;
        this.adminAuditService = adminAuditService;
    }

    @Transactional(readOnly = true)
    public List<AdminSettingItemResponse> getSettings(String section) {
        if (section != null && !section.isBlank()) {
            validateSection(section);
            return systemSettingRepository.findBySectionOrderBySettingKeyAsc(section.trim().toLowerCase()).stream().map(this::toResponse).toList();
        }
        return systemSettingRepository.findAllByOrderBySectionAscSettingKeyAsc().stream().map(this::toResponse).toList();
    }

    @Transactional
    public List<AdminSettingItemResponse> updateSettings(String section, AdminSettingsUpdateRequest request) {
        validateSection(section);
        for (var entry : request.values().entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            boolean secret = isSecretKey(key);
            SystemSetting setting = systemSettingRepository.findBySectionAndSettingKey(section, key)
                    .orElseGet(SystemSetting::new);
            setting.setSection(section);
            setting.setSettingKey(key);
            setting.setSecret(secret);
            if (!secret) {
                setting.setSettingValue(value);
            } else if (value != null && !value.isBlank()) {
                setting.setSettingValue("CONFIGURED");
            }
            systemSettingRepository.save(setting);
        }
        adminAuditService.log("UPDATE_SETTINGS", "SYSTEM_SETTING", section, null, "UPDATED", null);
        return getSettings(section);
    }

    private void validateSection(String section) {
        if (!ALLOWED_SECTIONS.contains(section.trim().toLowerCase())) {
            throw new ValidationException("Invalid settings section");
        }
    }

    private boolean isSecretKey(String key) {
        String normalized = key.toLowerCase();
        return normalized.contains("password") || normalized.contains("secret") || normalized.contains("apiKey".toLowerCase()) || normalized.contains("token");
    }

    private AdminSettingItemResponse toResponse(SystemSetting setting) {
        return new AdminSettingItemResponse(
                setting.getSection(),
                setting.getSettingKey(),
                setting.isSecret() ? null : setting.getSettingValue(),
                setting.isSecret(),
                setting.isSecret() ? setting.getSettingValue() != null && !setting.getSettingValue().isBlank() : setting.getSettingValue() != null
        );
    }
}
