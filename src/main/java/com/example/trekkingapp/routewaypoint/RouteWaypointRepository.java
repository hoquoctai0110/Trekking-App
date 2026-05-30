package com.example.trekkingapp.routewaypoint;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RouteWaypointRepository extends JpaRepository<RouteWaypoint, Long> {

    List<RouteWaypoint> findByRoute_RouteIdOrderByOrderIndexAsc(Long routeId);

    Optional<RouteWaypoint> findByWaypointId(Long waypointId);

    Optional<RouteWaypoint> findByWaypointIdAndRoute_RouteId(Long waypointId, Long routeId);

    boolean existsByRoute_RouteIdAndOrderIndex(Long routeId, Integer orderIndex);

    List<RouteWaypoint> findByRoute_RouteIdAndCategoryOrderByOrderIndexAsc(Long routeId, String category);
}
