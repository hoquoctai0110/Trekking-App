package com.example.trekkingapp.review;

import com.example.trekkingapp.auth.CurrentUserService;
import com.example.trekkingapp.booking.Booking;
import com.example.trekkingapp.booking.BookingStatus;
import com.example.trekkingapp.booking.BookingRepository;
import com.example.trekkingapp.role.Role;
import com.example.trekkingapp.role.UserRole;
import com.example.trekkingapp.tour.Tour;
import com.example.trekkingapp.user.User;
import com.example.trekkingapp.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private ReviewService reviewService;

    @Test
    void createReviewFailsWhenBookingIsPending() {
        User trekker = createTrekker(10L);
        Booking booking = createBooking(12L, 20L, trekker, "PENDING_PAYMENT");

        when(currentUserService.getCurrentUserId()).thenReturn(trekker.getUserId());
        when(userRepository.findById(trekker.getUserId())).thenReturn(Optional.of(trekker));
        when(bookingRepository.findByBookingId(booking.getBookingId())).thenReturn(Optional.of(booking));

        ReviewRequest request = new ReviewRequest(booking.getBookingId(), 20L, 5, "Great route");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> reviewService.createReview(request)
        );

        assertEquals("Booking must be completed before review", exception.getMessage());
    }

    @Test
    void createReviewSucceedsWhenBookingIsCompleted() {
        User trekker = createTrekker(10L);
        Booking booking = createBooking(12L, 20L, trekker, "COMPLETED");

        when(currentUserService.getCurrentUserId()).thenReturn(trekker.getUserId());
        when(userRepository.findById(trekker.getUserId())).thenReturn(Optional.of(trekker));
        when(bookingRepository.findByBookingId(booking.getBookingId())).thenReturn(Optional.of(booking));
        when(reviewRepository.existsByBooking_BookingId(booking.getBookingId())).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review review = invocation.getArgument(0);
            review.setReviewId(30L);
            review.setCreatedAt(LocalDateTime.of(2026, 6, 20, 12, 0));
            return review;
        });

        ReviewResponse response = reviewService.createReview(new ReviewRequest(12L, 20L, 5, "Great route"));

        assertEquals(30L, response.reviewId());
        assertEquals(12L, response.bookingId());
        assertEquals(20L, response.tourId());
        assertEquals(10L, response.userId());
        assertEquals(5, response.rating());
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    void createReviewFailsWhenBookingAlreadyHasReview() {
        User trekker = createTrekker(10L);
        Booking booking = createBooking(12L, 20L, trekker, "COMPLETED");

        when(currentUserService.getCurrentUserId()).thenReturn(trekker.getUserId());
        when(userRepository.findById(trekker.getUserId())).thenReturn(Optional.of(trekker));
        when(bookingRepository.findByBookingId(booking.getBookingId())).thenReturn(Optional.of(booking));
        when(reviewRepository.existsByBooking_BookingId(booking.getBookingId())).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> reviewService.createReview(new ReviewRequest(12L, 20L, 5, "Great route"))
        );

        assertEquals("Booking has already been reviewed", exception.getMessage());
    }

    @Test
    void getReviewsByTourReturnsRepositoryOrder() {
        User trekker = createTrekker(10L);
        Booking booking = createBooking(12L, 20L, trekker, "COMPLETED");
        Review review = new Review();
        review.setReviewId(30L);
        review.setBooking(booking);
        review.setTour(booking.getTour());
        review.setUser(trekker);
        review.setRating(4);
        review.setComment("Good trip");
        review.setCreatedAt(LocalDateTime.of(2026, 6, 20, 12, 0));

        when(reviewRepository.findByTour_TourIdOrderByCreatedAtDesc(20L)).thenReturn(List.of(review));

        List<ReviewResponse> responses = reviewService.getReviewsByTour(20L);

        assertEquals(1, responses.size());
        assertEquals(30L, responses.getFirst().reviewId());
        assertEquals("Good trip", responses.getFirst().comment());
    }

    private User createTrekker(Long userId) {
        Role role = new Role();
        role.setRoleName("TREKKER");

        User user = new User();
        user.setUserId(userId);
        user.setFullName("Trekker One");
        user.setEmail("trekker@example.com");

        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(role);
        user.setUserRoles(List.of(userRole));
        return user;
    }

    private Booking createBooking(Long bookingId, Long tourId, User trekker, String bookingStatus) {
        Tour tour = new Tour();
        tour.setTourId(tourId);
        tour.setTitle("Mountain Trek");

        Booking booking = new Booking();
        booking.setBookingId(bookingId);
        booking.setTour(tour);
        booking.setTrekker(trekker);
        booking.setBookingStatus(BookingStatus.valueOf(bookingStatus));
        return booking;
    }
}
