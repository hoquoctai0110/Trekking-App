package com.example.trekkingapp.offlineroute;

import com.example.trekkingapp.common.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/offline-routes")
public class OfflineRouteController {

    private final OfflineRouteService offlineRouteService;

    public OfflineRouteController(OfflineRouteService offlineRouteService) {
        this.offlineRouteService = offlineRouteService;
    }

    @PostMapping("/{routeId}/download")
    @PreAuthorize("hasAnyRole('TREKKER', 'TOUR_PROVIDER')")
    public ApiResponse<OfflineRouteResponse> downloadRoute(@PathVariable Long routeId) {
        return new ApiResponse<>(true, "Offline route downloaded successfully", offlineRouteService.download(routeId));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<OfflineRouteResponse>> getMyOfflineRoutes() {
        return new ApiResponse<>(true, "Offline routes retrieved successfully", offlineRouteService.findMyOfflineRoutes());
    }

    @DeleteMapping("/{routeId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<String> deleteOfflineRoute(@PathVariable Long routeId) {
        return new ApiResponse<>(true, "Offline route removed successfully", offlineRouteService.delete(routeId));
    }
}
