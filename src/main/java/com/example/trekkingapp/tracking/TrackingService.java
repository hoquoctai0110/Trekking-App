package com.example.trekkingapp.tracking;

import com.example.trekkingapp.auth.CurrentUserService;
import com.example.trekkingapp.booking.Booking;
import com.example.trekkingapp.booking.BookingRepository;
import com.example.trekkingapp.route.Route;
import com.example.trekkingapp.tour.Tour;
import com.example.trekkingapp.tourprovider.TourProvider;
import com.example.trekkingapp.tourprovider.TourProviderRepository;
import com.example.trekkingapp.user.User;
import com.example.trekkingapp.user.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TrackingService {

    private static final String BOOKING_STATUS_CONFIRMED = "CONFIRMED";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_PAUSED = "PAUSED";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_TOUR_PROVIDER = "ROLE_TOUR_PROVIDER";

    private final TrackingSessionRepository trackingSessionRepository;
    private final TrackingPointRepository trackingPointRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final TourProviderRepository tourProviderRepository;
    private final CurrentUserService currentUserService;

    public TrackingService(
            TrackingSessionRepository trackingSessionRepository,
            TrackingPointRepository trackingPointRepository,
            BookingRepository bookingRepository,
            UserRepository userRepository,
            TourProviderRepository tourProviderRepository,
            CurrentUserService currentUserService
    ) {
        this.trackingSessionRepository = trackingSessionRepository;
        this.trackingPointRepository = trackingPointRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.tourProviderRepository = tourProviderRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public TrackingSessionResponse startSession(TrackingSessionRequest request) {
        Long currentUserId = currentUserService.getCurrentUserId();
        Booking booking = bookingRepository.findByBookingId(request.bookingId())
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        if (!booking.getTrekker().getUserId().equals(currentUserId)) {
            throw new IllegalArgumentException("You are not allowed to track this booking");
        }

        if (!BOOKING_STATUS_CONFIRMED.equals(booking.getBookingStatus())) {
            throw new IllegalArgumentException("Booking must be confirmed before tracking");
        }

        if (trackingSessionRepository.findByUser_UserIdAndStatus(currentUserId, STATUS_ACTIVE).isPresent()) {
            throw new IllegalArgumentException("You already have an active tracking session");
        }

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("Current user not found"));
        Tour tour = booking.getTour();
        Route route = tour.getRoute();

        TrackingSession session = new TrackingSession();
        session.setUser(user);
        session.setBooking(booking);
        session.setTour(tour);
        session.setRoute(route);
        session.setStatus(STATUS_ACTIVE);
        session.setStartedAt(LocalDateTime.now());
        session.setTotalDistanceKm(0.0);

        return toSessionResponse(trackingSessionRepository.save(session));
    }

    @Transactional
    public TrackingPointResponse addPoint(Long sessionId, TrackingPointRequest request) {
        validateCoordinates(request.latitude(), request.longitude());

        Long currentUserId = currentUserService.getCurrentUserId();
        TrackingSession session = findSession(sessionId);

        if (!session.getUser().getUserId().equals(currentUserId)) {
            throw new IllegalArgumentException("You are not allowed to view this tracking session");
        }

        if (!STATUS_ACTIVE.equals(session.getStatus())) {
            throw new IllegalArgumentException("Tracking session is not active");
        }

        LocalDateTime recordedAt = request.recordedAt() == null ? LocalDateTime.now() : request.recordedAt();
        List<TrackingPoint> existingPoints = trackingPointRepository.findBySession_SessionIdOrderByRecordedAtAsc(sessionId);

        TrackingPoint point = new TrackingPoint();
        point.setSession(session);
        point.setLatitude(request.latitude());
        point.setLongitude(request.longitude());
        point.setAltitude(request.altitude());
        point.setSpeed(request.speed());
        point.setAccuracy(request.accuracy());
        point.setRecordedAt(recordedAt);

        if (!existingPoints.isEmpty()) {
            TrackingPoint previousPoint = existingPoints.get(existingPoints.size() - 1);
            double segmentKm = haversineDistanceKm(
                    previousPoint.getLatitude(),
                    previousPoint.getLongitude(),
                    request.latitude(),
                    request.longitude()
            );
            session.setTotalDistanceKm(defaultDistance(session.getTotalDistanceKm()) + segmentKm);
        }

        session.setLastLatitude(request.latitude());
        session.setLastLongitude(request.longitude());
        session.setLastLocationAt(recordedAt);
        trackingSessionRepository.save(session);

        return toPointResponse(trackingPointRepository.save(point));
    }

    @Transactional(readOnly = true)
    public List<TrackingSessionResponse> findMySessions() {
        Long currentUserId = currentUserService.getCurrentUserId();
        return trackingSessionRepository.findByUser_UserId(currentUserId)
                .stream()
                .map(this::toSessionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TrackingSessionResponse findSessionById(Long sessionId) {
        TrackingSession session = findSession(sessionId);
        validateCanView(session);
        return toSessionResponse(session);
    }

    @Transactional(readOnly = true)
    public List<TrackingPointResponse> findSessionPoints(Long sessionId) {
        TrackingSession session = findSession(sessionId);
        validateCanView(session);
        return trackingPointRepository.findBySession_SessionIdOrderByRecordedAtAsc(sessionId)
                .stream()
                .map(this::toPointResponse)
                .toList();
    }

    @Transactional
    public TrackingSessionResponse pauseSession(Long sessionId) {
        TrackingSession session = findOwnedSession(sessionId);
        if (!STATUS_ACTIVE.equals(session.getStatus())) {
            throw new IllegalArgumentException("Tracking session is not active");
        }

        session.setStatus(STATUS_PAUSED);
        return toSessionResponse(trackingSessionRepository.save(session));
    }

    @Transactional
    public TrackingSessionResponse resumeSession(Long sessionId) {
        TrackingSession session = findOwnedSession(sessionId);
        if (!STATUS_PAUSED.equals(session.getStatus())) {
            throw new IllegalArgumentException("Tracking session is not active");
        }

        session.setStatus(STATUS_ACTIVE);
        return toSessionResponse(trackingSessionRepository.save(session));
    }

    @Transactional
    public TrackingSessionResponse finishSession(Long sessionId) {
        TrackingSession session = findOwnedSession(sessionId);
        if (!STATUS_ACTIVE.equals(session.getStatus()) && !STATUS_PAUSED.equals(session.getStatus())) {
            throw new IllegalArgumentException("Tracking session is not active");
        }

        session.setStatus(STATUS_COMPLETED);
        session.setEndedAt(LocalDateTime.now());
        return toSessionResponse(trackingSessionRepository.save(session));
    }

    private TrackingSession findOwnedSession(Long sessionId) {
        Long currentUserId = currentUserService.getCurrentUserId();
        TrackingSession session = findSession(sessionId);
        if (!session.getUser().getUserId().equals(currentUserId)) {
            throw new IllegalArgumentException("You are not allowed to view this tracking session");
        }

        return session;
    }

    private TrackingSession findSession(Long sessionId) {
        return trackingSessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Tracking session not found"));
    }

    private void validateCanView(TrackingSession session) {
        Long currentUserId = currentUserService.getCurrentUserId();
        if (session.getUser().getUserId().equals(currentUserId) || hasRole(ROLE_ADMIN)) {
            return;
        }

        if (hasRole(ROLE_TOUR_PROVIDER)) {
            TourProvider provider = tourProviderRepository.findByUser_UserId(currentUserId).orElse(null);
            if (provider != null && session.getTour().getProvider().getProviderId().equals(provider.getProviderId())) {
                return;
            }
        }

        throw new IllegalArgumentException("You are not allowed to view this tracking session");
    }

    private boolean hasRole(String authority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.getAuthorities()
                .stream()
                .anyMatch(grantedAuthority -> authority.equals(grantedAuthority.getAuthority()));
    }

    private void validateCoordinates(Double latitude, Double longitude) {
        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Invalid latitude");
        }

        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Invalid longitude");
        }
    }

    private TrackingSessionResponse toSessionResponse(TrackingSession session) {
        return new TrackingSessionResponse(
                session.getSessionId(),
                session.getUser().getUserId(),
                session.getTour().getTourId(),
                session.getRoute().getRouteId(),
                session.getBooking().getBookingId(),
                session.getStatus(),
                session.getStartedAt(),
                session.getEndedAt(),
                session.getTotalDistanceKm(),
                session.getLastLatitude(),
                session.getLastLongitude(),
                session.getLastLocationAt()
        );
    }

    private TrackingPointResponse toPointResponse(TrackingPoint point) {
        return new TrackingPointResponse(
                point.getPointId(),
                point.getSession().getSessionId(),
                point.getLatitude(),
                point.getLongitude(),
                point.getAltitude(),
                point.getSpeed(),
                point.getAccuracy(),
                point.getRecordedAt()
        );
    }

    private double defaultDistance(Double distanceKm) {
        return distanceKm == null ? 0.0 : distanceKm;
    }

    private double haversineDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        double earthRadiusKm = 6371.0;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2)
                * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusKm * c;
    }
}
