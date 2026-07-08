package com.example.trekkingapp.review;

import com.example.trekkingapp.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/reviews")
    @PreAuthorize("hasRole('TREKKER')")
    public ApiResponse<ReviewResponse> createReview(@Valid @RequestBody ReviewRequest request) {
        return new ApiResponse<>(true, "Review created successfully", reviewService.createReview(request));
    }

    @GetMapping("/tours/{tourId}/reviews")
    public ApiResponse<List<ReviewResponse>> getTourReviews(@PathVariable Long tourId) {
        return new ApiResponse<>(true, "Reviews retrieved successfully", reviewService.getReviewsByTour(tourId));
    }

    @GetMapping("/tours/{tourId}/reviews/summary")
    public ApiResponse<ReviewSummaryResponse> getTourReviewSummary(@PathVariable Long tourId) {
        return new ApiResponse<>(true, "Review summary retrieved successfully", reviewService.getTourReviewSummary(tourId));
    }
}
