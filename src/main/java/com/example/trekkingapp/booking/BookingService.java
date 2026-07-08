package com.example.trekkingapp.booking;

import com.example.trekkingapp.auth.CurrentUserService;
import com.example.trekkingapp.payment.CreatePaymentResult;
import com.example.trekkingapp.payment.PaymentService;
import com.example.trekkingapp.tour.Tour;
import com.example.trekkingapp.tour.TourRepository;
import com.example.trekkingapp.tourschedule.TourSchedule;
import com.example.trekkingapp.tourschedule.TourScheduleRepository;
import com.example.trekkingapp.tourschedule.TourScheduleStatus;
import com.example.trekkingapp.tourprovider.TourProvider;
import com.example.trekkingapp.tourprovider.TourProviderRepository;
import com.example.trekkingapp.user.User;
import com.example.trekkingapp.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private static final String ROLE_TREKKER = "TREKKER";
    private static final String ROLE_TOUR_PROVIDER = "TOUR_PROVIDER";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String TOUR_STATUS_DELETED = "DELETED";
    private static final String TOUR_STATUS_CANCELLED = "CANCELLED";
    private final TourScheduleRepository tourScheduleRepository;

    private final BookingRepository bookingRepository;
    private final TourRepository tourRepository;
    private final UserRepository userRepository;
    private final TourProviderRepository tourProviderRepository;
    private final CurrentUserService currentUserService;
    private final BookingStatusManager bookingStatusManager;
    private final PaymentService paymentService;

    public BookingService(
            TourScheduleRepository tourScheduleRepository,
            BookingRepository bookingRepository,
            TourRepository tourRepository,
            UserRepository userRepository,
            TourProviderRepository tourProviderRepository,
            CurrentUserService currentUserService,
            BookingStatusManager bookingStatusManager,
            PaymentService paymentService
    ) {
        this.tourScheduleRepository = tourScheduleRepository;
        this.bookingRepository = bookingRepository;
        this.tourRepository = tourRepository;
        this.userRepository = userRepository;
        this.tourProviderRepository = tourProviderRepository;
        this.currentUserService = currentUserService;
        this.bookingStatusManager = bookingStatusManager;
        this.paymentService = paymentService;
    }

    @Transactional
    public BookingResponse create(BookingRequest request) {
        User trekker = findCurrentUser();
        validateTrekkerRole(trekker);

        Tour tour = tourRepository.findByTourIdAndStatusNot(request.tourId(), TOUR_STATUS_DELETED)
                .orElseThrow(() -> new IllegalArgumentException("Tour not found"));
        validateTourAvailable(tour);

        TourSchedule schedule = tourScheduleRepository.findByScheduleIdAndTour_TourId(request.scheduleId(), request.tourId())
                .orElseThrow(() -> new IllegalArgumentException("Tour schedule not found"));

        if (schedule.getStatus() != TourScheduleStatus.OPEN) {
            throw new IllegalArgumentException("Tour schedule is not open for booking");
        }

        if (schedule.getAvailableSlots() < request.numberOfPeople()) {
            throw new IllegalArgumentException("Number of people exceeds available slots");
        }

        Booking booking = new Booking();
        booking.setTour(tour);
        booking.setSchedule(schedule);
        booking.setTrekker(trekker);
        booking.setNumberOfPeople(request.numberOfPeople());
        booking.setTotalPrice(tour.getPrice().multiply(BigDecimal.valueOf(request.numberOfPeople())));
        bookingStatusManager.markPendingPayment(booking);

        schedule.setBookedCount(schedule.getBookedCount() + request.numberOfPeople());
        if (schedule.getBookedCount() >= tour.getMaxParticipants()) {
            schedule.setStatus(TourScheduleStatus.FULL);
        }

        tourScheduleRepository.save(schedule);
        Booking savedBooking = bookingRepository.save(booking);
        CreatePaymentResult createPaymentResult = paymentService.initializePayment(savedBooking);
        log.info("booking_created bookingId={} orderCode={} bookingStatus={} paymentStatus={}",
                savedBooking.getBookingId(),
                createPaymentResult.orderCode(),
                savedBooking.getBookingStatus(),
                savedBooking.getPaymentStatus());
        return toResponse(savedBooking, createPaymentResult.checkoutUrl(), createPaymentResult.orderCode());
    }

    @Transactional
    public List<BookingResponse> findMyBookings() {
        Long currentUserId = currentUserService.getCurrentUserId();
        return bookingRepository.findByTrekker_UserId(currentUserId)
                .stream()
                .map(this::toResponseWithSync)
                .toList();
    }

    @Transactional
    public BookingResponse cancelMyBooking(Long bookingId) {
        Long currentUserId = currentUserService.getCurrentUserId();
        Booking booking = findBooking(bookingId);

        if (!booking.getTrekker().getUserId().equals(currentUserId)) {
            throw new IllegalArgumentException("You are not allowed to modify this booking");
        }

        if (booking.getBookingStatus() == BookingStatus.COMPLETED) {
            throw new IllegalArgumentException("Completed booking cannot be cancelled");
        }

        if (booking.getBookingStatus() == BookingStatus.CANCELLED) {
            throw new IllegalArgumentException("Booking already cancelled");
        }

        bookingStatusManager.cancelBooking(booking, PaymentStatus.CANCELLED);
        return toResponse(bookingRepository.save(booking));
    }

    @Transactional
    public List<BookingResponse> findProviderBookings() {
        TourProvider provider = findCurrentProvider();
        return bookingRepository.findByTour_Provider_ProviderId(provider.getProviderId())
                .stream()
                .map(this::toResponseWithSync)
                .toList();
    }

    @Transactional
    public BookingResponse getBookingById(Long bookingId) {
        User currentUser = findCurrentUser();

        Booking booking = bookingRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        if (hasRole(currentUser, ROLE_ADMIN)) {
            return toResponseWithSync(booking);
        }

        if (hasRole(currentUser, ROLE_TREKKER)
                && booking.getTrekker().getUserId().equals(currentUser.getUserId())) {
            return toResponseWithSync(booking);
        }

        if (hasRole(currentUser, ROLE_TOUR_PROVIDER)
                && booking.getTour().getProvider().getUser().getUserId().equals(currentUser.getUserId())) {
            return toResponseWithSync(booking);
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to view this booking");
    }

    @Transactional
    public BookingResponse confirmProviderBooking(Long bookingId) {
        TourProvider provider = findCurrentProvider();
        Booking booking = findBooking(bookingId);
        validateProviderOwnsBooking(booking, provider);

        if (booking.getBookingStatus() == BookingStatus.CONFIRMED) {
            return toResponse(booking);
        }

        if (booking.getPaymentStatus() != PaymentStatus.PAID) {
            throw new IllegalArgumentException("Only paid booking can be confirmed");
        }

        if (booking.getBookingStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new IllegalArgumentException("Only pending payment booking can be confirmed");
        }

        booking.setBookingStatus(BookingStatus.CONFIRMED);
        return toResponse(bookingRepository.save(booking));
    }

    @Transactional
    public BookingResponse completeProviderBooking(Long bookingId) {
        TourProvider provider = findCurrentProvider();
        Booking booking = findBooking(bookingId);
        validateProviderOwnsBooking(booking, provider);

        if (booking.getBookingStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalArgumentException("Only confirmed booking can be completed");
        }

        bookingStatusManager.markCompleted(booking);
        return toResponse(bookingRepository.save(booking));
    }

    @Transactional
    public List<BookingResponse> findAll() {
        return bookingRepository.findAll()
                .stream()
                .map(this::toResponseWithSync)
                .toList();
    }

    private User findCurrentUser() {
        Long currentUserId = currentUserService.getCurrentUserId();
        return userRepository.findById(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("Current user not found"));
    }

    private TourProvider findCurrentProvider() {
        Long currentUserId = currentUserService.getCurrentUserId();
        return tourProviderRepository.findByUser_UserId(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("Tour provider profile not found"));
    }

    private Booking findBooking(Long bookingId) {
        return bookingRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
    }

    private void validateTrekkerRole(User user) {
        if (!hasRole(user, ROLE_TREKKER)) {
            throw new IllegalArgumentException("Current user must have TREKKER role");
        }
    }

    private boolean hasRole(User user, String roleName) {
        return user.getUserRoles()
                .stream()
                .anyMatch(userRole -> roleName.equals(userRole.getRole().getRoleName()));
    }

    private void validateTourAvailable(Tour tour) {
        if (TOUR_STATUS_DELETED.equals(tour.getStatus()) || TOUR_STATUS_CANCELLED.equals(tour.getStatus())) {
            throw new IllegalArgumentException("Tour is not available for booking");
        }
    }

    private void validateProviderOwnsBooking(Booking booking, TourProvider provider) {
        Long bookingProviderId = booking.getTour().getProvider().getProviderId();
        if (!bookingProviderId.equals(provider.getProviderId())) {
            throw new IllegalArgumentException("You are not allowed to modify this booking");
        }
    }

    private BookingResponse toResponse(Booking booking) {
        Long orderCode = booking.getPayment() == null ? null : booking.getPayment().getOrderCode();
        return toResponse(booking, null, orderCode);
    }

    private BookingResponse toResponseWithSync(Booking booking) {
        if (bookingStatusManager.synchronizePaidBooking(booking)) {
            bookingRepository.save(booking);
        }
        return toResponse(booking);
    }

    private BookingResponse toResponse(Booking booking, String checkoutUrl, Long orderCode) {
        Tour tour = booking.getTour();
        TourSchedule schedule = booking.getSchedule();
        User trekker = booking.getTrekker();
        return new BookingResponse(
                booking.getBookingId(),
                tour.getTourId(),
                schedule == null ? null : schedule.getScheduleId(),
                tour.getTitle(),
                trekker.getUserId(),
                trekker.getFullName(),
                trekker.getEmail(),
                schedule == null ? null : schedule.getStartDateTime(),
                schedule == null ? null : schedule.getEndDateTime(),
                booking.getNumberOfPeople(),
                booking.getTotalPrice(),
                booking.getBookingStatus().name(),
                booking.getPaymentStatus().name(),
                booking.getBookedAt(),
                booking.getBookedAt(),
                booking.getUpdatedAt(),
                orderCode,
                checkoutUrl
        );
    }
}
