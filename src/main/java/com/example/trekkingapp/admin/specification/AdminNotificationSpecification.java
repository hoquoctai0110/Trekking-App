package com.example.trekkingapp.admin.specification;

import com.example.trekkingapp.admin.notification.AdminNotification;
import org.springframework.data.jpa.domain.Specification;

public final class AdminNotificationSpecification {

    private AdminNotificationSpecification() {
    }

    public static Specification<AdminNotification> search(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String keyword = "%" + search.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(cb.coalesce(root.get("title"), "")), keyword),
                cb.like(cb.lower(cb.coalesce(root.get("body"), "")), keyword)
        );
    }

    public static Specification<AdminNotification> type(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(cb.upper(root.get("type")), type.trim().toUpperCase());
    }

    public static Specification<AdminNotification> status(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(cb.upper(root.get("status")), status.trim().toUpperCase());
    }

    public static Specification<AdminNotification> recipientType(String recipientType) {
        if (recipientType == null || recipientType.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(cb.upper(root.get("recipientType")), recipientType.trim().toUpperCase());
    }
}
