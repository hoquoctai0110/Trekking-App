package com.example.trekkingapp.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {

    @Override
    @EntityGraph(attributePaths = {"tour", "tour.provider", "tour.provider.user", "schedule", "trekker", "payment"})
    Page<Booking> findAll(Specification<Booking> spec, Pageable pageable);

    List<Booking> findByTrekker_UserId(Long userId);

    List<Booking> findByTour_Provider_ProviderId(Long providerId);

    Optional<Booking> findByBookingId(Long bookingId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"tour", "tour.provider", "tour.provider.user", "schedule", "trekker", "payment"})
    Optional<Booking> findWithLockByBookingId(Long bookingId);

    long countByTrekker_UserId(Long userId);

    long countByTour_Provider_User_UserId(Long userId);

    long countByTour_TourId(Long tourId);
}
