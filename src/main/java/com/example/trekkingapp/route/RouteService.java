package com.example.trekkingapp.route;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class RouteService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_DELETED = "DELETED";
    private static final String CREATED_TYPE_USER = "USER";

    private final RouteRepository routeRepository;

    public RouteService(RouteRepository routeRepository) {
        this.routeRepository = routeRepository;
    }

    @Transactional
    public RouteResponse create(RouteRequest request) {
        Route route = new Route();
        applyCreateFields(route, request);
        return toResponse(routeRepository.save(route));
    }

    @Transactional(readOnly = true)
    public List<RouteResponse> findAll() {
        return routeRepository.findByStatusNot(STATUS_DELETED)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<RouteResponse> findById(Long routeId) {
        return routeRepository.findByRouteIdAndStatusNot(routeId, STATUS_DELETED)
                .map(this::toResponse);
    }

    @Transactional
    public Optional<RouteResponse> update(Long routeId, RouteRequest request) {
        return routeRepository.findByRouteIdAndStatusNot(routeId, STATUS_DELETED)
                .map(route -> {
                    applyUpdateFields(route, request);
                    return toResponse(routeRepository.save(route));
                });
    }

    @Transactional
    public boolean delete(Long routeId) {
        return routeRepository.findByRouteIdAndStatusNot(routeId, STATUS_DELETED)
                .map(route -> {
                    route.setStatus(STATUS_DELETED);
                    routeRepository.save(route);
                    return true;
                })
                .orElse(false);
    }

    private void applyCreateFields(Route route, RouteRequest request) {
        route.setRouteName(request.routeName());
        route.setPolylineData(request.polylineData());
        route.setDistanceKm(request.distanceKm());
        route.setEstimatedDurationMin(request.estimatedDurationMin());
        route.setDifficulty(request.difficulty());
        route.setStatus(defaultIfBlank(request.status(), STATUS_ACTIVE));
        route.setCreatedBy(request.createdBy());
        route.setCreatedType(defaultIfBlank(request.createdType(), CREATED_TYPE_USER));
    }

    private void applyUpdateFields(Route route, RouteRequest request) {
        route.setRouteName(request.routeName());
        route.setPolylineData(request.polylineData());
        route.setDistanceKm(request.distanceKm());
        route.setEstimatedDurationMin(request.estimatedDurationMin());
        route.setDifficulty(request.difficulty());

        if (!isBlank(request.status())) {
            route.setStatus(request.status());
        }

        route.setCreatedBy(request.createdBy());

        if (!isBlank(request.createdType())) {
            route.setCreatedType(request.createdType());
        }
    }

    private RouteResponse toResponse(Route route) {
        return new RouteResponse(
                route.getRouteId(),
                route.getRouteName(),
                route.getPolylineData(),
                route.getDistanceKm(),
                route.getEstimatedDurationMin(),
                route.getDifficulty(),
                route.getStatus(),
                route.getCreatedBy(),
                route.getCreatedType(),
                route.getCreatedAt(),
                route.getUpdatedAt()
        );
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
