package com.example.trekkingapp.tracking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrackingPointRepository extends JpaRepository<TrackingPoint, Long> {

    List<TrackingPoint> findBySession_SessionIdOrderByRecordedAtAsc(Long sessionId);
}
