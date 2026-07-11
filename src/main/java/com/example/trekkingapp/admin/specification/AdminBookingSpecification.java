package com.example.trekkingapp.admin.specification;

import com.example.trekkingapp.booking.Booking;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public final class AdminBookingSpecification {

    private AdminBookingSpecification() {
    }

    public static Specification<Booking> search(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String keyword = "%" + search.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(cb.coalesce(root.get("trekker").get("fullName"), "")), keyword),
                cb.like(cb.lower(cb.coalesce(root.get("trekker").get("email"), "")), keyword),
                cb.like(cb.lower(cb.coalesce(root.get("tour").get("title"), "")), keyword)
        );
    }

    public static Specification<Booking> bookingStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("bookingStatus"), Enum.valueOf(com.example.trekkingapp.booking.BookingStatus.class, status.trim().toUpperCase()));
    }

    public static Specification<Booking> paymentStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("paymentStatus"), Enum.valueOf(com.example.trekkingapp.booking.PaymentStatus.class, status.trim().toUpperCase()));
    }

    public static Specification<Booking> dateFrom(LocalDateTime dateFrom) {
        if (dateFrom == null) {
            return null;
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("bookedAt"), dateFrom);
    }

    public static Specification<Booking> dateTo(LocalDateTime dateTo) {
        if (dateTo == null) {
            return null;
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("bookedAt"), dateTo);
    }

    public static Specification<Booking> tourId(Long tourId) {
        if (tourId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("tour").get("tourId"), tourId);
    }

    public static Specification<Booking> providerId(Long providerId) {
        if (providerId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("tour").get("provider").get("providerId"), providerId);
    }

    public static Specification<Booking> userId(Long userId) {
        if (userId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("trekker").get("userId"), userId);
    }
}
