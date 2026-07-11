package com.example.trekkingapp.admin.specification;

import com.example.trekkingapp.review.Review;
import org.springframework.data.jpa.domain.Specification;

public final class AdminReviewSpecification {

    private AdminReviewSpecification() {
    }

    public static Specification<Review> search(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String keyword = "%" + search.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(cb.coalesce(root.get("comment"), "")), keyword),
                cb.like(cb.lower(cb.coalesce(root.get("user").get("fullName"), "")), keyword),
                cb.like(cb.lower(cb.coalesce(root.get("tour").get("title"), "")), keyword)
        );
    }

    public static Specification<Review> rating(Integer rating) {
        if (rating == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("rating"), rating);
    }

    public static Specification<Review> flagged(Boolean flagged) {
        if (flagged == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("flagged"), flagged);
    }

    public static Specification<Review> visible(Boolean visible) {
        if (visible == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("visible"), visible);
    }

    public static Specification<Review> tourId(Long tourId) {
        if (tourId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("tour").get("tourId"), tourId);
    }

    public static Specification<Review> userId(Long userId) {
        if (userId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("user").get("userId"), userId);
    }

    public static Specification<Review> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }
}
