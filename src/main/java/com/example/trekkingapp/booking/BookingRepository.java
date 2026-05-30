package com.example.trekkingapp.booking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByTrekker_UserId(Long userId);

    List<Booking> findByTour_Provider_ProviderId(Long providerId);

    Optional<Booking> findByBookingId(Long bookingId);
}
