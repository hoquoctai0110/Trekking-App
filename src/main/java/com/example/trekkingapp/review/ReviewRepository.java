package com.example.trekkingapp.review;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long>, JpaSpecificationExecutor<Review> {

    @Override
    @EntityGraph(attributePaths = {"user", "tour", "tour.provider", "tour.provider.user", "booking"})
    Page<Review> findAll(Specification<Review> spec, Pageable pageable);

    boolean existsByBooking_BookingId(Long bookingId);

    List<Review> findByTour_TourIdOrderByCreatedAtDesc(Long tourId);

    List<Review> findByTour_TourId(Long tourId);

    long countByTour_TourIdAndDeletedAtIsNull(Long tourId);

    @Query("select avg(r.rating) from Review r where r.tour.tourId = :tourId and r.deletedAt is null")
    Optional<Double> findAverageRatingByTourId(@Param("tourId") Long tourId);
}
