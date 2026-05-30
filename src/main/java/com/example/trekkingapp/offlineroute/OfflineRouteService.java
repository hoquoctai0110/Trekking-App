package com.example.trekkingapp.offlineroute;

import com.example.trekkingapp.auth.CurrentUserService;
import com.example.trekkingapp.route.Route;
import com.example.trekkingapp.route.RouteRepository;
import com.example.trekkingapp.routewaypoint.RouteWaypointRepository;
import com.example.trekkingapp.routewaypoint.RouteWaypointResponse;
import com.example.trekkingapp.user.User;
import com.example.trekkingapp.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OfflineRouteService {

    private static final String STATUS_DELETED = "DELETED";

    private final OfflineRouteRepository offlineRouteRepository;
    private final RouteRepository routeRepository;
    private final RouteWaypointRepository routeWaypointRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    public OfflineRouteService(
            OfflineRouteRepository offlineRouteRepository,
            RouteRepository routeRepository,
            RouteWaypointRepository routeWaypointRepository,
            UserRepository userRepository,
            CurrentUserService currentUserService
    ) {
        this.offlineRouteRepository = offlineRouteRepository;
        this.routeRepository = routeRepository;
        this.routeWaypointRepository = routeWaypointRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public OfflineRouteResponse download(Long routeId) {
        Long currentUserId = currentUserService.getCurrentUserId();
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("Current user not found"));
        Route route = findActiveRoute(routeId);

        OfflineRoute offlineRoute = offlineRouteRepository.findByUser_UserIdAndRoute_RouteId(currentUserId, routeId)
                .orElseGet(() -> {
                    OfflineRoute newOfflineRoute = new OfflineRoute();
                    newOfflineRoute.setUser(user);
                    newOfflineRoute.setRoute(route);
                    return newOfflineRoute;
                });

        offlineRoute.setRoute(route);
        offlineRoute.setLocalVersion(buildLocalVersion(route));
        offlineRoute.setLastSyncedAt(LocalDateTime.now());

        return toResponse(offlineRouteRepository.save(offlineRoute));
    }

    @Transactional(readOnly = true)
    public List<OfflineRouteResponse> findMyOfflineRoutes() {
        Long currentUserId = currentUserService.getCurrentUserId();
        return offlineRouteRepository.findByUser_UserId(currentUserId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public String delete(Long routeId) {
        Long currentUserId = currentUserService.getCurrentUserId();
        OfflineRoute offlineRoute = offlineRouteRepository.findByUser_UserIdAndRoute_RouteId(currentUserId, routeId)
                .orElseThrow(() -> new IllegalArgumentException("Offline route not found"));

        offlineRouteRepository.delete(offlineRoute);
        return "Offline route removed successfully";
    }

    private Route findActiveRoute(Long routeId) {
        return routeRepository.findByRouteIdAndStatusNot(routeId, STATUS_DELETED)
                .orElseThrow(() -> new IllegalArgumentException("Route not found"));
    }

    private OfflineRouteResponse toResponse(OfflineRoute offlineRoute) {
        Route route = offlineRoute.getRoute();
        List<RouteWaypointResponse> waypoints = routeWaypointRepository.findByRoute_RouteIdOrderByOrderIndexAsc(route.getRouteId())
                .stream()
                .map(waypoint -> new RouteWaypointResponse(
                        waypoint.getWaypointId(),
                        route.getRouteId(),
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
                ))
                .toList();

        return new OfflineRouteResponse(
                offlineRoute.getOfflineRouteId(),
                route.getRouteId(),
                route.getRouteName(),
                route.getPolylineData(),
                route.getDistanceKm(),
                route.getEstimatedDurationMin(),
                route.getDifficulty(),
                route.getStartLatitude(),
                route.getStartLongitude(),
                route.getEndLatitude(),
                route.getEndLongitude(),
                route.getElevationGain(),
                waypoints,
                offlineRoute.getDownloadedAt(),
                offlineRoute.getLastSyncedAt(),
                offlineRoute.getLocalVersion()
        );
    }

    private String buildLocalVersion(Route route) {
        return route.getRouteId() + ":" + route.getUpdatedAt();
    }
}
