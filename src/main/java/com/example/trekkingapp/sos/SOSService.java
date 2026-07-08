package com.example.trekkingapp.sos;

import com.example.trekkingapp.auth.CurrentUserService;
import com.example.trekkingapp.booking.Booking;
import com.example.trekkingapp.booking.BookingRepository;
import com.example.trekkingapp.tour.Tour;
import com.example.trekkingapp.tourprovider.TourProvider;
import com.example.trekkingapp.tourprovider.TourProviderRepository;
import com.example.trekkingapp.tracking.TrackingSession;
import com.example.trekkingapp.tracking.TrackingSessionRepository;
import com.example.trekkingapp.user.User;
import com.example.trekkingapp.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class SOSService {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final SOSAlertRepository sosAlertRepository;
    private final BookingRepository bookingRepository;
    private final TrackingSessionRepository trackingSessionRepository;
    private final UserRepository userRepository;
    private final TourProviderRepository tourProviderRepository;
    private final CurrentUserService currentUserService;

    public SOSService(
            SOSAlertRepository sosAlertRepository,
            BookingRepository bookingRepository,
            TrackingSessionRepository trackingSessionRepository,
            UserRepository userRepository,
            TourProviderRepository tourProviderRepository,
            CurrentUserService currentUserService
    ) {
        this.sosAlertRepository = sosAlertRepository;
        this.bookingRepository = bookingRepository;
        this.trackingSessionRepository = trackingSessionRepository;
        this.userRepository = userRepository;
        this.tourProviderRepository = tourProviderRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public SOSResponse create(SOSRequest request) {
        validateCoordinates(request.latitude(), request.longitude());

        Long currentUserId = currentUserService.getCurrentUserId();
        String clientRequestId = normalizeClientRequestId(request.clientRequestId());
        if (clientRequestId != null) {
            Optional<SOSAlert> existingAlert = sosAlertRepository.findByUser_UserIdAndClientRequestId(
                    currentUserId,
                    clientRequestId
            );
            if (existingAlert.isPresent()) {
                return toResponse(existingAlert.get());
            }
        }

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Current user not found"));
        Booking booking = bookingRepository.findByBookingId(request.bookingId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        if (!booking.getTrekker().getUserId().equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to create SOS for this booking");
        }

        TrackingSession trackingSession = null;
        if (request.trackingSessionId() != null) {
            trackingSession = trackingSessionRepository.findBySessionId(request.trackingSessionId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tracking session not found"));
            validateTrackingSession(trackingSession, currentUserId, booking.getBookingId());
        }

        SOSAlert alert = new SOSAlert();
        alert.setUser(currentUser);
        alert.setBooking(booking);
        alert.setTrackingSession(trackingSession);
        alert.setTour(booking.getTour());
        alert.setLatitude(request.latitude());
        alert.setLongitude(request.longitude());
        alert.setMessage(request.message());
        alert.setStatus(SOSAlertStatus.PENDING);
        alert.setSource(request.source() == null ? SOSAlertSource.API : request.source());
        alert.setClientCreatedAt(request.clientCreatedAt() == null ? LocalDateTime.now() : request.clientCreatedAt());
        alert.setClientRequestId(clientRequestId);

        return toResponse(sosAlertRepository.save(alert));
    }

    @Transactional(readOnly = true)
    public List<SOSResponse> findMine() {
        Long currentUserId = currentUserService.getCurrentUserId();
        return sosAlertRepository.findByUser_UserIdOrderByCreatedAtDesc(currentUserId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SOSResponse> findForProvider() {
        TourProvider provider = findCurrentProvider();
        return sosAlertRepository.findByTour_Provider_ProviderIdOrderByCreatedAtDesc(provider.getProviderId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SOSResponse> findAll() {
        return sosAlertRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public SOSResponse acknowledge(Long sosId) {
        SOSAlert alert = findAlert(sosId);
        validateProviderOrAdminCanModify(alert);

        if (alert.getStatus() == SOSAlertStatus.PENDING) {
            alert.setStatus(SOSAlertStatus.ACKNOWLEDGED);
            alert.setAcknowledgedAt(LocalDateTime.now());
        }

        return toResponse(sosAlertRepository.save(alert));
    }

    @Transactional
    public SOSResponse resolve(Long sosId) {
        SOSAlert alert = findAlert(sosId);
        validateProviderOrAdminCanModify(alert);

        if (alert.getAcknowledgedAt() == null) {
            alert.setAcknowledgedAt(LocalDateTime.now());
        }
        alert.setStatus(SOSAlertStatus.RESOLVED);
        alert.setResolvedAt(LocalDateTime.now());

        return toResponse(sosAlertRepository.save(alert));
    }

    private SOSAlert findAlert(Long sosId) {
        return sosAlertRepository.findBySosId(sosId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SOS alert not found"));
    }

    private TourProvider findCurrentProvider() {
        Long currentUserId = currentUserService.getCurrentUserId();
        return tourProviderRepository.findByUser_UserId(currentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Tour provider profile not found"));
    }

    private void validateProviderOrAdminCanModify(SOSAlert alert) {
        if (hasRole(ROLE_ADMIN)) {
            return;
        }

        Long currentUserId = currentUserService.getCurrentUserId();
        boolean isProviderOwner = tourProviderRepository.findByUser_UserId(currentUserId)
                .map(provider -> alert.getTour().getProvider().getProviderId().equals(provider.getProviderId()))
                .orElse(false);

        if (!isProviderOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to modify this SOS alert");
        }
    }

    private void validateTrackingSession(TrackingSession session, Long currentUserId, Long bookingId) {
        if (!session.getUser().getUserId().equals(currentUserId)
                || !session.getBooking().getBookingId().equals(bookingId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Tracking session does not belong to this user and booking"
            );
        }
    }

    private void validateCoordinates(Double latitude, Double longitude) {
        if (latitude == null || latitude.isNaN() || latitude.isInfinite() || latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Invalid latitude");
        }

        if (longitude == null || longitude.isNaN() || longitude.isInfinite() || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Invalid longitude");
        }
    }

    private String normalizeClientRequestId(String clientRequestId) {
        if (clientRequestId == null || clientRequestId.isBlank()) {
            return null;
        }

        return clientRequestId.trim();
    }

    private boolean hasRole(String authority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.getAuthorities()
                .stream()
                .anyMatch(grantedAuthority -> authority.equals(grantedAuthority.getAuthority()));
    }

    private SOSResponse toResponse(SOSAlert alert) {
        Booking booking = alert.getBooking();
        Tour tour = alert.getTour();
        User user = alert.getUser();
        return new SOSResponse(
                alert.getSosId(),
                booking.getBookingId(),
                tour.getTourId(),
                tour.getTitle(),
                user.getUserId(),
                user.getFullName(),
                user.getEmail(),
                alert.getLatitude(),
                alert.getLongitude(),
                alert.getMessage(),
                alert.getStatus(),
                alert.getSource(),
                alert.getClientCreatedAt(),
                alert.getCreatedAt(),
                alert.getAcknowledgedAt(),
                alert.getResolvedAt()
        );
    }
}
