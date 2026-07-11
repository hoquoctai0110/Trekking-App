package com.example.trekkingapp.tourprovider;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

public interface TourProviderRepository extends JpaRepository<TourProvider, Long>, JpaSpecificationExecutor<TourProvider> {

    @Override
    @EntityGraph(attributePaths = {"user", "user.userRoles", "user.userRoles.role"})
    Page<TourProvider> findAll(Specification<TourProvider> spec, Pageable pageable);

    Optional<TourProvider> findByUser_UserId(Long userId);

    boolean existsByUser_UserId(Long userId);

    List<TourProvider> findByStatusNot(String status);
}
