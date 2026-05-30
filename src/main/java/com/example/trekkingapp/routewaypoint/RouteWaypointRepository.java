package com.example.trekkingapp.routewaypoint;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RouteWaypointRepository extends JpaRepository<RouteWaypoint, Long> {

    List<RouteWaypoint> findByRoute_RouteIdOrderByOrderIndexAsc(Long routeId);

    Optional<RouteWaypoint> findByWaypointIdAndRoute_RouteId(Long waypointId, Long routeId);
}
