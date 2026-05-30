package com.example.trekkingapp.routewaypoint;

import com.example.trekkingapp.route.Route;
import com.example.trekkingapp.route.RouteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RouteWaypointService {

    private static final String STATUS_DELETED = "DELETED";

    private final RouteWaypointRepository routeWaypointRepository;
    private final RouteRepository routeRepository;

    public RouteWaypointService(RouteWaypointRepository routeWaypointRepository, RouteRepository routeRepository) {
        this.routeWaypointRepository = routeWaypointRepository;
        this.routeRepository = routeRepository;
    }

    @Transactional
    public RouteWaypointResponse create(Long routeId, RouteWaypointRequest request) {
        Route route = findActiveRoute(routeId);

        RouteWaypoint waypoint = new RouteWaypoint();
        waypoint.setRoute(route);
        applyRequest(waypoint, request);

        return toResponse(routeWaypointRepository.save(waypoint));
    }

    @Transactional(readOnly = true)
    public List<RouteWaypointResponse> findByRoute(Long routeId) {
        findActiveRoute(routeId);
        return routeWaypointRepository.findByRoute_RouteIdOrderByOrderIndexAsc(routeId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public RouteWaypointResponse update(Long routeId, Long waypointId, RouteWaypointRequest request) {
        findActiveRoute(routeId);
        RouteWaypoint waypoint = findWaypoint(routeId, waypointId);
        applyRequest(waypoint, request);
        return toResponse(routeWaypointRepository.save(waypoint));
    }

    @Transactional
    public String delete(Long routeId, Long waypointId) {
        findActiveRoute(routeId);
        RouteWaypoint waypoint = findWaypoint(routeId, waypointId);
        routeWaypointRepository.delete(waypoint);
        return "Route waypoint deleted successfully";
    }

    public RouteWaypointResponse toResponse(RouteWaypoint waypoint) {
        return new RouteWaypointResponse(
                waypoint.getWaypointId(),
                waypoint.getRoute().getRouteId(),
                waypoint.getName(),
                waypoint.getLatitude(),
                waypoint.getLongitude(),
                waypoint.getType(),
                waypoint.getDescription(),
                waypoint.getOrderIndex(),
                waypoint.getCreatedAt(),
                waypoint.getUpdatedAt()
        );
    }

    private Route findActiveRoute(Long routeId) {
        return routeRepository.findByRouteIdAndStatusNot(routeId, STATUS_DELETED)
                .orElseThrow(() -> new IllegalArgumentException("Route not found"));
    }

    private RouteWaypoint findWaypoint(Long routeId, Long waypointId) {
        return routeWaypointRepository.findByWaypointIdAndRoute_RouteId(waypointId, routeId)
                .orElseThrow(() -> new IllegalArgumentException("Route waypoint not found"));
    }

    private void applyRequest(RouteWaypoint waypoint, RouteWaypointRequest request) {
        waypoint.setName(request.name());
        waypoint.setLatitude(request.latitude());
        waypoint.setLongitude(request.longitude());
        waypoint.setType(request.type());
        waypoint.setDescription(request.description());
        waypoint.setOrderIndex(request.orderIndex());
    }
}
