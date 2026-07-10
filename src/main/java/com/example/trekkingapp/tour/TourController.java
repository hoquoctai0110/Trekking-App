package com.example.trekkingapp.tour;

import com.example.trekkingapp.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tours")
public class TourController {

    private final TourService tourService;

    public TourController(TourService tourService) {
        this.tourService = tourService;
    }

    @PostMapping("/me")
    @PreAuthorize("hasRole('TOUR_PROVIDER')")
    public ApiResponse<TourResponse> createMyTour(@Valid @RequestBody TourRequest request) {
        return new ApiResponse<>(true, "Tour created successfully", tourService.createMyTour(request));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('TOUR_PROVIDER')")
    public ApiResponse<List<TourResponse>> getMyTours() {
        return new ApiResponse<>(true, "Tours retrieved successfully", tourService.findMyTours());
    }

    @PutMapping("/me/{tourId}")
    @PreAuthorize("hasRole('TOUR_PROVIDER')")
    public ApiResponse<TourResponse> updateMyTour(
            @PathVariable Long tourId,
            @Valid @RequestBody TourRequest request
    ) {
        return new ApiResponse<>(true, "Tour updated successfully", tourService.updateMyTour(tourId, request));
    }

    @DeleteMapping("/me/{tourId}")
    @PreAuthorize("hasRole('TOUR_PROVIDER')")
    public ApiResponse<String> deleteMyTour(@PathVariable Long tourId) {
        return new ApiResponse<>(true, "Tour deleted successfully", tourService.deleteMyTour(tourId));
    }

    @GetMapping
    public ApiResponse<List<TourResponse>> getTours() {
        return new ApiResponse<>(true, "Tours retrieved successfully", tourService.findPublishedTours());
    }

    @GetMapping("/provider/{providerId}")
    public ApiResponse<List<TourResponse>> getToursByProvider(@PathVariable Long providerId) {
        return new ApiResponse<>(true, "Tours retrieved successfully", tourService.findByProviderId(providerId));
    }

    @GetMapping("/{tourId}")
    public ApiResponse<TourResponse> getTour(@PathVariable Long tourId) {
        return new ApiResponse<>(true, "Tour retrieved successfully", tourService.findById(tourId));
    }

    @PostMapping("/{tourId}/images")
    @PreAuthorize("hasAnyRole('TOUR_PROVIDER', 'ADMIN')")
    public ApiResponse<List<TourImageResponse>> uploadTourImages(
            @PathVariable Long tourId,
            @RequestParam("files") List<MultipartFile> files
    ) {
        return new ApiResponse<>(true, "Tour images uploaded successfully", tourService.uploadTourImages(tourId, files));
    }

    @GetMapping("/{tourId}/images")
    public ApiResponse<List<TourImageResponse>> getTourImages(@PathVariable Long tourId) {
        return new ApiResponse<>(true, "Tour images retrieved successfully", tourService.getTourImages(tourId));
    }

    @DeleteMapping("/{tourId}/images/{imageId}")
    @PreAuthorize("hasAnyRole('TOUR_PROVIDER', 'ADMIN')")
    public ApiResponse<String> deleteTourImage(@PathVariable Long tourId, @PathVariable Long imageId) {
        return new ApiResponse<>(true, "Tour image deleted successfully", tourService.deleteTourImage(tourId, imageId));
    }

    @PatchMapping("/{tourId}/images/{imageId}/cover")
    @PreAuthorize("hasAnyRole('TOUR_PROVIDER', 'ADMIN')")
    public ApiResponse<TourImageResponse> setCoverImage(@PathVariable Long tourId, @PathVariable Long imageId) {
        return new ApiResponse<>(true, "Tour cover image updated successfully", tourService.setCoverImage(tourId, imageId));
    }
}
