package com.example.trekkingapp.tourschedule;

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
@RequestMapping("/api/v1/tours/{tourId}/schedules")
public class TourScheduleController {

    private final TourScheduleService tourScheduleService;

    public TourScheduleController(TourScheduleService tourScheduleService) {
        this.tourScheduleService = tourScheduleService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TOUR_PROVIDER', 'ADMIN')")
    public ApiResponse<TourScheduleResponse> createSchedule(
            @PathVariable Long tourId,
            @Valid @RequestBody TourScheduleRequest request
    ) {
        return new ApiResponse<>(true, "Tour schedule created successfully", tourScheduleService.createSchedule(tourId, request));
    }

    @GetMapping("/available")
    public ApiResponse<List<TourScheduleResponse>> getAvailableSchedules(@PathVariable Long tourId) {
        return new ApiResponse<>(true, "Tour schedules retrieved successfully", tourScheduleService.findAvailableSchedules(tourId));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('TOUR_PROVIDER', 'ADMIN')")
    public ApiResponse<List<TourScheduleResponse>> getMySchedules(@PathVariable Long tourId) {
        return new ApiResponse<>(true, "Tour schedules retrieved successfully", tourScheduleService.findMySchedules(tourId));
    }

    @PutMapping("/{scheduleId}")
    @PreAuthorize("hasAnyRole('TOUR_PROVIDER', 'ADMIN')")
    public ApiResponse<TourScheduleResponse> updateSchedule(
            @PathVariable Long tourId,
            @PathVariable Long scheduleId,
            @Valid @RequestBody TourScheduleRequest request
    ) {
        return new ApiResponse<>(true, "Tour schedule updated successfully", tourScheduleService.updateSchedule(tourId, scheduleId, request));
    }

    @DeleteMapping("/{scheduleId}")
    @PreAuthorize("hasAnyRole('TOUR_PROVIDER', 'ADMIN')")
    public ApiResponse<String> cancelSchedule(@PathVariable Long tourId, @PathVariable Long scheduleId) {
        return new ApiResponse<>(true, "Tour schedule cancelled successfully", tourScheduleService.cancelSchedule(tourId, scheduleId));
    }
}
