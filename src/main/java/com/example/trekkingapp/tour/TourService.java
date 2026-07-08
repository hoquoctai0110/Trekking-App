package com.example.trekkingapp.tour;

import com.example.trekkingapp.auth.CurrentUserService;
import com.example.trekkingapp.route.Route;
import com.example.trekkingapp.route.RouteRepository;
import com.example.trekkingapp.tourprovider.TourProvider;
import com.example.trekkingapp.tourprovider.TourProviderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TourService {

    private static final Logger log = LoggerFactory.getLogger(TourService.class);

    private static final String STATUS_DELETED = "DELETED";
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_PUBLISHED = "PUBLISHED";

    private final TourRepository tourRepository;
    private final TourProviderRepository tourProviderRepository;
    private final RouteRepository routeRepository;
    private final CurrentUserService currentUserService;

    public TourService(
            TourRepository tourRepository,
            TourProviderRepository tourProviderRepository,
            RouteRepository routeRepository,
            CurrentUserService currentUserService
    ) {
        this.tourRepository = tourRepository;
        this.tourProviderRepository = tourProviderRepository;
        this.routeRepository = routeRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public TourResponse createMyTour(TourRequest request) {
        TourProvider provider = findCurrentProvider();
        Route route = findActiveRoute(request.routeId());

        Tour tour = new Tour();
        tour.setProvider(provider);
        tour.setRoute(route);
        tour.setStatus(STATUS_DRAFT);
        applyRequest(tour, request);

        return toResponse(tourRepository.save(tour));
    }

    @Transactional(readOnly = true)
    public List<TourResponse> findMyTours() {
        TourProvider provider = findCurrentProvider();
        return tourRepository.findByProvider_ProviderIdAndStatusNot(provider.getProviderId(), STATUS_DELETED)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TourResponse updateMyTour(Long tourId, TourRequest request) {
        TourProvider provider = findCurrentProvider();
        Tour tour = findActiveTour(tourId);

        validateOwnership(tour, provider);
        tour.setRoute(findActiveRoute(request.routeId()));
        applyRequest(tour, request);

        return toResponse(tourRepository.save(tour));
    }

    @Transactional
    public String deleteMyTour(Long tourId) {
        TourProvider provider = findCurrentProvider();
        Tour tour = findActiveTour(tourId);

        validateOwnership(tour, provider);
        tour.setStatus(STATUS_DELETED);
        tourRepository.save(tour);

        return "Tour deleted successfully";
    }

    @Transactional(readOnly = true)
    public List<TourResponse> findPublishedTours() {
        List<Tour> publishedTours = tourRepository.findByStatus(STATUS_PUBLISHED);
        log.info("public_tours_loaded count={}", publishedTours.size());
        return publishedTours
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TourResponse findById(Long tourId) {
        return toResponse(findActiveTour(tourId));
    }

    @Transactional(readOnly = true)
    public List<TourResponse> findByProviderId(Long providerId) {
        return tourRepository.findByProvider_ProviderIdAndStatusNot(providerId, STATUS_DELETED)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private TourProvider findCurrentProvider() {
        Long currentUserId = currentUserService.getCurrentUserId();
        TourProvider provider = tourProviderRepository.findByUser_UserId(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("Tour provider profile not found"));

        if (STATUS_DELETED.equals(provider.getStatus())) {
            throw new IllegalArgumentException("Provider profile is deleted");
        }

        return provider;
    }

    private Route findActiveRoute(Long routeId) {
        return routeRepository.findByRouteIdAndStatusNot(routeId, STATUS_DELETED)
                .orElseThrow(() -> new IllegalArgumentException("Route not found"));
    }

    private Tour findActiveTour(Long tourId) {
        return tourRepository.findByTourIdAndStatusNot(tourId, STATUS_DELETED)
                .orElseThrow(() -> new IllegalArgumentException("Tour not found"));
    }

    private void validateOwnership(Tour tour, TourProvider provider) {
        if (!tour.getProvider().getProviderId().equals(provider.getProviderId())) {
            throw new IllegalArgumentException("You are not allowed to modify this tour");
        }
    }

    private void applyRequest(Tour tour, TourRequest request) {
        tour.setTitle(request.title());
        tour.setDescription(request.description());
        tour.setPrice(request.price());
        tour.setMaxParticipants(request.maxParticipants());
        tour.setDifficulty(request.difficulty());
        tour.setDuration(request.duration());
        tour.setMeetingPoint(request.meetingPoint());
        tour.setStartDate(request.startDate());
        tour.setEndDate(request.endDate());
    }

    private TourResponse toResponse(Tour tour) {
        TourProvider provider = tour.getProvider();
        Route route = tour.getRoute();
        return new TourResponse(
                tour.getTourId(),
                provider.getProviderId(),
                provider.getCompanyName(),
                route.getRouteId(),
                route.getRouteName(),
                tour.getTitle(),
                tour.getDescription(),
                tour.getPrice(),
                tour.getMaxParticipants(),
                tour.getDifficulty(),
                tour.getDuration(),
                tour.getMeetingPoint(),
                tour.getStatus(),
                tour.getStartDate(),
                tour.getEndDate(),
                tour.getCreatedAt(),
                tour.getUpdatedAt()
        );
    }
}
