package com.example.trekkingapp.review;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByBooking_BookingId(Long bookingId);

    List<Review> findByTour_TourIdOrderByCreatedAtDesc(Long tourId);

    List<Review> findByTour_TourId(Long tourId);
}
