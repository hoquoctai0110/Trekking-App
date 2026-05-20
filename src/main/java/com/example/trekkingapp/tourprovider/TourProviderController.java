package com.example.trekkingapp.tourprovider;

import com.example.trekkingapp.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/v1/tour-providers")
public class TourProviderController {

    private final TourProviderService tourProviderService;

    public TourProviderController(TourProviderService tourProviderService) {
        this.tourProviderService = tourProviderService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TourProviderResponse>> createTourProvider(
            @Valid @RequestBody TourProviderRequest request
    ) {
        TourProviderResponse response = tourProviderService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Tour provider created successfully", response));
    }

    @GetMapping
    public ApiResponse<List<TourProviderResponse>> getTourProviders() {
        return new ApiResponse<>(true, "Tour providers retrieved successfully", tourProviderService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TourProviderResponse>> getTourProvider(@PathVariable Long id) {
        return tourProviderService.findById(id)
                .map(tourProvider -> ResponseEntity.ok(
                        new ApiResponse<>(true, "Tour provider retrieved successfully", tourProvider)
                ))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>(false, "Tour provider not found", null)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TourProviderResponse>> updateTourProvider(
            @PathVariable Long id,
            @Valid @RequestBody TourProviderRequest request
    ) {
        return tourProviderService.update(id, request)
                .map(tourProvider -> ResponseEntity.ok(
                        new ApiResponse<>(true, "Tour provider updated successfully", tourProvider)
                ))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>(false, "Tour provider not found", null)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTourProvider(@PathVariable Long id) {
        if (!tourProviderService.delete(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, "Tour provider not found", null));
        }

        return ResponseEntity.ok(new ApiResponse<>(true, "Tour provider deleted successfully", null));
    }
}
