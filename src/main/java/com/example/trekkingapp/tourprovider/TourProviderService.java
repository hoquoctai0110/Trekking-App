package com.example.trekkingapp.tourprovider;

import com.example.trekkingapp.auth.CurrentUserService;
import com.example.trekkingapp.user.User;
import com.example.trekkingapp.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TourProviderService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_DELETED = "DELETED";
    private static final String ROLE_TOUR_PROVIDER = "TOUR_PROVIDER";

    private final TourProviderRepository tourProviderRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    public TourProviderService(
            TourProviderRepository tourProviderRepository,
            UserRepository userRepository,
            CurrentUserService currentUserService
    ) {
        this.tourProviderRepository = tourProviderRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public TourProviderResponse createMyProfile(TourProviderRequest request) {
        Long currentUserId = currentUserService.getCurrentUserId();
        User user = findCurrentUser(currentUserId);
        validateTourProviderRole(user);

        if (tourProviderRepository.existsByUser_UserId(currentUserId)) {
            throw new IllegalArgumentException("Current user already has a tour provider profile");
        }

        TourProvider tourProvider = new TourProvider();
        tourProvider.setUser(user);
        tourProvider.setStatus(STATUS_PENDING);
        applyRequest(tourProvider, request);

        return toResponse(tourProviderRepository.save(tourProvider));
    }

    @Transactional(readOnly = true)
    public TourProviderResponse getMyProfile() {
        Long currentUserId = currentUserService.getCurrentUserId();
        return tourProviderRepository.findByUser_UserId(currentUserId)
                .filter(tourProvider -> !STATUS_DELETED.equals(tourProvider.getStatus()))
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Tour provider profile not found for current user"));
    }

    @Transactional
    public TourProviderResponse updateMyProfile(TourProviderRequest request) {
        Long currentUserId = currentUserService.getCurrentUserId();
        TourProvider tourProvider = tourProviderRepository.findByUser_UserId(currentUserId)
                .filter(existingProvider -> !STATUS_DELETED.equals(existingProvider.getStatus()))
                .orElseThrow(() -> new IllegalArgumentException("Tour provider profile not found for current user"));

        applyRequest(tourProvider, request);
        return toResponse(tourProviderRepository.save(tourProvider));
    }

    @Transactional
    public String deleteMyProfile() {
        Long currentUserId = currentUserService.getCurrentUserId();
        TourProvider tourProvider = tourProviderRepository.findByUser_UserId(currentUserId)
                .filter(existingProvider -> !STATUS_DELETED.equals(existingProvider.getStatus()))
                .orElseThrow(() -> new IllegalArgumentException("Tour provider profile not found for current user"));

        tourProvider.setStatus(STATUS_DELETED);
        tourProviderRepository.save(tourProvider);
        return "Tour provider profile deleted successfully";
    }

    @Transactional(readOnly = true)
    public TourProviderResponse findById(Long providerId) {
        return tourProviderRepository.findById(providerId)
                .filter(tourProvider -> !STATUS_DELETED.equals(tourProvider.getStatus()))
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Tour provider not found"));
    }

    @Transactional(readOnly = true)
    public List<TourProviderResponse> findAll() {
        return tourProviderRepository.findByStatusNot(STATUS_DELETED)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private User findCurrentUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Current user not found"));
    }

    private void validateTourProviderRole(User user) {
        boolean hasTourProviderRole = user.getUserRoles()
                .stream()
                .anyMatch(userRole -> ROLE_TOUR_PROVIDER.equals(userRole.getRole().getRoleName()));

        if (!hasTourProviderRole) {
            throw new IllegalArgumentException("Current user must have TOUR_PROVIDER role");
        }
    }

    private void applyRequest(TourProvider tourProvider, TourProviderRequest request) {
        tourProvider.setCompanyName(request.companyName());
        tourProvider.setDescription(request.description());
        tourProvider.setBusinessLicenseUrl(request.businessLicenseUrl());
        tourProvider.setPhone(request.phone());
        tourProvider.setEmail(request.email());
        tourProvider.setAddress(request.address());
    }

    private TourProviderResponse toResponse(TourProvider tourProvider) {
        User user = tourProvider.getUser();
        return new TourProviderResponse(
                tourProvider.getProviderId(),
                user.getUserId(),
                user.getEmail(),
                tourProvider.getCompanyName(),
                tourProvider.getDescription(),
                tourProvider.getBusinessLicenseUrl(),
                tourProvider.getPhone(),
                tourProvider.getEmail(),
                tourProvider.getAddress(),
                tourProvider.getStatus(),
                tourProvider.getCreatedAt(),
                tourProvider.getUpdatedAt()
        );
    }
}
