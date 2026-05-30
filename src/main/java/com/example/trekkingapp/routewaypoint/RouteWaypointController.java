package com.example.trekkingapp.routewaypoint;

import com.example.trekkingapp.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/routes/{routeId}/waypoints")
public class RouteWaypointController {

    private final RouteWaypointService routeWaypointService;

    public RouteWaypointController(RouteWaypointService routeWaypointService) {
        this.routeWaypointService = routeWaypointService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TOUR_PROVIDER')")
    public ApiResponse<RouteWaypointResponse> createWaypoint(
            @PathVariable Long routeId,
            @Valid @RequestBody RouteWaypointRequest request
    ) {
        return new ApiResponse<>(true, "Route waypoint created successfully", routeWaypointService.create(routeId, request));
    }

    @GetMapping
    public ApiResponse<List<RouteWaypointResponse>> getWaypoints(@PathVariable Long routeId) {
        return new ApiResponse<>(true, "Route waypoints retrieved successfully", routeWaypointService.findByRoute(routeId));
    }

    @GetMapping("/mobile")
    public ApiResponse<List<MobileRouteWaypointResponse>> getMobileWaypoints(@PathVariable Long routeId) {
        return new ApiResponse<>(true, "Mobile route waypoints retrieved successfully", routeWaypointService.findMobileByRoute(routeId));
    }

    @GetMapping("/{waypointId}")
    public ApiResponse<RouteWaypointResponse> getWaypoint(@PathVariable Long routeId, @PathVariable Long waypointId) {
        return new ApiResponse<>(true, "Route waypoint retrieved successfully", routeWaypointService.findById(routeId, waypointId));
    }

    @GetMapping("/category/{category}")
    public ApiResponse<List<RouteWaypointResponse>> getWaypointsByCategory(
            @PathVariable Long routeId,
            @PathVariable String category
    ) {
        return new ApiResponse<>(true, "Route waypoints retrieved successfully", routeWaypointService.findByCategory(routeId, category));
    }

    @PutMapping("/{waypointId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TOUR_PROVIDER')")
    public ApiResponse<RouteWaypointResponse> updateWaypoint(
            @PathVariable Long routeId,
            @PathVariable Long waypointId,
            @Valid @RequestBody RouteWaypointRequest request
    ) {
        return new ApiResponse<>(true, "Route waypoint updated successfully", routeWaypointService.update(routeId, waypointId, request));
    }

    @DeleteMapping("/{waypointId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TOUR_PROVIDER')")
    public ApiResponse<String> deleteWaypoint(@PathVariable Long routeId, @PathVariable Long waypointId) {
        return new ApiResponse<>(true, "Route waypoint deleted successfully", routeWaypointService.delete(routeId, waypointId));
    }
}
