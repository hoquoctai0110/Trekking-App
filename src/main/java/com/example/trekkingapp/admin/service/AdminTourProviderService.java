package com.example.trekkingapp.admin.service;

import com.example.trekkingapp.admin.AdminSortUtils;
import com.example.trekkingapp.admin.audit.AdminAuditService;
import com.example.trekkingapp.admin.dto.request.AdminActionReasonRequest;
import com.example.trekkingapp.admin.dto.response.AdminTourProviderDetailResponse;
import com.example.trekkingapp.admin.dto.response.AdminTourProviderResponse;
import com.example.trekkingapp.admin.dto.response.PageResponse;
import com.example.trekkingapp.admin.specification.AdminTourProviderSpecification;
import com.example.trekkingapp.booking.BookingRepository;
import com.example.trekkingapp.common.InvalidStateTransitionException;
import com.example.trekkingapp.common.ResourceNotFoundException;
import com.example.trekkingapp.payment.PaymentRepository;
import com.example.trekkingapp.review.ReviewRepository;
import com.example.trekkingapp.tour.TourRepository;
import com.example.trekkingapp.tourprovider.TourProvider;
import com.example.trekkingapp.tourprovider.TourProviderRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;

@Service
public class AdminTourProviderService {

    private static final Set<String> SORT_FIELDS = Set.of("createdAt", "updatedAt", "companyName", "status");

    private final TourProviderRepository tourProviderRepository;
    private final BookingRepository bookingRepository;
    private final TourRepository tourRepository;
    private final ReviewRepository reviewRepository;
    private final PaymentRepository paymentRepository;
    private final AdminAuditService adminAuditService;

    public AdminTourProviderService(
            TourProviderRepository tourProviderRepository,
            BookingRepository bookingRepository,
            TourRepository tourRepository,
            ReviewRepository reviewRepository,
            PaymentRepository paymentRepository,
            AdminAuditService adminAuditService
    ) {
        this.tourProviderRepository = tourProviderRepository;
        this.bookingRepository = bookingRepository;
        this.tourRepository = tourRepository;
        this.reviewRepository = reviewRepository;
        this.paymentRepository = paymentRepository;
        this.adminAuditService = adminAuditService;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminTourProviderResponse> findProviders(int page, int size, String sort, String search, String status) {
        Pageable pageable = AdminSortUtils.pageable(page, size, sort, "createdAt", SORT_FIELDS);
        Specification<TourProvider> specification = Specification
                .where(AdminTourProviderSpecification.search(search))
                .and(AdminTourProviderSpecification.status(status));
        return PageResponse.from(tourProviderRepository.findAll(specification, pageable).map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public AdminTourProviderDetailResponse getProvider(Long id) {
        TourProvider provider = findProvider(id);
        return toDetailResponse(provider);
    }

    @Transactional
    public AdminTourProviderDetailResponse approve(Long id) {
        TourProvider provider = findProvider(id);
        ensureState(provider.getStatus(), "PENDING", "Only pending provider can be approved");
        String oldStatus = provider.getStatus();
        provider.setStatus("APPROVED");
        tourProviderRepository.save(provider);
        adminAuditService.log("APPROVE_PROVIDER", "TOUR_PROVIDER", provider.getProviderId(), oldStatus, provider.getStatus(), null);
        return toDetailResponse(provider);
    }

    @Transactional
    public AdminTourProviderDetailResponse reject(Long id, AdminActionReasonRequest request) {
        TourProvider provider = findProvider(id);
        ensureState(provider.getStatus(), "PENDING", "Only pending provider can be rejected");
        String oldStatus = provider.getStatus();
        provider.setStatus("REJECTED");
        tourProviderRepository.save(provider);
        adminAuditService.log("REJECT_PROVIDER", "TOUR_PROVIDER", provider.getProviderId(), oldStatus, provider.getStatus(), request.reason());
        return toDetailResponse(provider);
    }

    @Transactional
    public AdminTourProviderDetailResponse suspend(Long id, AdminActionReasonRequest request) {
        TourProvider provider = findProvider(id);
        if (!"APPROVED".equalsIgnoreCase(provider.getStatus()) && !"ACTIVE".equalsIgnoreCase(provider.getStatus())) {
            throw new InvalidStateTransitionException("Only approved or active provider can be suspended");
        }
        String oldStatus = provider.getStatus();
        provider.setStatus("SUSPENDED");
        tourProviderRepository.save(provider);
        adminAuditService.log("SUSPEND_PROVIDER", "TOUR_PROVIDER", provider.getProviderId(), oldStatus, provider.getStatus(), request.reason());
        return toDetailResponse(provider);
    }

    @Transactional
    public AdminTourProviderDetailResponse reactivate(Long id) {
        TourProvider provider = findProvider(id);
        ensureState(provider.getStatus(), "SUSPENDED", "Only suspended provider can be reactivated");
        String oldStatus = provider.getStatus();
        provider.setStatus("ACTIVE");
        tourProviderRepository.save(provider);
        adminAuditService.log("REACTIVATE_PROVIDER", "TOUR_PROVIDER", provider.getProviderId(), oldStatus, provider.getStatus(), null);
        return toDetailResponse(provider);
    }

    private TourProvider findProvider(Long id) {
        return tourProviderRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Tour provider not found"));
    }

    private void ensureState(String currentStatus, String expectedStatus, String message) {
        if (!expectedStatus.equalsIgnoreCase(currentStatus)) {
            throw new InvalidStateTransitionException(message);
        }
    }

    private AdminTourProviderResponse toResponse(TourProvider provider) {
        return new AdminTourProviderResponse(
                provider.getProviderId(),
                provider.getUser().getUserId(),
                provider.getCompanyName(),
                provider.getEmail(),
                provider.getPhone(),
                provider.getUser().getAvatarUrl(),
                null,
                0.0,
                bookingRepository.countByTour_Provider_User_UserId(provider.getUser().getUserId()),
                provider.getStatus(),
                null,
                provider.getCreatedAt(),
                BigDecimal.ZERO
        );
    }

    private AdminTourProviderDetailResponse toDetailResponse(TourProvider provider) {
        return new AdminTourProviderDetailResponse(
                provider.getProviderId(),
                provider.getUser().getUserId(),
                provider.getUser().getFullName(),
                provider.getEmail(),
                provider.getPhone(),
                provider.getUser().getAvatarUrl(),
                provider.getCompanyName(),
                provider.getDescription(),
                provider.getBusinessLicenseUrl(),
                provider.getCitizenIdImageUrl(),
                provider.getAddress(),
                provider.getStatus(),
                provider.getCreatedAt(),
                provider.getUpdatedAt(),
                0.0,
                bookingRepository.countByTour_Provider_User_UserId(provider.getUser().getUserId()),
                tourRepository.countByProvider_User_UserId(provider.getUser().getUserId())
        );
    }
}
