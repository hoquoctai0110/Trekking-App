package com.example.trekkingapp.payment;

import com.example.trekkingapp.auth.CurrentUserService;
import com.example.trekkingapp.booking.Booking;
import com.example.trekkingapp.booking.BookingRepository;
import com.example.trekkingapp.booking.BookingStatus;
import com.example.trekkingapp.booking.BookingStatusManager;
import com.example.trekkingapp.booking.PaymentStatus;
import com.example.trekkingapp.tourprovider.TourProviderRepository;
import com.example.trekkingapp.user.User;
import com.example.trekkingapp.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingStatusManager bookingStatusManager;

    @Mock
    private PayOSClient payOSClient;

    @Mock
    private PayOSProperties payOSProperties;

    @Mock
    private PayOSSignatureService payOSSignatureService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TourProviderRepository tourProviderRepository;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
                paymentRepository,
                bookingRepository,
                bookingStatusManager,
                payOSClient,
                payOSProperties,
                payOSSignatureService,
                currentUserService,
                userRepository,
                tourProviderRepository
        );

        lenient().doAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            booking.setPaymentStatus(PaymentStatus.PAID);
            if (booking.getBookingStatus() == BookingStatus.PENDING_PAYMENT
                    || booking.getBookingStatus() == BookingStatus.PENDING) {
                booking.setBookingStatus(BookingStatus.CONFIRMED);
            }
            return true;
        }).when(bookingStatusManager).synchronizePaidBooking(any(Booking.class));

        lenient().doAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            PaymentStatus paymentStatus = invocation.getArgument(1);
            booking.setBookingStatus(BookingStatus.CANCELLED);
            booking.setPaymentStatus(paymentStatus);
            return null;
        }).when(bookingStatusManager).cancelBooking(any(Booking.class), any(PaymentStatus.class));
    }

    @Test
    void returnEndpointMarksPaymentAndBookingPaid() {
        Booking booking = createBooking(12L, BookingStatus.PENDING_PAYMENT, PaymentStatus.PENDING);
        Payment payment = createPayment(1L, 10001L, PaymentStatus.PENDING, booking);

        when(paymentRepository.findByOrderCode(10001L)).thenReturn(Optional.of(payment));

        PaymentReturnResponse response = paymentService.handlePaymentReturn(Map.of(
                "orderCode", "10001",
                "status", "PAID",
                "code", "00",
                "id", "txn-123"
        ));

        assertEquals(1L, response.paymentId());
        assertEquals(12L, response.bookingId());
        assertEquals("PAID", response.status());
        assertEquals("PAID", response.paymentStatus());
        assertEquals("CONFIRMED", response.bookingStatus());
        assertEquals("10001", response.orderCode());
        assertEquals(true, response.success());
        assertEquals(PaymentStatus.PAID, payment.getStatus());
        assertEquals("txn-123", payment.getTransactionId());
        verify(bookingStatusManager).synchronizePaidBooking(booking);
        verify(bookingRepository).save(booking);
        verify(paymentRepository).save(payment);
    }

    @Test
    void webhookRepairsBookingWhenPaymentAlreadyPaidButBookingStillPending() {
        Booking booking = createBooking(12L, BookingStatus.PENDING_PAYMENT, PaymentStatus.PENDING);
        Payment payment = createPayment(1L, 10003L, PaymentStatus.PAID, booking);
        PayOSWebhookRequest request = new PayOSWebhookRequest(
                "00",
                "Success",
                true,
                "signed",
                new PayOSWebhookRequest.PayOSWebhookData(
                        10003L,
                        BigDecimal.valueOf(500000),
                        "CTBOOK12",
                        "plink-1",
                        "txn-456",
                        null,
                        "VND",
                        "00",
                        "Success",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                )
        );

        when(payOSProperties.getClientId()).thenReturn("client");
        when(payOSProperties.getApiKey()).thenReturn("api");
        when(payOSProperties.getChecksumKey()).thenReturn("checksum");
        when(payOSProperties.getReturnUrl()).thenReturn("https://return");
        when(payOSProperties.getCancelUrl()).thenReturn("https://cancel");
        when(payOSSignatureService.verify(any(), eq("checksum"), eq("signed"))).thenReturn(true);
        when(paymentRepository.findByOrderCode(10003L)).thenReturn(Optional.of(payment));

        paymentService.handlePayOSWebhook(request);

        assertEquals(PaymentStatus.PAID, booking.getPaymentStatus());
        assertEquals(BookingStatus.CONFIRMED, booking.getBookingStatus());
        assertEquals("txn-456", payment.getTransactionId());
        verify(bookingRepository).save(booking);
        verify(paymentRepository).save(payment);
    }

    @Test
    void syncPaymentStatusMarksPaymentAndBookingPaidFromRemoteStatus() {
        Booking booking = createBooking(12L, BookingStatus.PENDING_PAYMENT, PaymentStatus.PENDING);
        Payment payment = createPayment(1L, 10004L, PaymentStatus.PENDING, booking);
        User trekker = createUser(77L);
        booking.setTrekker(trekker);

        when(payOSProperties.getClientId()).thenReturn("client");
        when(payOSProperties.getApiKey()).thenReturn("api");
        when(payOSProperties.getChecksumKey()).thenReturn("checksum");
        when(payOSProperties.getReturnUrl()).thenReturn("https://return");
        when(payOSProperties.getCancelUrl()).thenReturn("https://cancel");
        when(paymentRepository.findByOrderCode(10004L)).thenReturn(Optional.of(payment));
        when(currentUserService.getCurrentUserId()).thenReturn(77L);
        when(userRepository.findById(77L)).thenReturn(Optional.of(trekker));
        when(payOSClient.getPaymentLinkInformation(10004L)).thenReturn(
                new PayOSGetPaymentLinkResponse(
                        "00",
                        "success",
                        new PayOSGetPaymentLinkResponse.PayOSGetPaymentLinkData(
                                "plink-10004",
                                10004L,
                                BigDecimal.valueOf(500000),
                                BigDecimal.valueOf(500000),
                                BigDecimal.ZERO,
                                "PAID",
                                "2024-01-15T10:30:00.000Z"
                        ),
                        "sig"
                )
        );

        PaymentSyncResponse response = paymentService.syncPaymentStatus(10004L);

        assertEquals("PAID", response.localPaymentStatus());
        assertEquals("CONFIRMED", response.localBookingStatus());
        assertEquals("PAID", response.remoteStatus());
        assertEquals(PaymentStatus.PAID, payment.getStatus());
        assertEquals(PaymentStatus.PAID, booking.getPaymentStatus());
        assertEquals(BookingStatus.CONFIRMED, booking.getBookingStatus());
        assertEquals("plink-10004", payment.getPayosPaymentLinkId());
        assertEquals("plink-10004", payment.getTransactionId());
        verify(bookingRepository).save(booking);
        verify(paymentRepository).save(payment);
    }

    @Test
    void syncPaymentStatusCancelsBookingForRemoteCancelledStatus() {
        Booking booking = createBooking(12L, BookingStatus.PENDING_PAYMENT, PaymentStatus.PENDING);
        Payment payment = createPayment(1L, 10005L, PaymentStatus.PENDING, booking);
        User trekker = createUser(77L);
        booking.setTrekker(trekker);

        when(payOSProperties.getClientId()).thenReturn("client");
        when(payOSProperties.getApiKey()).thenReturn("api");
        when(payOSProperties.getChecksumKey()).thenReturn("checksum");
        when(payOSProperties.getReturnUrl()).thenReturn("https://return");
        when(payOSProperties.getCancelUrl()).thenReturn("https://cancel");
        when(paymentRepository.findByOrderCode(10005L)).thenReturn(Optional.of(payment));
        when(currentUserService.getCurrentUserId()).thenReturn(77L);
        when(userRepository.findById(77L)).thenReturn(Optional.of(trekker));
        when(payOSClient.getPaymentLinkInformation(10005L)).thenReturn(
                new PayOSGetPaymentLinkResponse(
                        "00",
                        "success",
                        new PayOSGetPaymentLinkResponse.PayOSGetPaymentLinkData(
                                "plink-10005",
                                10005L,
                                BigDecimal.valueOf(500000),
                                BigDecimal.ZERO,
                                BigDecimal.valueOf(500000),
                                "CANCELLED",
                                "2024-01-15T10:30:00.000Z"
                        ),
                        "sig"
                )
        );

        PaymentSyncResponse response = paymentService.syncPaymentStatus(10005L);

        assertEquals("CANCELLED", response.localPaymentStatus());
        assertEquals("CANCELLED", response.localBookingStatus());
        assertEquals("CANCELLED", response.remoteStatus());
        assertEquals(PaymentStatus.CANCELLED, payment.getStatus());
        assertEquals(PaymentStatus.CANCELLED, booking.getPaymentStatus());
        assertEquals(BookingStatus.CANCELLED, booking.getBookingStatus());
        verify(bookingRepository).save(booking);
        verify(paymentRepository).save(payment);
    }

    @Test
    void cancelEndpointMarksPaymentCancelledWithoutChangingBookingToPaid() {
        Booking booking = createBooking(12L, BookingStatus.PENDING_PAYMENT, PaymentStatus.PENDING);
        Payment payment = createPayment(1L, 10002L, PaymentStatus.PENDING, booking);

        when(paymentRepository.findByOrderCode(10002L)).thenReturn(Optional.of(payment));

        PaymentReturnResponse response = paymentService.handlePaymentCancel(Map.of(
                "orderCode", "10002",
                "cancel", "true"
        ));

        assertEquals("CANCELLED", response.status());
        assertEquals("PENDING_PAYMENT", response.bookingStatus());
        assertEquals("UNPAID", response.paymentStatus());
        assertEquals(false, response.success());
        assertEquals(PaymentStatus.CANCELLED, payment.getStatus());
        verify(paymentRepository).save(payment);
        verify(bookingStatusManager, never()).synchronizePaidBooking(any(Booking.class));
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    private Booking createBooking(Long bookingId, BookingStatus bookingStatus, PaymentStatus paymentStatus) {
        Booking booking = new Booking();
        booking.setBookingId(bookingId);
        booking.setBookingStatus(bookingStatus);
        booking.setPaymentStatus(paymentStatus);
        return booking;
    }

    private Payment createPayment(Long paymentId, Long orderCode, PaymentStatus status, Booking booking) {
        Payment payment = new Payment();
        payment.setId(paymentId);
        payment.setOrderCode(orderCode);
        payment.setStatus(status);
        payment.setBooking(booking);
        payment.setAmount(BigDecimal.valueOf(500000));
        payment.setCurrency("VND");
        booking.setPayment(payment);
        return payment;
    }

    private User createUser(Long userId) {
        User user = new User();
        user.setUserId(userId);
        return user;
    }
}
