package com.example.trekkingapp.admin.specification;

import com.example.trekkingapp.tour.Tour;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

public final class AdminTourSpecification {

    private AdminTourSpecification() {
    }

    public static Specification<Tour> search(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String keyword = "%" + search.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(cb.coalesce(root.get("title"), "")), keyword),
                cb.like(cb.lower(cb.coalesce(root.get("description"), "")), keyword)
        );
    }

    public static Specification<Tour> status(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(cb.upper(root.get("status")), status.trim().toUpperCase());
    }

    public static Specification<Tour> difficulty(String difficulty) {
        if (difficulty == null || difficulty.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(cb.upper(root.get("difficulty")), difficulty.trim().toUpperCase());
    }

    public static Specification<Tour> province(String province) {
        if (province == null || province.isBlank()) {
            return null;
        }
        return (root, query, cb) -> {
            var route = root.join("route", JoinType.LEFT);
            return cb.like(cb.lower(cb.coalesce(route.get("routeName"), "")), "%" + province.trim().toLowerCase() + "%");
        };
    }

    public static Specification<Tour> providerId(Long providerId) {
        if (providerId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("provider").get("providerId"), providerId);
    }
}
