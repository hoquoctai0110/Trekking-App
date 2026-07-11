package com.example.trekkingapp.admin.specification;

import com.example.trekkingapp.user.User;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

public final class AdminUserSpecification {

    private AdminUserSpecification() {
    }

    public static Specification<User> search(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String keyword = "%" + search.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(cb.coalesce(root.get("fullName"), "")), keyword),
                cb.like(cb.lower(cb.coalesce(root.get("email"), "")), keyword),
                cb.like(cb.lower(cb.coalesce(root.get("phone"), "")), keyword)
        );
    }

    public static Specification<User> status(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(cb.upper(root.get("status")), status.trim().toUpperCase());
    }

    public static Specification<User> role(String role) {
        if (role == null || role.isBlank()) {
            return null;
        }
        return (root, query, cb) -> {
            var userRoles = root.join("userRoles", JoinType.LEFT);
            var roleJoin = userRoles.join("role", JoinType.LEFT);
            query.distinct(true);
            return cb.equal(cb.upper(roleJoin.get("roleName")), role.trim().toUpperCase());
        };
    }
}
