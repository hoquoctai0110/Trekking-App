package com.example.trekkingapp.routewaypoint;

import com.example.trekkingapp.route.Route;
import com.example.trekkingapp.route.RouteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class RouteWaypointService {

    private static final String STATUS_DELETED = "DELETED";
    private static final Set<WaypointCategory> SUPPORTED_CATEGORIES = EnumSet.allOf(WaypointCategory.class);

    private final RouteWaypointRepository routeWaypointRepository;
    private final RouteRepository routeRepository;

    public RouteWaypointService(RouteWaypointRepository routeWaypointRepository, RouteRepository routeRepository) {
        this.routeWaypointRepository = routeWaypointRepository;
        this.routeRepository = routeRepository;
    }

    @Transactional
    public RouteWaypointResponse create(Long routeId, RouteWaypointRequest request) {
        Route route = findActiveRoute(routeId);
        validateRequest(routeId, null, request);

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

    @Transactional(readOnly = true)
    public RouteWaypointResponse findById(Long routeId, Long waypointId) {
        findActiveRoute(routeId);
        return toResponse(findWaypoint(routeId, waypointId));
    }

    @Transactional(readOnly = true)
    public List<RouteWaypointResponse> findByCategory(Long routeId, String category) {
        findActiveRoute(routeId);
        String normalizedCategory = normalizeCategory(category);
        return routeWaypointRepository.findByRoute_RouteIdAndCategoryOrderByOrderIndexAsc(routeId, normalizedCategory)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MobileRouteWaypointResponse> findMobileByRoute(Long routeId) {
        findActiveRoute(routeId);
        return routeWaypointRepository.findByRoute_RouteIdOrderByOrderIndexAsc(routeId)
                .stream()
                .map(this::toMobileResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RouteSummaryResponse getRouteSummary(Long routeId) {
        Route route = findActiveRoute(routeId);
        List<RouteWaypoint> waypoints = routeWaypointRepository.findByRoute_RouteIdOrderByOrderIndexAsc(routeId);

        RouteWaypointResponse startWaypoint = waypoints.stream()
                .filter(waypoint -> WaypointCategory.START.name().equals(waypoint.getCategory()))
                .findFirst()
                .map(this::toResponse)
                .orElse(null);

        RouteWaypointResponse endWaypoint = waypoints.stream()
                .filter(waypoint -> WaypointCategory.END.name().equals(waypoint.getCategory()))
                .findFirst()
                .map(this::toResponse)
                .orElse(null);

        return new RouteSummaryResponse(
                route.getRouteId(),
                route.getRouteName(),
                waypoints.size(),
                countByCategory(waypoints, WaypointCategory.CAMP),
                countByCategory(waypoints, WaypointCategory.WATER),
                countByCategory(waypoints, WaypointCategory.DANGER),
                startWaypoint,
                endWaypoint,
                route.getEstimatedDurationMin(),
                route.getDistanceKm(),
                route.getElevationGain()
        );
    }

    @Transactional
    public RouteWaypointResponse update(Long routeId, Long waypointId, RouteWaypointRequest request) {
        findActiveRoute(routeId);
        RouteWaypoint waypoint = findWaypoint(routeId, waypointId);
        validateRequest(routeId, waypointId, request);
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
                waypoint.getDescription(),
                waypoint.getLatitude(),
                waypoint.getLongitude(),
                waypoint.getCategory(),
                waypoint.getOrderIndex(),
                waypoint.getElevation(),
                waypoint.getDistanceFromStartKm(),
                waypoint.getEstimatedArrivalMinute(),
                waypoint.getMandatory(),
                waypoint.getIconKey(),
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
        waypoint.setDescription(request.description());
        waypoint.setLatitude(request.latitude());
        waypoint.setLongitude(request.longitude());
        waypoint.setCategory(normalizeCategory(request.category()));
        waypoint.setOrderIndex(request.orderIndex());
        waypoint.setElevation(request.elevation());
        waypoint.setDistanceFromStartKm(request.distanceFromStartKm());
        waypoint.setEstimatedArrivalMinute(request.estimatedArrivalMinute());
        waypoint.setMandatory(Boolean.TRUE.equals(request.mandatory()));
        waypoint.setIconKey(request.iconKey());
    }

    private MobileRouteWaypointResponse toMobileResponse(RouteWaypoint waypoint) {
        return new MobileRouteWaypointResponse(
                waypoint.getWaypointId(),
                waypoint.getName(),
                waypoint.getLatitude(),
                waypoint.getLongitude(),
                waypoint.getCategory(),
                waypoint.getIconKey(),
                waypoint.getOrderIndex()
        );
    }

    private void validateRequest(Long routeId, Long waypointId, RouteWaypointRequest request) {
        if (request.latitude() < -90 || request.latitude() > 90) {
            throw new IllegalArgumentException("Invalid latitude");
        }

        if (request.longitude() < -180 || request.longitude() > 180) {
            throw new IllegalArgumentException("Invalid longitude");
        }

        normalizeCategory(request.category());

        boolean orderChanged = true;
        if (waypointId != null) {
            RouteWaypoint existingWaypoint = routeWaypointRepository.findByWaypointId(waypointId)
                    .orElseThrow(() -> new IllegalArgumentException("Route waypoint not found"));
            orderChanged = !existingWaypoint.getOrderIndex().equals(request.orderIndex());
        }

        if (orderChanged && routeWaypointRepository.existsByRoute_RouteIdAndOrderIndex(routeId, request.orderIndex())) {
            throw new IllegalArgumentException("Waypoint order already exists");
        }
    }

    private String normalizeCategory(String category) {
        try {
            WaypointCategory waypointCategory = WaypointCategory.valueOf(category.toUpperCase(Locale.ROOT));
            if (!SUPPORTED_CATEGORIES.contains(waypointCategory)) {
                throw new IllegalArgumentException("Unsupported waypoint category");
            }
            return waypointCategory.name();
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Unsupported waypoint category");
        }
    }

    private long countByCategory(List<RouteWaypoint> waypoints, WaypointCategory category) {
        return waypoints.stream()
                .filter(waypoint -> category.name().equals(waypoint.getCategory()))
                .count();
    }

    private enum WaypointCategory {
        START,
        CHECKPOINT,
        END,
        CAMP,
        WATER,
        VIEWPOINT,
        EMERGENCY,
        DANGER,
        SHELTER
    }
}
