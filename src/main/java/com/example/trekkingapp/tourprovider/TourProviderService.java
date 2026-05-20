package com.example.trekkingapp.tourprovider;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class TourProviderService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_DELETED = "DELETED";

    private final TourProviderRepository tourProviderRepository;

    public TourProviderService(TourProviderRepository tourProviderRepository) {
        this.tourProviderRepository = tourProviderRepository;
    }

    @Transactional
    public TourProviderResponse create(TourProviderRequest request) {
        TourProvider tourProvider = new TourProvider();
        applyCreateFields(tourProvider, request);
        return toResponse(tourProviderRepository.save(tourProvider));
    }

    @Transactional(readOnly = true)
    public List<TourProviderResponse> findAll() {
        return tourProviderRepository.findByStatusNot(STATUS_DELETED)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<TourProviderResponse> findById(Long providerId) {
        return tourProviderRepository.findByProviderIdAndStatusNot(providerId, STATUS_DELETED)
                .map(this::toResponse);
    }

    @Transactional
    public Optional<TourProviderResponse> update(Long providerId, TourProviderRequest request) {
        return tourProviderRepository.findByProviderIdAndStatusNot(providerId, STATUS_DELETED)
                .map(tourProvider -> {
                    applyUpdateFields(tourProvider, request);
                    return toResponse(tourProviderRepository.save(tourProvider));
                });
    }

    @Transactional
    public boolean delete(Long providerId) {
        return tourProviderRepository.findByProviderIdAndStatusNot(providerId, STATUS_DELETED)
                .map(tourProvider -> {
                    tourProvider.setStatus(STATUS_DELETED);
                    tourProviderRepository.save(tourProvider);
                    return true;
                })
                .orElse(false);
    }

    private void applyCreateFields(TourProvider tourProvider, TourProviderRequest request) {
        tourProvider.setCompanyName(request.companyName());
        tourProvider.setDescription(request.description());
        tourProvider.setBusinessLicenseUrl(request.businessLicenseUrl());
        tourProvider.setPhone(request.phone());
        tourProvider.setEmail(request.email());
        tourProvider.setAddress(request.address());
        tourProvider.setStatus(defaultIfBlank(request.status(), STATUS_PENDING));
    }

    private void applyUpdateFields(TourProvider tourProvider, TourProviderRequest request) {
        tourProvider.setCompanyName(request.companyName());
        tourProvider.setDescription(request.description());
        tourProvider.setBusinessLicenseUrl(request.businessLicenseUrl());
        tourProvider.setPhone(request.phone());
        tourProvider.setEmail(request.email());
        tourProvider.setAddress(request.address());

        if (!isBlank(request.status())) {
            tourProvider.setStatus(request.status());
        }
    }

    private TourProviderResponse toResponse(TourProvider tourProvider) {
        return new TourProviderResponse(
                tourProvider.getProviderId(),
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

    private String defaultIfBlank(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
