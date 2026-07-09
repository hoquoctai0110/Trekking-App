package com.example.trekkingapp.tracking;

import com.example.trekkingapp.auth.CurrentUserService;
import com.example.trekkingapp.booking.Booking;
import com.example.trekkingapp.booking.BookingStatus;
import com.example.trekkingapp.booking.BookingStatusManager;
import com.example.trekkingapp.booking.BookingRepository;
import com.example.trekkingapp.route.Route;
import com.example.trekkingapp.tour.Tour;
import com.example.trekkingapp.tourprovider.TourProvider;
import com.example.trekkingapp.tourprovider.TourProviderRepository;
import com.example.trekkingapp.user.User;
import com.example.trekkingapp.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class TrackingService {

    private static final Logger log = LoggerFactory.getLogger(TrackingService.class);

    private static final String STATUS_ACTIVE = TrackingStatus.ACTIVE.name();
    private static final String STATUS_PAUSED = TrackingStatus.PAUSED.name();
    private static final String STATUS_COMPLETED = TrackingStatus.COMPLETED.name();
    private static final String ROUTE_TYPE_ONE_WAY = "ONE_WAY";
    private static final String ROUTE_TYPE_ROUND_TRIP = "ROUND_TRIP";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_TOUR_PROVIDER = "ROLE_TOUR_PROVIDER";
    private static final double MIN_MEANINGFUL_DISTANCE_KM = 0.005;
    private static final long MIN_MEANINGFUL_SECONDS = 30;

    private final TrackingSessionRepository trackingSessionRepository;
    private final TrackingPointRepository trackingPointRepository;
    private final BookingRepository bookingRepository;
    private final BookingStatusManager bookingStatusManager;
    private final UserRepository userRepository;
    private final TourProviderRepository tourProviderRepository;
    private final CurrentUserService currentUserService;

    public TrackingService(
            TrackingSessionRepository trackingSessionRepository,
            TrackingPointRepository trackingPointRepository,
            BookingRepository bookingRepository,
            BookingStatusManager bookingStatusManager,
            UserRepository userRepository,
            TourProviderRepository tourProviderRepository,
            CurrentUserService currentUserService
    ) {
        this.trackingSessionRepository = trackingSessionRepository;
        this.trackingPointRepository = trackingPointRepository;
        this.bookingRepository = bookingRepository;
        this.bookingStatusManager = bookingStatusManager;
        this.userRepository = userRepository;
        this.tourProviderRepository = tourProviderRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public TrackingSessionResponse startSession(TrackingSessionRequest request) {
        TrackingDirection direction = normalizeDirection(request.direction());
        Long currentUserId = currentUserService.getCurrentUserId();
        Booking booking = bookingRepository.findByBookingId(request.bookingId())
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        log.info("[Tracking] start_session_request userId={} email={} bookingId={} tourId={} routeId={} sessionId={} bookingStatus={} paymentStatus={} direction={}",
                currentUserId,
                safeEmail(booking.getTrekker()),
                booking.getBookingId(),
                safeTourId(booking),
                safeRouteId(booking),
                null,
                booking.getBookingStatus(),
                booking.getPaymentStatus(),
                direction);

        if (bookingStatusManager.synchronizePaidBooking(booking)) {
            bookingRepository.save(booking);
            log.info("[Tracking] booking_status_synchronized userId={} email={} bookingId={} tourId={} routeId={} sessionId={} bookingStatus={} paymentStatus={}",
                    currentUserId,
                    safeEmail(booking.getTrekker()),
                    booking.getBookingId(),
                    safeTourId(booking),
                    safeRouteId(booking),
                    null,
                    booking.getBookingStatus(),
                    booking.getPaymentStatus());
        }

        if (!booking.getTrekker().getUserId().equals(currentUserId)) {
            throw new IllegalArgumentException("You are not allowed to track this booking");
        }

        if (booking.getBookingStatus() != BookingStatus.CONFIRMED
                && booking.getBookingStatus() != BookingStatus.COMPLETED) {
            throw new IllegalArgumentException("Booking must be confirmed or completed before tracking");
        }

        boolean activeSessionExists = trackingSessionRepository.existsByBooking_BookingIdAndDirectionAndStatus(
                request.bookingId(),
                direction.name(),
                STATUS_ACTIVE
        );
        boolean completedOutboundExists = trackingSessionRepository.existsByBooking_BookingIdAndDirectionAndStatus(
                request.bookingId(),
                TrackingDirection.OUTBOUND.name(),
                STATUS_COMPLETED
        );

        log.info("[Tracking] start_session_gate userId={} email={} bookingId={} tourId={} routeId={} sessionId={} bookingStatus={} paymentStatus={} activeSessionExists={} completedOutboundExists={}",
                currentUserId,
                safeEmail(booking.getTrekker()),
                booking.getBookingId(),
                safeTourId(booking),
                safeRouteId(booking),
                null,
                booking.getBookingStatus(),
                booking.getPaymentStatus(),
                activeSessionExists,
                completedOutboundExists);

        if (activeSessionExists) {
            throw new IllegalArgumentException("Booking already has an active " + direction.name() + " tracking session");
        }

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("Current user not found"));
        Tour tour = booking.getTour();
        Route route = tour.getRoute();
        validateDirectionForRoute(route, direction);

        if (direction == TrackingDirection.RETURN && !completedOutboundExists) {
            throw new IllegalArgumentException("RETURN tracking requires completed OUTBOUND tracking session");
        }

        TrackingSession session = new TrackingSession();
        session.setUser(user);
        session.setBooking(booking);
        session.setTour(tour);
        session.setRoute(route);
        session.setStatus(STATUS_ACTIVE);
        session.setDirection(direction.name());
        session.setStartedAt(LocalDateTime.now());
        session.setTotalDistanceKm(0.0);

        TrackingSession savedSession = trackingSessionRepository.save(session);
        log.info("[Tracking] start_session_success userId={} email={} bookingId={} tourId={} routeId={} sessionId={} bookingStatus={} paymentStatus={} direction={}",
                currentUserId,
                safeEmail(booking.getTrekker()),
                booking.getBookingId(),
                safeTourId(booking),
                safeRouteId(booking),
                savedSession.getSessionId(),
                booking.getBookingStatus(),
                booking.getPaymentStatus(),
                direction);
        return toSessionResponse(savedSession);
    }

    @Transactional
    public TrackingPointResponse addLocation(TrackingLocationRequest request) {
        log.info("[Tracking] add_location_request userId={} trackingSessionId={} latitude={} longitude={}",
                currentUserService.getCurrentUserId(),
                request.trackingSessionId(),
                request.latitude(),
                request.longitude());
        TrackingPointRequest pointRequest = new TrackingPointRequest(
                request.latitude(),
                request.longitude(),
                request.altitude(),
                request.speed(),
                request.accuracy(),
                request.recordedAt()
        );
        return addPoint(request.trackingSessionId(), pointRequest);
    }

    @Transactional
    public TrackingLocationBatchResponse syncLocations(TrackingLocationBatchRequest request) {
        Long currentUserId = currentUserService.getCurrentUserId();
        TrackingSession session = findSession(request.trackingSessionId());
        logSessionContext("sync_locations_request", session, currentUserId);

        if (!session.getUser().getUserId().equals(currentUserId)) {
            throw new IllegalArgumentException("You are not allowed to view this tracking session");
        }

        if (!STATUS_ACTIVE.equals(session.getStatus()) && !STATUS_PAUSED.equals(session.getStatus())) {
            throw new IllegalArgumentException("Tracking session is not active");
        }

        int syncedCount = 0;
        int skippedCount = 0;

        List<TrackingLocationPointRequest> sortedPoints = request.points()
                .stream()
                .sorted(Comparator.comparing(point -> point.recordedAt() == null ? LocalDateTime.MAX : point.recordedAt()))
                .toList();

        for (TrackingLocationPointRequest pointRequest : sortedPoints) {
            validateCoordinates(pointRequest.latitude(), pointRequest.longitude());

            LocalDateTime recordedAt = pointRequest.recordedAt() == null
                    ? LocalDateTime.now()
                    : pointRequest.recordedAt();

            if (trackingPointRepository.existsBySession_SessionIdAndRecordedAtAndLatitudeAndLongitude(
                    session.getSessionId(),
                    recordedAt,
                    pointRequest.latitude(),
                    pointRequest.longitude()
            )) {
                skippedCount++;
                continue;
            }

            TrackingPoint previousPoint = trackingPointRepository
                    .findTopBySession_SessionIdOrderByRecordedAtDescPointIdDesc(session.getSessionId())
                    .orElse(null);

            TrackingPoint point = new TrackingPoint();
            point.setSession(session);
            point.setLatitude(pointRequest.latitude());
            point.setLongitude(pointRequest.longitude());
            point.setAltitude(pointRequest.altitude());
            point.setSpeed(pointRequest.speed());
            point.setAccuracy(pointRequest.accuracy());
            point.setRecordedAt(recordedAt);

            if (previousPoint != null) {
                double segmentKm = haversineDistanceKm(
                        previousPoint.getLatitude(),
                        previousPoint.getLongitude(),
                        pointRequest.latitude(),
                        pointRequest.longitude()
                );
                session.setTotalDistanceKm(defaultDistance(session.getTotalDistanceKm()) + segmentKm);
            }

            session.setLastLatitude(pointRequest.latitude());
            session.setLastLongitude(pointRequest.longitude());
            session.setLastLocationAt(recordedAt);
            trackingPointRepository.save(point);
            syncedCount++;
        }

        trackingSessionRepository.save(session);
        log.info("[Tracking] sync_locations_success userId={} email={} bookingId={} tourId={} routeId={} sessionId={} bookingStatus={} paymentStatus={} syncedCount={} skippedCount={}",
                currentUserId,
                safeEmail(session.getUser()),
                session.getBooking().getBookingId(),
                session.getTour().getTourId(),
                session.getRoute().getRouteId(),
                session.getSessionId(),
                session.getBooking().getBookingStatus(),
                session.getBooking().getPaymentStatus(),
                syncedCount,
                skippedCount);
        return new TrackingLocationBatchResponse(syncedCount, skippedCount);
    }

    @Transactional
    public TrackingPointResponse addPoint(Long sessionId, TrackingPointRequest request) {
        validateCoordinates(request.latitude(), request.longitude());

        Long currentUserId = currentUserService.getCurrentUserId();
        TrackingSession session = findSession(sessionId);
        logSessionContext("add_point_request", session, currentUserId);

        if (!session.getUser().getUserId().equals(currentUserId)) {
            throw new IllegalArgumentException("You are not allowed to view this tracking session");
        }

        if (!STATUS_ACTIVE.equals(session.getStatus())) {
            if (STATUS_PAUSED.equals(session.getStatus())) {
                log.info("[Tracking] rejected location update because session paused session={}", sessionId);
            }
            throw new IllegalArgumentException("Tracking session is not active");
        }

        LocalDateTime recordedAt = request.recordedAt() == null ? LocalDateTime.now() : request.recordedAt();
        TrackingPoint previousPoint = trackingPointRepository
                .findTopBySession_SessionIdOrderByRecordedAtDescPointIdDesc(sessionId)
                .orElse(null);

        if (previousPoint != null && !isMeaningfulUpdate(previousPoint, request, recordedAt)) {
            return toPointResponse(previousPoint);
        }

        TrackingPoint point = new TrackingPoint();
        point.setSession(session);
        point.setLatitude(request.latitude());
        point.setLongitude(request.longitude());
        point.setAltitude(request.altitude());
        point.setSpeed(request.speed());
        point.setAccuracy(request.accuracy());
        point.setRecordedAt(recordedAt);

        if (previousPoint != null) {
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

        TrackingPoint savedPoint = trackingPointRepository.save(point);
        log.info("[Tracking] add_point_success userId={} email={} bookingId={} tourId={} routeId={} sessionId={} bookingStatus={} paymentStatus={} pointId={}",
                currentUserId,
                safeEmail(session.getUser()),
                session.getBooking().getBookingId(),
                session.getTour().getTourId(),
                session.getRoute().getRouteId(),
                session.getSessionId(),
                session.getBooking().getBookingStatus(),
                session.getBooking().getPaymentStatus(),
                savedPoint.getPointId());
        return toPointResponse(savedPoint);
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
        logSessionContext("find_session_by_id", session, currentUserService.getCurrentUserId());
        return toSessionResponse(session);
    }

    @Transactional(readOnly = true)
    public TrackingLatestSessionResponse findLatestSessionByBookingId(Long bookingId) {
        Long currentUserId = currentUserService.getCurrentUserId();
        Booking booking = bookingRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        if (!booking.getTrekker().getUserId().equals(currentUserId)) {
            throw new IllegalArgumentException("You are not allowed to track this booking");
        }

        TrackingSession latestSession = trackingSessionRepository
                .findTopByBooking_BookingIdOrderByCreatedAtDescSessionIdDesc(bookingId)
                .orElse(null);

        log.info("[Tracking] find_latest_session_by_booking userId={} email={} bookingId={} tourId={} routeId={} sessionId={} bookingStatus={} paymentStatus={} found={}",
                currentUserId,
                safeEmail(booking.getTrekker()),
                booking.getBookingId(),
                safeTourId(booking),
                safeRouteId(booking),
                latestSession == null ? null : latestSession.getSessionId(),
                booking.getBookingStatus(),
                booking.getPaymentStatus(),
                latestSession != null);

        if (latestSession == null) {
            return null;
        }

        return new TrackingLatestSessionResponse(
                latestSession.getSessionId(),
                latestSession.getBooking().getBookingId(),
                defaultDirection(latestSession.getDirection()),
                latestSession.getStatus(),
                latestSession.getStartedAt(),
                latestSession.getEndedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<TrackingPointResponse> findSessionPoints(Long sessionId) {
        TrackingSession session = findSession(sessionId);
        validateCanView(session);
        logSessionContext("find_session_points", session, currentUserService.getCurrentUserId());
        return trackingPointRepository.findBySession_SessionIdOrderByRecordedAtAsc(sessionId)
                .stream()
                .map(this::toPointResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TrackingPointResponse findLatestPoint(Long sessionId) {
        TrackingSession session = findSession(sessionId);
        validateCanView(session);
        logSessionContext("find_latest_point", session, currentUserService.getCurrentUserId());
        return trackingPointRepository.findTopBySession_SessionIdOrderByRecordedAtDescPointIdDesc(sessionId)
                .map(this::toPointResponse)
                .orElseThrow(() -> new IllegalArgumentException("Tracking location not found"));
    }

    @Transactional
    public TrackingSessionResponse pauseSession(Long sessionId) {
        TrackingSession session = findOwnedSession(sessionId);
        logSessionContext("pause_session_request", session, currentUserService.getCurrentUserId());
        if (!STATUS_ACTIVE.equals(session.getStatus())) {
            throw new IllegalArgumentException("Tracking session is not active");
        }

        session.setStatus(STATUS_PAUSED);
        session.setPausedAt(LocalDateTime.now());
        TrackingSession savedSession = trackingSessionRepository.save(session);
        logSessionContext("pause_session_success", savedSession, currentUserService.getCurrentUserId());
        return toSessionResponse(savedSession);
    }

    @Transactional
    public TrackingSessionResponse resumeSession(Long sessionId) {
        TrackingSession session = findOwnedSession(sessionId);
        logSessionContext("resume_session_request", session, currentUserService.getCurrentUserId());
        if (!STATUS_PAUSED.equals(session.getStatus())) {
            throw new IllegalArgumentException("Tracking session is not active");
        }

        session.setStatus(STATUS_ACTIVE);
        TrackingSession savedSession = trackingSessionRepository.save(session);
        logSessionContext("resume_session_success", savedSession, currentUserService.getCurrentUserId());
        return toSessionResponse(savedSession);
    }

    @Transactional
    public TrackingSessionResponse finishSession(Long sessionId) {
        TrackingSession session = findOwnedSession(sessionId);
        logSessionContext("finish_session_request", session, currentUserService.getCurrentUserId());
        if (!STATUS_ACTIVE.equals(session.getStatus()) && !STATUS_PAUSED.equals(session.getStatus())) {
            throw new IllegalArgumentException("Tracking session is not active");
        }

        session.setStatus(STATUS_COMPLETED);
        session.setEndedAt(LocalDateTime.now());
        TrackingSession savedSession = trackingSessionRepository.save(session);
        logSessionContext("finish_session_success", savedSession, currentUserService.getCurrentUserId());
        return toSessionResponse(savedSession);
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
        if (latitude == null || latitude.isNaN() || latitude.isInfinite() || latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Invalid latitude");
        }

        if (longitude == null || longitude.isNaN() || longitude.isInfinite() || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Invalid longitude");
        }
    }

    private boolean isMeaningfulUpdate(TrackingPoint previousPoint, TrackingPointRequest request, LocalDateTime recordedAt) {
        double distanceKm = haversineDistanceKm(
                previousPoint.getLatitude(),
                previousPoint.getLongitude(),
                request.latitude(),
                request.longitude()
        );
        long seconds = Math.abs(Duration.between(previousPoint.getRecordedAt(), recordedAt).getSeconds());

        return distanceKm >= MIN_MEANINGFUL_DISTANCE_KM || seconds >= MIN_MEANINGFUL_SECONDS;
    }

    private TrackingSessionResponse toSessionResponse(TrackingSession session) {
        return new TrackingSessionResponse(
                session.getSessionId(),
                session.getUser().getUserId(),
                session.getTour().getTourId(),
                session.getRoute().getRouteId(),
                session.getBooking().getBookingId(),
                session.getStatus(),
                defaultDirection(session.getDirection()),
                session.getStartedAt(),
                session.getEndedAt(),
                session.getPausedAt(),
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

    private TrackingDirection normalizeDirection(String direction) {
        if (direction == null || direction.isBlank()) {
            return TrackingDirection.OUTBOUND;
        }

        try {
            return TrackingDirection.valueOf(direction.toUpperCase(Locale.ROOT));
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Unsupported tracking direction");
        }
    }

    private void validateDirectionForRoute(Route route, TrackingDirection direction) {
        String routeType = route.getRouteType();
        if (routeType == null || routeType.isBlank()) {
            routeType = ROUTE_TYPE_ONE_WAY;
        }

        if (direction == TrackingDirection.OUTBOUND) {
            return;
        }

        if (!ROUTE_TYPE_ROUND_TRIP.equals(routeType.toUpperCase(Locale.ROOT))) {
            throw new IllegalArgumentException("RETURN tracking is only allowed for ROUND_TRIP routes");
        }
    }

    private String defaultDirection(String direction) {
        return direction == null || direction.isBlank() ? TrackingDirection.OUTBOUND.name() : direction;
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

    private void logSessionContext(String action, TrackingSession session, Long currentUserId) {
        log.info("[Tracking] {} userId={} email={} bookingId={} tourId={} routeId={} sessionId={} bookingStatus={} paymentStatus={} sessionStatus={}",
                action,
                currentUserId,
                safeEmail(session.getUser()),
                session.getBooking().getBookingId(),
                session.getTour().getTourId(),
                session.getRoute().getRouteId(),
                session.getSessionId(),
                session.getBooking().getBookingStatus(),
                session.getBooking().getPaymentStatus(),
                session.getStatus());
    }

    private String safeEmail(User user) {
        return user == null ? null : user.getEmail();
    }

    private Long safeTourId(Booking booking) {
        return booking.getTour() == null ? null : booking.getTour().getTourId();
    }

    private Long safeRouteId(Booking booking) {
        return booking.getTour() == null || booking.getTour().getRoute() == null
                ? null
                : booking.getTour().getRoute().getRouteId();
    }
}
