package com.example.trekkingapp.admin.service;

import com.example.trekkingapp.admin.AdminSortUtils;
import com.example.trekkingapp.admin.audit.AdminAuditService;
import com.example.trekkingapp.admin.dto.request.AdminActionReasonRequest;
import com.example.trekkingapp.admin.dto.response.AdminTourResponse;
import com.example.trekkingapp.admin.dto.response.PageResponse;
import com.example.trekkingapp.admin.specification.AdminTourSpecification;
import com.example.trekkingapp.common.InvalidStateTransitionException;
import com.example.trekkingapp.common.ResourceNotFoundException;
import com.example.trekkingapp.review.ReviewRepository;
import com.example.trekkingapp.route.Route;
import com.example.trekkingapp.tour.Tour;
import com.example.trekkingapp.tour.TourRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class AdminTourService {

    private static final Set<String> SORT_FIELDS = Set.of("createdAt", "updatedAt", "title", "price", "status");

    private final TourRepository tourRepository;
    private final ReviewRepository reviewRepository;
    private final AdminAuditService adminAuditService;

    public AdminTourService(TourRepository tourRepository, ReviewRepository reviewRepository, AdminAuditService adminAuditService) {
        this.tourRepository = tourRepository;
        this.reviewRepository = reviewRepository;
        this.adminAuditService = adminAuditService;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminTourResponse> findTours(int page, int size, String sort, String search, String status, String province, String difficulty, Long providerId) {
        Pageable pageable = AdminSortUtils.pageable(page, size, sort, "createdAt", SORT_FIELDS);
        Specification<Tour> specification = Specification
                .where(AdminTourSpecification.search(search))
                .and(AdminTourSpecification.status(status))
                .and(AdminTourSpecification.province(province))
                .and(AdminTourSpecification.difficulty(difficulty))
                .and(AdminTourSpecification.providerId(providerId));
        return PageResponse.from(tourRepository.findAll(specification, pageable).map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public AdminTourResponse getTour(Long id) {
        return toResponse(findTour(id));
    }

    @Transactional
    public AdminTourResponse approve(Long id) {
        Tour tour = findTour(id);
        ensureState(tour.getStatus(), "DRAFT", "Only draft tour can be approved");
        changeStatus(tour, "APPROVED", "APPROVE_TOUR", null);
        return toResponse(tour);
    }

    @Transactional
    public AdminTourResponse reject(Long id, AdminActionReasonRequest request) {
        Tour tour = findTour(id);
        if (!"DRAFT".equalsIgnoreCase(tour.getStatus()) && !"APPROVED".equalsIgnoreCase(tour.getStatus())) {
            throw new InvalidStateTransitionException("Only draft or approved tour can be rejected");
        }
        changeStatus(tour, "REJECTED", "REJECT_TOUR", request.reason());
        return toResponse(tour);
    }

    @Transactional
    public AdminTourResponse publish(Long id) {
        Tour tour = findTour(id);
        ensureState(tour.getStatus(), "APPROVED", "Only approved tour can be published");
        changeStatus(tour, "PUBLISHED", "PUBLISH_TOUR", null);
        return toResponse(tour);
    }

    @Transactional
    public AdminTourResponse unpublish(Long id) {
        Tour tour = findTour(id);
        ensureState(tour.getStatus(), "PUBLISHED", "Only published tour can be unpublished");
        changeStatus(tour, "APPROVED", "UNPUBLISH_TOUR", null);
        return toResponse(tour);
    }

    @Transactional
    public AdminTourResponse archive(Long id) {
        Tour tour = findTour(id);
        if ("ARCHIVED".equalsIgnoreCase(tour.getStatus())) {
            throw new InvalidStateTransitionException("Tour is already archived");
        }
        changeStatus(tour, "ARCHIVED", "ARCHIVE_TOUR", null);
        return toResponse(tour);
    }

    private void changeStatus(Tour tour, String newStatus, String action, String reason) {
        String oldStatus = tour.getStatus();
        tour.setStatus(newStatus);
        tourRepository.save(tour);
        adminAuditService.log(action, "TOUR", tour.getTourId(), oldStatus, newStatus, reason);
    }

    private Tour findTour(Long id) {
        return tourRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Tour not found"));
    }

    private void ensureState(String current, String expected, String message) {
        if (!expected.equalsIgnoreCase(current)) {
            throw new InvalidStateTransitionException(message);
        }
    }

    private AdminTourResponse toResponse(Tour tour) {
        Route route = tour.getRoute();
        return new AdminTourResponse(
                tour.getTourId(),
                tour.getTitle(),
                tour.getDescription(),
                tour.getCoverImageUrl(),
                tour.getDifficulty(),
                route == null ? null : route.getDistanceKm(),
                tour.getDuration(),
                route == null ? null : route.getElevationGain(),
                route == null ? null : route.getRouteName(),
                tour.getProvider().getCompanyName(),
                tour.getMaxParticipants(),
                tour.getPrice(),
                tour.getStatus(),
                reviewRepository.findAverageRatingByTourId(tour.getTourId()).orElse(0.0),
                reviewRepository.countByTour_TourIdAndDeletedAtIsNull(tour.getTourId()),
                tour.getCreatedAt(),
                tour.getUpdatedAt()
        );
    }
}
