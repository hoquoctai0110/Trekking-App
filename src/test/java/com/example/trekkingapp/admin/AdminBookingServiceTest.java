package com.example.trekkingapp.admin;

import com.example.trekkingapp.admin.audit.AdminAuditService;
import com.example.trekkingapp.admin.dto.request.AdminActionReasonRequest;
import com.example.trekkingapp.admin.dto.response.AdminBookingResponse;
import com.example.trekkingapp.admin.service.AdminBookingService;
import com.example.trekkingapp.booking.Booking;
import com.example.trekkingapp.booking.BookingRepository;
import com.example.trekkingapp.booking.BookingStatus;
import com.example.trekkingapp.booking.BookingStatusManager;
import com.example.trekkingapp.booking.PaymentStatus;
import com.example.trekkingapp.common.ConflictException;
import com.example.trekkingapp.payment.Payment;
import com.example.trekkingapp.payment.PaymentRepository;
import com.example.trekkingapp.tour.Tour;
import com.example.trekkingapp.tourprovider.TourProvider;
import com.example.trekkingapp.user.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminBookingServiceTest {

    @Test
    void refundBookingIsIdempotentWhenRefundAlreadyRequested() {
        BookingRepository bookingRepository = mock(BookingRepository.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        BookingStatusManager bookingStatusManager = mock(BookingStatusManager.class);
        AdminAuditService adminAuditService = mock(AdminAuditService.class);
        AdminBookingService service = new AdminBookingService(bookingRepository, paymentRepository, bookingStatusManager, adminAuditService);

        Booking booking = createBooking();
        Payment payment = createPayment(booking);
        payment.setRefundStatus("PENDING_MANUAL");

        when(bookingRepository.findWithLockByBookingId(booking.getBookingId())).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBooking_BookingId(booking.getBookingId())).thenReturn(Optional.of(payment));

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> service.refundBooking(booking.getBookingId(), new AdminActionReasonRequest("manual refund"))
        );

        assertEquals("Refund has already been requested", exception.getMessage());
    }

    @Test
    void cancelBookingReleasesSlotsOnce() {
        BookingRepository bookingRepository = mock(BookingRepository.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        BookingStatusManager bookingStatusManager = mock(BookingStatusManager.class);
        AdminAuditService adminAuditService = mock(AdminAuditService.class);
        AdminBookingService service = new AdminBookingService(bookingRepository, paymentRepository, bookingStatusManager, adminAuditService);

        Booking booking = createBooking();
        when(bookingRepository.findWithLockByBookingId(booking.getBookingId())).thenReturn(Optional.of(booking));
        doAnswer(invocation -> {
            Booking value = invocation.getArgument(0);
            value.setBookingStatus(BookingStatus.CANCELLED);
            value.setPaymentStatus(PaymentStatus.CANCELLED);
            return null;
        }).when(bookingStatusManager).cancelBooking(booking, PaymentStatus.CANCELLED);

        AdminBookingResponse response = service.cancelBooking(booking.getBookingId(), new AdminActionReasonRequest("admin cancel"));

        assertEquals("CANCELLED", response.bookingStatus());
        assertEquals("CANCELLED", response.paymentStatus());
        verify(bookingStatusManager).cancelBooking(booking, PaymentStatus.CANCELLED);
    }

    private Booking createBooking() {
        TourProvider provider = new TourProvider();
        provider.setCompanyName("Provider");

        Tour tour = new Tour();
        tour.setTitle("Tour");
        tour.setProvider(provider);

        User trekker = new User();
        trekker.setFullName("User");

        Booking booking = new Booking();
        booking.setBookingId(10L);
        booking.setBookingStatus(BookingStatus.PENDING_PAYMENT);
        booking.setPaymentStatus(PaymentStatus.PENDING);
        booking.setTour(tour);
        booking.setTrekker(trekker);
        booking.setTotalPrice(BigDecimal.valueOf(100));
        booking.setNumberOfPeople(2);
        return booking;
    }

    private Payment createPayment(Booking booking) {
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setStatus(PaymentStatus.PAID);
        payment.setAmount(BigDecimal.valueOf(100));
        booking.setPayment(payment);
        return payment;
    }
}
