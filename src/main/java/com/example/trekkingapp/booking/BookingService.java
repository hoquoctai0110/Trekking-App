package com.example.trekkingapp.booking;

import com.example.trekkingapp.auth.CurrentUserService;
import com.example.trekkingapp.tour.Tour;
import com.example.trekkingapp.tour.TourRepository;
import com.example.trekkingapp.tourprovider.TourProvider;
import com.example.trekkingapp.tourprovider.TourProviderRepository;
import com.example.trekkingapp.user.User;
import com.example.trekkingapp.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class BookingService {

    private static final String ROLE_TREKKER = "TREKKER";
    private static final String TOUR_STATUS_DELETED = "DELETED";
    private static final String TOUR_STATUS_CANCELLED = "CANCELLED";
    private static final String BOOKING_STATUS_PENDING = "PENDING";
    private static final String BOOKING_STATUS_CONFIRMED = "CONFIRMED";
    private static final String BOOKING_STATUS_COMPLETED = "COMPLETED";
    private static final String BOOKING_STATUS_CANCELLED = "CANCELLED";
    private static final String PAYMENT_STATUS_UNPAID = "UNPAID";

    private final BookingRepository bookingRepository;
    private final TourRepository tourRepository;
    private final UserRepository userRepository;
    private final TourProviderRepository tourProviderRepository;
    private final CurrentUserService currentUserService;

    public BookingService(
            BookingRepository bookingRepository,
            TourRepository tourRepository,
            UserRepository userRepository,
            TourProviderRepository tourProviderRepository,
            CurrentUserService currentUserService
    ) {
        this.bookingRepository = bookingRepository;
        this.tourRepository = tourRepository;
        this.userRepository = userRepository;
        this.tourProviderRepository = tourProviderRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public BookingResponse create(BookingRequest request) {
        User trekker = findCurrentUser();
        validateTrekkerRole(trekker);

        Tour tour = tourRepository.findById(request.tourId())
                .orElseThrow(() -> new IllegalArgumentException("Tour not found"));
        validateTourAvailable(tour);

        if (request.numberOfPeople() > tour.getMaxParticipants()) {
            throw new IllegalArgumentException("Number of people exceeds maximum participants");
        }

        Booking booking = new Booking();
        booking.setTour(tour);
        booking.setTrekker(trekker);
        booking.setNumberOfPeople(request.numberOfPeople());
        booking.setTotalPrice(tour.getPrice().multiply(BigDecimal.valueOf(request.numberOfPeople())));
        booking.setBookingStatus(BOOKING_STATUS_PENDING);
        booking.setPaymentStatus(PAYMENT_STATUS_UNPAID);

        return toResponse(bookingRepository.save(booking));
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> findMyBookings() {
        Long currentUserId = currentUserService.getCurrentUserId();
        return bookingRepository.findByTrekker_UserId(currentUserId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public BookingResponse cancelMyBooking(Long bookingId) {
        Long currentUserId = currentUserService.getCurrentUserId();
        Booking booking = findBooking(bookingId);

        if (!booking.getTrekker().getUserId().equals(currentUserId)) {
            throw new IllegalArgumentException("You are not allowed to modify this booking");
        }

        if (BOOKING_STATUS_COMPLETED.equals(booking.getBookingStatus())) {
            throw new IllegalArgumentException("Completed booking cannot be cancelled");
        }

        if (BOOKING_STATUS_CANCELLED.equals(booking.getBookingStatus())) {
            throw new IllegalArgumentException("Booking already cancelled");
        }

        booking.setBookingStatus(BOOKING_STATUS_CANCELLED);
        return toResponse(bookingRepository.save(booking));
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> findProviderBookings() {
        TourProvider provider = findCurrentProvider();
        return bookingRepository.findByTour_Provider_ProviderId(provider.getProviderId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public BookingResponse confirmProviderBooking(Long bookingId) {
        TourProvider provider = findCurrentProvider();
        Booking booking = findBooking(bookingId);
        validateProviderOwnsBooking(booking, provider);

        if (!BOOKING_STATUS_PENDING.equals(booking.getBookingStatus())) {
            throw new IllegalArgumentException("Only pending booking can be confirmed");
        }

        booking.setBookingStatus(BOOKING_STATUS_CONFIRMED);
        return toResponse(bookingRepository.save(booking));
    }

    @Transactional
    public BookingResponse completeProviderBooking(Long bookingId) {
        TourProvider provider = findCurrentProvider();
        Booking booking = findBooking(bookingId);
        validateProviderOwnsBooking(booking, provider);

        if (!BOOKING_STATUS_CONFIRMED.equals(booking.getBookingStatus())) {
            throw new IllegalArgumentException("Only confirmed booking can be completed");
        }

        booking.setBookingStatus(BOOKING_STATUS_COMPLETED);
        return toResponse(bookingRepository.save(booking));
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> findAll() {
        return bookingRepository.findAll()
                .stream()
                .map(this::toResponse)
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
        boolean hasTrekkerRole = user.getUserRoles()
                .stream()
                .anyMatch(userRole -> ROLE_TREKKER.equals(userRole.getRole().getRoleName()));

        if (!hasTrekkerRole) {
            throw new IllegalArgumentException("Current user must have TREKKER role");
        }
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
        Tour tour = booking.getTour();
        User trekker = booking.getTrekker();
        return new BookingResponse(
                booking.getBookingId(),
                tour.getTourId(),
                tour.getTitle(),
                trekker.getUserId(),
                trekker.getEmail(),
                booking.getNumberOfPeople(),
                booking.getTotalPrice(),
                booking.getBookingStatus(),
                booking.getPaymentStatus(),
                booking.getBookedAt(),
                booking.getUpdatedAt()
        );
    }
}
