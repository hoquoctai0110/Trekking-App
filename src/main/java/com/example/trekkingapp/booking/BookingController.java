package com.example.trekkingapp.booking;

import com.example.trekkingapp.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    @PreAuthorize("hasRole('TREKKER')")
    public ApiResponse<BookingResponse> createBooking(@Valid @RequestBody BookingRequest request) {
        return new ApiResponse<>(true, "Booking created successfully", bookingService.create(request));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('TREKKER')")
    public ApiResponse<List<BookingResponse>> getMyBookings() {
        return new ApiResponse<>(true, "Bookings retrieved successfully", bookingService.findMyBookings());
    }

    @PutMapping("/me/{bookingId}/cancel")
    @PreAuthorize("hasRole('TREKKER')")
    public ApiResponse<BookingResponse> cancelMyBooking(@PathVariable Long bookingId) {
        return new ApiResponse<>(true, "Booking cancelled successfully", bookingService.cancelMyBooking(bookingId));
    }

    @GetMapping("/provider")
    @PreAuthorize("hasRole('TOUR_PROVIDER')")
    public ApiResponse<List<BookingResponse>> getProviderBookings() {
        return new ApiResponse<>(true, "Bookings retrieved successfully", bookingService.findProviderBookings());
    }

    @PutMapping("/provider/{bookingId}/confirm")
    @PreAuthorize("hasRole('TOUR_PROVIDER')")
    public ApiResponse<BookingResponse> confirmProviderBooking(@PathVariable Long bookingId) {
        return new ApiResponse<>(true, "Booking confirmed successfully", bookingService.confirmProviderBooking(bookingId));
    }

    @PutMapping("/provider/{bookingId}/complete")
    @PreAuthorize("hasRole('TOUR_PROVIDER')")
    public ApiResponse<BookingResponse> completeProviderBooking(@PathVariable Long bookingId) {
        return new ApiResponse<>(true, "Booking completed successfully", bookingService.completeProviderBooking(bookingId));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<BookingResponse>> getBookings() {
        return new ApiResponse<>(true, "Bookings retrieved successfully", bookingService.findAll());
    }
}
