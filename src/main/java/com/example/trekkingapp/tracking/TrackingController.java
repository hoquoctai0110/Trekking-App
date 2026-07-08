package com.example.trekkingapp.tracking;

import com.example.trekkingapp.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tracking")
public class TrackingController {

    private final TrackingService trackingService;

    public TrackingController(TrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @PostMapping("/sessions/start")
    @PreAuthorize("hasRole('TREKKER')")
    public ApiResponse<TrackingSessionResponse> startSession(@Valid @RequestBody TrackingSessionRequest request) {
        return new ApiResponse<>(true, "Tracking session started successfully", trackingService.startSession(request));
    }

    @PostMapping("/start")
    @PreAuthorize("hasRole('TREKKER')")
    public ApiResponse<TrackingStartResponse> start(@Valid @RequestBody TrackingSessionRequest request) {
        TrackingSessionResponse session = trackingService.startSession(request);
        return new ApiResponse<>(
                true,
                "Tracking session started successfully",
                new TrackingStartResponse(session.trackingSessionId(), session.direction())
        );
    }

    @PostMapping("/location")
    @PreAuthorize("hasRole('TREKKER')")
    public ApiResponse<TrackingPointResponse> addLocation(@Valid @RequestBody TrackingLocationRequest request) {
        return new ApiResponse<>(true, "Tracking point added successfully", trackingService.addLocation(request));
    }

    @PostMapping("/location/batch")
    @PreAuthorize("hasRole('TREKKER')")
    public ApiResponse<TrackingLocationBatchResponse> syncLocations(
            @Valid @RequestBody TrackingLocationBatchRequest request
    ) {
        return new ApiResponse<>(true, "Tracking points synced successfully", trackingService.syncLocations(request));
    }

    @PostMapping("/sessions/{sessionId}/points")
    @PreAuthorize("hasRole('TREKKER')")
    public ApiResponse<TrackingPointResponse> addPoint(
            @PathVariable Long sessionId,
            @Valid @RequestBody TrackingPointRequest request
    ) {
        return new ApiResponse<>(true, "Tracking point added successfully", trackingService.addPoint(sessionId, request));
    }

    @GetMapping("/sessions/me")
    @PreAuthorize("hasRole('TREKKER')")
    public ApiResponse<List<TrackingSessionResponse>> getMySessions() {
        return new ApiResponse<>(true, "Tracking sessions retrieved successfully", trackingService.findMySessions());
    }

    @GetMapping("/sessions/{sessionId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<TrackingSessionResponse> getSession(@PathVariable Long sessionId) {
        return new ApiResponse<>(true, "Tracking session retrieved successfully", trackingService.findSessionById(sessionId));
    }

    @GetMapping("/session/{sessionId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<TrackingSessionResponse> getSessionAlias(@PathVariable Long sessionId) {
        return new ApiResponse<>(true, "Tracking session retrieved successfully", trackingService.findSessionById(sessionId));
    }

    @GetMapping("/sessions/{sessionId}/points")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<TrackingPointResponse>> getSessionPoints(@PathVariable Long sessionId) {
        return new ApiResponse<>(true, "Tracking points retrieved successfully", trackingService.findSessionPoints(sessionId));
    }

    @GetMapping("/session/{sessionId}/history")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<TrackingPointResponse>> getSessionHistory(@PathVariable Long sessionId) {
        return new ApiResponse<>(true, "Tracking points retrieved successfully", trackingService.findSessionPoints(sessionId));
    }

    @GetMapping("/session/{sessionId}/latest")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<TrackingPointResponse> getLatestPoint(@PathVariable Long sessionId) {
        return new ApiResponse<>(true, "Latest tracking point retrieved successfully", trackingService.findLatestPoint(sessionId));
    }

    @PutMapping("/sessions/{sessionId}/pause")
    @PreAuthorize("hasRole('TREKKER')")
    public ApiResponse<TrackingSessionResponse> pauseSession(@PathVariable Long sessionId) {
        return new ApiResponse<>(true, "Tracking session paused successfully", trackingService.pauseSession(sessionId));
    }

    @PutMapping("/sessions/{sessionId}/resume")
    @PreAuthorize("hasRole('TREKKER')")
    public ApiResponse<TrackingSessionResponse> resumeSession(@PathVariable Long sessionId) {
        return new ApiResponse<>(true, "Tracking session resumed successfully", trackingService.resumeSession(sessionId));
    }

    @PutMapping("/sessions/{sessionId}/finish")
    @PreAuthorize("hasRole('TREKKER')")
    public ApiResponse<TrackingSessionResponse> finishSession(@PathVariable Long sessionId) {
        return new ApiResponse<>(true, "Tracking session finished successfully", trackingService.finishSession(sessionId));
    }

    @PostMapping("/session/{sessionId}/complete")
    @PreAuthorize("hasRole('TREKKER')")
    public ApiResponse<TrackingSessionResponse> completeSession(@PathVariable Long sessionId) {
        return new ApiResponse<>(true, "Tracking session completed successfully", trackingService.finishSession(sessionId));
    }
}
