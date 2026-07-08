package com.example.trekkingapp.route;

import com.example.trekkingapp.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/v1/routes")
public class RouteController {

    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RouteResponse>> createRoute(@Valid @RequestBody RouteRequest request) {
        RouteResponse response = routeService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Route created successfully", response));
    }

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('TOUR_PROVIDER', 'ADMIN')")
    public ResponseEntity<ApiResponse<RouteResponse>> generateRoute(@Valid @RequestBody RouteGenerateRequest request) {
        RouteResponse response = routeService.generate(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Route generated successfully", response));
    }

    @GetMapping
    public ApiResponse<List<RouteResponse>> getRoutes() {
        return new ApiResponse<>(true, "Routes retrieved successfully", routeService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RouteResponse>> getRoute(@PathVariable Long id) {
        return routeService.findById(id)
                .map(route -> ResponseEntity.ok(new ApiResponse<>(true, "Route retrieved successfully", route)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>(false, "Route not found", null)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RouteResponse>> updateRoute(
            @PathVariable Long id,
            @Valid @RequestBody RouteRequest request
    ) {
        return routeService.update(id, request)
                .map(route -> ResponseEntity.ok(new ApiResponse<>(true, "Route updated successfully", route)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>(false, "Route not found", null)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRoute(@PathVariable Long id) {
        if (!routeService.delete(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, "Route not found", null));
        }

        return ResponseEntity.ok(new ApiResponse<>(true, "Route deleted successfully", null));
    }
}
