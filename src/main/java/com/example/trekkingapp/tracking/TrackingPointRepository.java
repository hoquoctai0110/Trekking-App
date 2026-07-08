package com.example.trekkingapp.tracking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TrackingPointRepository extends JpaRepository<TrackingPoint, Long> {

    List<TrackingPoint> findBySession_SessionIdOrderByRecordedAtAsc(Long sessionId);

    Optional<TrackingPoint> findTopBySession_SessionIdOrderByRecordedAtDescPointIdDesc(Long sessionId);

    boolean existsBySession_SessionIdAndRecordedAtAndLatitudeAndLongitude(
            Long sessionId,
            LocalDateTime recordedAt,
            Double latitude,
            Double longitude
    );
}
