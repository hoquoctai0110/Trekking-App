package com.example.trekkingapp.review;

import com.example.trekkingapp.auth.CurrentUserService;
import com.example.trekkingapp.booking.Booking;
import com.example.trekkingapp.booking.BookingStatus;
import com.example.trekkingapp.booking.BookingRepository;
import com.example.trekkingapp.user.User;
import com.example.trekkingapp.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ReviewService {

    private static final String ROLE_TREKKER = "TREKKER";
    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    public ReviewService(
            ReviewRepository reviewRepository,
            BookingRepository bookingRepository,
            UserRepository userRepository,
            CurrentUserService currentUserService
    ) {
        this.reviewRepository = reviewRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public ReviewResponse createReview(ReviewRequest request) {
        User currentUser = findCurrentUser();
        if (!hasRole(currentUser, ROLE_TREKKER)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only trekkers can create reviews");
        }

        Booking booking = bookingRepository.findByBookingId(request.bookingId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        if (!booking.getTrekker().getUserId().equals(currentUser.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to review this booking");
        }

        if (!booking.getTour().getTourId().equals(request.tourId())) {
            throw new IllegalArgumentException("Booking does not belong to the requested tour");
        }

        if (booking.getBookingStatus() != BookingStatus.COMPLETED) {
            throw new IllegalArgumentException("Booking must be completed before review");
        }

        if (reviewRepository.existsByBooking_BookingId(booking.getBookingId())) {
            throw new IllegalArgumentException("Booking has already been reviewed");
        }

        Review review = new Review();
        review.setBooking(booking);
        review.setTour(booking.getTour());
        review.setUser(currentUser);
        review.setRating(request.rating());
        review.setComment(request.comment());

        return toResponse(reviewRepository.save(review));
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsByTour(Long tourId) {
        return reviewRepository.findByTour_TourIdOrderByCreatedAtDesc(tourId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReviewSummaryResponse getTourReviewSummary(Long tourId) {
        List<Review> reviews = reviewRepository.findByTour_TourId(tourId);
        long reviewCount = reviews.size();
        double averageRating = reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);

        return new ReviewSummaryResponse(tourId, averageRating, reviewCount);
    }

    private User findCurrentUser() {
        Long currentUserId = currentUserService.getCurrentUserId();
        return userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Current user not found"));
    }

    private boolean hasRole(User user, String roleName) {
        return user.getUserRoles()
                .stream()
                .anyMatch(userRole -> roleName.equals(userRole.getRole().getRoleName()));
    }

    private ReviewResponse toResponse(Review review) {
        User user = review.getUser();
        return new ReviewResponse(
                review.getReviewId(),
                review.getBooking().getBookingId(),
                review.getTour().getTourId(),
                user.getUserId(),
                user.getFullName(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt()
        );
    }
}
