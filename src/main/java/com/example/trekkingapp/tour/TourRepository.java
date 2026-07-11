package com.example.trekkingapp.tour;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

public interface TourRepository extends JpaRepository<Tour, Long>, JpaSpecificationExecutor<Tour> {

    @Override
    @EntityGraph(attributePaths = {"provider", "provider.user", "route"})
    Page<Tour> findAll(Specification<Tour> spec, Pageable pageable);

    List<Tour> findByStatus(String status);

    List<Tour> findByStatusNot(String status);

    List<Tour> findByProvider_ProviderIdAndStatusNot(Long providerId, String status);

    Optional<Tour> findByTourIdAndStatusNot(Long tourId, String status);

    long countByProvider_User_UserId(Long userId);

    @Query("""
            select distinct t
            from Tour t
            join fetch t.provider
            join fetch t.route
            where t.status = :status
            """)
    List<Tour> findPublishedToursWithRelations(String status);

    @Query("""
            select distinct t
            from Tour t
            join fetch t.provider
            join fetch t.route
            where t.provider.providerId = :providerId and t.status <> :status
            """)
    List<Tour> findByProviderWithRelations(Long providerId, String status);

    @Query("""
            select t
            from Tour t
            join fetch t.provider
            join fetch t.route
            where t.tourId = :tourId and t.status <> :status
            """)
    Optional<Tour> findByTourIdWithRelations(Long tourId, String status);
}
