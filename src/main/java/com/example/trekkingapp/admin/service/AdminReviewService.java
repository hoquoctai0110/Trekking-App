package com.example.trekkingapp.admin.service;

import com.example.trekkingapp.admin.AdminSortUtils;
import com.example.trekkingapp.admin.audit.AdminAuditService;
import com.example.trekkingapp.admin.dto.request.AdminReviewFlagRequest;
import com.example.trekkingapp.admin.dto.request.AdminReviewVisibilityRequest;
import com.example.trekkingapp.admin.dto.response.AdminReviewResponse;
import com.example.trekkingapp.admin.dto.response.PageResponse;
import com.example.trekkingapp.admin.specification.AdminReviewSpecification;
import com.example.trekkingapp.common.ResourceNotFoundException;
import com.example.trekkingapp.review.Review;
import com.example.trekkingapp.review.ReviewRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

@Service
public class AdminReviewService {

    private static final Set<String> SORT_FIELDS = Set.of("createdAt", "updatedAt", "rating");

    private final ReviewRepository reviewRepository;
    private final AdminAuditService adminAuditService;

    public AdminReviewService(ReviewRepository reviewRepository, AdminAuditService adminAuditService) {
        this.reviewRepository = reviewRepository;
        this.adminAuditService = adminAuditService;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminReviewResponse> findReviews(int page, int size, String sort, String search, Integer rating, Boolean flagged, Boolean visible, Long tourId, Long userId) {
        Pageable pageable = AdminSortUtils.pageable(page, size, sort, "createdAt", SORT_FIELDS);
        Specification<Review> specification = Specification
                .where(AdminReviewSpecification.notDeleted())
                .and(AdminReviewSpecification.search(search))
                .and(AdminReviewSpecification.rating(rating))
                .and(AdminReviewSpecification.flagged(flagged))
                .and(AdminReviewSpecification.visible(visible))
                .and(AdminReviewSpecification.tourId(tourId))
                .and(AdminReviewSpecification.userId(userId));
        return PageResponse.from(reviewRepository.findAll(specification, pageable).map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> stats() {
        return Map.of(
                "totalReviews", reviewRepository.count(),
                "visibleReviews", reviewRepository.count(AdminReviewSpecification.notDeleted().and(AdminReviewSpecification.visible(true))),
                "flaggedReviews", reviewRepository.count(AdminReviewSpecification.notDeleted().and(AdminReviewSpecification.flagged(true)))
        );
    }

    @Transactional
    public AdminReviewResponse updateVisibility(Long id, AdminReviewVisibilityRequest request) {
        Review review = findReview(id);
        review.setVisible(request.visible());
        review.setModerationReason(request.reason());
        reviewRepository.save(review);
        adminAuditService.log("SET_REVIEW_VISIBILITY", "REVIEW", review.getReviewId(), null, String.valueOf(request.visible()), request.reason());
        return toResponse(review);
    }

    @Transactional
    public AdminReviewResponse updateFlag(Long id, AdminReviewFlagRequest request) {
        Review review = findReview(id);
        review.setFlagged(request.flagged());
        review.setModerationReason(request.reason());
        reviewRepository.save(review);
        adminAuditService.log("FLAG_REVIEW", "REVIEW", review.getReviewId(), null, String.valueOf(request.flagged()), request.reason());
        return toResponse(review);
    }

    @Transactional
    public void delete(Long id) {
        Review review = findReview(id);
        review.setVisible(false);
        review.setDeletedAt(LocalDateTime.now());
        reviewRepository.save(review);
        adminAuditService.log("DELETE_REVIEW", "REVIEW", review.getReviewId(), null, "SOFT_DELETED", null);
    }

    private Review findReview(Long id) {
        return reviewRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Review not found"));
    }

    private AdminReviewResponse toResponse(Review review) {
        return new AdminReviewResponse(
                review.getReviewId(),
                review.getUser().getFullName(),
                review.getTour().getTitle(),
                review.getTour().getProvider().getCompanyName(),
                review.getRating(),
                review.getComment(),
                review.getVisible(),
                review.getFlagged(),
                review.getCreatedAt()
        );
    }
}
