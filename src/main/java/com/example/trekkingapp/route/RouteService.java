package com.example.trekkingapp.route;

import com.example.trekkingapp.auth.CurrentUserService;
import com.example.trekkingapp.routewaypoint.RouteWaypoint;
import com.example.trekkingapp.routewaypoint.RouteWaypointRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class RouteService {

    private static final Logger log = LoggerFactory.getLogger(RouteService.class);

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_DELETED = "DELETED";
    private static final String CREATED_TYPE_USER = "USER";

    private final RouteRepository routeRepository;
    private final RouteWaypointRepository routeWaypointRepository;
    private final MapboxDirectionsClient mapboxDirectionsClient;
    private final CurrentUserService currentUserService;

    public RouteService(RouteRepository routeRepository,
                        RouteWaypointRepository routeWaypointRepository,
                        MapboxDirectionsClient mapboxDirectionsClient,
                        CurrentUserService currentUserService) {
        this.routeRepository = routeRepository;
        this.routeWaypointRepository = routeWaypointRepository;
        this.mapboxDirectionsClient = mapboxDirectionsClient;
        this.currentUserService = currentUserService;
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
        route.setRouteType(normalizeRouteType(request.routeType()).name());
        route.setCreatedBy(request.createdBy());
        route.setCreatedType(defaultIfBlank(request.createdType(), CREATED_TYPE_USER));
        applyRouteShapeFields(route, request);
    }

    @Transactional
    public RouteResponse generate(RouteGenerateRequest request) {
        RouteType routeType = normalizeRouteType(request.routeType());
        validatePoint(request.start(), "start");

        List<RoutePointRequest> checkpoints = request.checkpoints() == null
                ? Collections.emptyList()
                : request.checkpoints();

        validateGenerateRequest(routeType, request, checkpoints);

        RoutePointRequest mapboxEnd = routeType == RouteType.LOOP ? request.start() : request.end();
        List<RoutePointRequest> mapboxCheckpoints = routeType == RouteType.ROUND_TRIP
                ? Collections.emptyList()
                : checkpoints;
        for (RoutePointRequest checkpoint : checkpoints) {
            validatePoint(checkpoint, "checkpoint");
        }

        log.info("[RouteGenerate] routeType = {}", routeType);

        MapboxDirectionsResponse directionsResponse = mapboxDirectionsClient.getWalkingRoute(
                request.start(),
                mapboxCheckpoints,
                mapboxEnd
        );

        MapboxDirectionsResponse.MapboxRouteData routeData = extractRouteData(directionsResponse);
        GeneratedRoute generatedRoute = buildGeneratedRoute(routeType, routeData, request.start(), mapboxEnd);

        Route route = new Route();
        route.setRouteName(request.routeName());
        route.setPolylineData(generatedRoute.polylineData());
        route.setDistanceKm(generatedRoute.distanceKm());
        route.setEstimatedDurationMin(generatedRoute.estimatedDurationMin());
        route.setDifficulty(request.difficulty());
        route.setStatus(STATUS_ACTIVE);
        route.setRouteType(routeType.name());
        route.setCreatedBy(currentUserService.getCurrentUserId());
        route.setCreatedType(CREATED_TYPE_USER);
        route.setStartLatitude(request.start().latitude());
        route.setStartLongitude(request.start().longitude());
        route.setEndLatitude(generatedRoute.end().latitude());
        route.setEndLongitude(generatedRoute.end().longitude());

        log.info("[RouteGenerate] original distance = {}", routeData.distance() / 1000D);
        log.info("[RouteGenerate] final distance = {}", generatedRoute.distanceKm());
        log.info("[RouteGenerate] coordinates count = {}", generatedRoute.coordinatesCount());

        Route savedRoute = routeRepository.save(route);
        routeWaypointRepository.saveAll(buildGeneratedWaypoints(
                savedRoute,
                routeType,
                request.start(),
                checkpoints,
                request.end()
        ));

        return toResponse(savedRoute);
    }

    private void applyUpdateFields(Route route, RouteRequest request) {
        route.setRouteName(request.routeName());
        route.setPolylineData(request.polylineData());
        route.setDistanceKm(request.distanceKm());
        route.setEstimatedDurationMin(request.estimatedDurationMin());
        route.setDifficulty(request.difficulty());
        route.setRouteType(normalizeRouteType(request.routeType()).name());
        applyRouteShapeFields(route, request);

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
                defaultIfBlank(route.getRouteType(), RouteType.ONE_WAY.name()),
                route.getCreatedBy(),
                route.getCreatedType(),
                route.getStartLatitude(),
                route.getStartLongitude(),
                route.getEndLatitude(),
                route.getEndLongitude(),
                route.getElevationGain(),
                route.getCreatedAt(),
                route.getUpdatedAt()
        );
    }

    private void applyRouteShapeFields(Route route, RouteRequest request) {
        route.setStartLatitude(request.startLatitude());
        route.setStartLongitude(request.startLongitude());
        route.setEndLatitude(request.endLatitude());
        route.setEndLongitude(request.endLongitude());
        route.setElevationGain(request.elevationGain());
    }

    private MapboxDirectionsResponse.MapboxRouteData extractRouteData(MapboxDirectionsResponse directionsResponse) {
        if (directionsResponse == null
                || directionsResponse.routes() == null
                || directionsResponse.routes().isEmpty()) {
            throw new IllegalArgumentException("Mapbox returned no route for the provided points");
        }

        MapboxDirectionsResponse.MapboxRouteData routeData = directionsResponse.routes().getFirst();
        if (routeData.geometry() == null || routeData.geometry().isBlank()) {
            throw new IllegalArgumentException("Mapbox returned no route geometry for the provided points");
        }

        if (routeData.distance() == null || routeData.duration() == null) {
            throw new IllegalArgumentException("Mapbox route response is missing distance or duration");
        }

        return routeData;
    }

    private List<RouteWaypoint> buildGeneratedWaypoints(Route route,
                                                        RouteType routeType,
                                                        RoutePointRequest start,
                                                        List<RoutePointRequest> checkpoints,
                                                        RoutePointRequest end) {
        List<RouteWaypoint> waypoints = new ArrayList<>();
        int orderIndex = 0;

        waypoints.add(createGeneratedWaypoint(route, start, "START", orderIndex++));

        if (routeType == RouteType.ROUND_TRIP) {
            waypoints.add(createGeneratedWaypoint(route, end, "CHECKPOINT", orderIndex++));
            waypoints.add(createGeneratedWaypoint(route, start, "END", orderIndex));
            return waypoints;
        }

        for (RoutePointRequest checkpoint : checkpoints) {
            waypoints.add(createGeneratedWaypoint(route, checkpoint, "CHECKPOINT", orderIndex++));
        }

        RoutePointRequest finalPoint = routeType == RouteType.LOOP ? start : end;
        waypoints.add(createGeneratedWaypoint(route, finalPoint, "END", orderIndex));
        return waypoints;
    }

    private RouteWaypoint createGeneratedWaypoint(Route route,
                                                  RoutePointRequest point,
                                                  String category,
                                                  int orderIndex) {
        RouteWaypoint waypoint = new RouteWaypoint();
        waypoint.setRoute(route);
        waypoint.setName(point.name());
        waypoint.setLatitude(point.latitude());
        waypoint.setLongitude(point.longitude());
        waypoint.setCategory(category);
        waypoint.setOrderIndex(orderIndex);
        waypoint.setMandatory(Boolean.TRUE);
        return waypoint;
    }

    private void validatePoint(RoutePointRequest point, String fieldName) {
        if (point == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }

        if (point.latitude() == null || point.latitude().isNaN() || point.latitude().isInfinite()
                || point.latitude() < -90 || point.latitude() > 90) {
            throw new IllegalArgumentException("Invalid latitude");
        }

        if (point.longitude() == null || point.longitude().isNaN() || point.longitude().isInfinite()
                || point.longitude() < -180 || point.longitude() > 180) {
            throw new IllegalArgumentException("Invalid longitude");
        }
    }

    private void validateGenerateRequest(RouteType routeType,
                                         RouteGenerateRequest request,
                                         List<RoutePointRequest> checkpoints) {
        if ((routeType == RouteType.ONE_WAY || routeType == RouteType.ROUND_TRIP) && request.end() == null) {
            throw new IllegalArgumentException("end is required");
        }

        if (routeType == RouteType.ROUND_TRIP) {
            validatePoint(request.end(), "end");
        }

        if (routeType == RouteType.ONE_WAY) {
            validatePoint(request.end(), "end");
        }

        if (routeType == RouteType.LOOP && checkpoints.isEmpty()) {
            throw new IllegalArgumentException("LOOP route requires at least one checkpoint");
        }
    }

    private GeneratedRoute buildGeneratedRoute(RouteType routeType,
                                               MapboxDirectionsResponse.MapboxRouteData routeData,
                                               RoutePointRequest start,
                                               RoutePointRequest end) {
        double originalDistanceKm = routeData.distance() / 1000D;
        int originalDurationMin = (int) Math.round(routeData.duration() / 60D);

        if (routeType != RouteType.ROUND_TRIP) {
            int coordinatesCount = PolylineCodec.decode(routeData.geometry()).size();
            return new GeneratedRoute(routeData.geometry(), originalDistanceKm, originalDurationMin, end, coordinatesCount);
        }

        List<PolylineCodec.Coordinate> coordinates = PolylineCodec.decode(routeData.geometry());
        if (coordinates.isEmpty()) {
            throw new IllegalArgumentException("Mapbox returned no route coordinates");
        }

        List<PolylineCodec.Coordinate> roundTripCoordinates = new ArrayList<>(coordinates);
        for (int i = coordinates.size() - 2; i >= 0; i--) {
            roundTripCoordinates.add(coordinates.get(i));
        }

        return new GeneratedRoute(
                PolylineCodec.encode(roundTripCoordinates),
                originalDistanceKm * 2,
                originalDurationMin * 2,
                start,
                roundTripCoordinates.size()
        );
    }

    private RouteType normalizeRouteType(String routeType) {
        if (isBlank(routeType)) {
            return RouteType.ONE_WAY;
        }

        try {
            return RouteType.valueOf(routeType.toUpperCase(Locale.ROOT));
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Unsupported routeType");
        }
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record GeneratedRoute(
            String polylineData,
            Double distanceKm,
            Integer estimatedDurationMin,
            RoutePointRequest end,
            int coordinatesCount
    ) {
    }
}
