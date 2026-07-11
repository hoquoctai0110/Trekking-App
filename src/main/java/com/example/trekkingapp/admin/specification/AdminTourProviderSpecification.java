package com.example.trekkingapp.admin.specification;

import com.example.trekkingapp.tourprovider.TourProvider;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

public final class AdminTourProviderSpecification {

    private AdminTourProviderSpecification() {
    }

    public static Specification<TourProvider> search(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String keyword = "%" + search.trim().toLowerCase() + "%";
        return (root, query, cb) -> {
            var user = root.join("user", JoinType.LEFT);
            return cb.or(
                    cb.like(cb.lower(cb.coalesce(root.get("companyName"), "")), keyword),
                    cb.like(cb.lower(cb.coalesce(root.get("email"), "")), keyword),
                    cb.like(cb.lower(cb.coalesce(root.get("phone"), "")), keyword),
                    cb.like(cb.lower(cb.coalesce(user.get("fullName"), "")), keyword)
            );
        };
    }

    public static Specification<TourProvider> status(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(cb.upper(root.get("status")), status.trim().toUpperCase());
    }
}
