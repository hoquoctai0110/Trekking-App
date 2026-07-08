package com.example.trekkingapp.tracking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrackingSessionRepository extends JpaRepository<TrackingSession, Long> {

    Optional<TrackingSession> findBySessionId(Long sessionId);

    List<TrackingSession> findByUser_UserId(Long userId);

    Optional<TrackingSession> findByUser_UserIdAndStatus(Long userId, String status);

    boolean existsByBooking_BookingIdAndStatus(Long bookingId, String status);

    Optional<TrackingSession> findByBooking_BookingIdAndDirectionAndStatus(
            Long bookingId,
            String direction,
            String status
    );

    boolean existsByBooking_BookingIdAndDirectionAndStatus(Long bookingId, String direction, String status);
}
