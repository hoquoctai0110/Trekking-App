package com.example.trekkingapp.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    @Override
    @EntityGraph(attributePaths = {"userRoles", "userRoles.role"})
    Page<User> findAll(Specification<User> spec, Pageable pageable);

    Optional<User> findByGoogleId(String googleId);

    Optional<User> findByEmail(String email);

    boolean existsByGoogleId(String googleId);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);
}
