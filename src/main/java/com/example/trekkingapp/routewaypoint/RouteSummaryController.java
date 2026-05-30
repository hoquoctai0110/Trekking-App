package com.example.trekkingapp.routewaypoint;

import com.example.trekkingapp.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/routes")
public class RouteSummaryController {

    private final RouteWaypointService routeWaypointService;

    public RouteSummaryController(RouteWaypointService routeWaypointService) {
        this.routeWaypointService = routeWaypointService;
    }

    @GetMapping("/{routeId}/summary")
    public ApiResponse<RouteSummaryResponse> getRouteSummary(@PathVariable Long routeId) {
        return new ApiResponse<>(true, "Route summary retrieved successfully", routeWaypointService.getRouteSummary(routeId));
    }
}
