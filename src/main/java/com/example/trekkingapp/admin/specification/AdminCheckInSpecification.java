package com.example.trekkingapp.admin.specification;

import com.example.trekkingapp.admin.checkin.AdminCheckInLog;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class AdminCheckInSpecification {

    private AdminCheckInSpecification() {
    }

    public static Specification<AdminCheckInLog> search(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String keyword = "%" + search.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(cb.coalesce(root.get("user").get("fullName"), "")), keyword),
                cb.like(cb.lower(cb.coalesce(root.get("tour").get("title"), "")), keyword),
                cb.like(cb.lower(cb.coalesce(root.get("checkpoint"), "")), keyword)
        );
    }

    public static Specification<AdminCheckInLog> tourId(Long tourId) {
        if (tourId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("tour").get("tourId"), tourId);
    }

    public static Specification<AdminCheckInLog> scheduleId(Long scheduleId) {
        if (scheduleId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("schedule").get("scheduleId"), scheduleId);
    }

    public static Specification<AdminCheckInLog> status(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(cb.upper(root.get("status")), status.trim().toUpperCase());
    }

    public static Specification<AdminCheckInLog> date(LocalDate date) {
        if (date == null) {
            return null;
        }
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay().minusNanos(1);
        return (root, query, cb) -> cb.between(root.get("checkInTime"), start, end);
    }
}
