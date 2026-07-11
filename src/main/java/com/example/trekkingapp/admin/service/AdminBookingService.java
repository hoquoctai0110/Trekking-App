package com.example.trekkingapp.admin.service;

import com.example.trekkingapp.admin.AdminSortUtils;
import com.example.trekkingapp.admin.audit.AdminAuditService;
import com.example.trekkingapp.admin.dto.request.AdminActionReasonRequest;
import com.example.trekkingapp.admin.dto.response.AdminBookingResponse;
import com.example.trekkingapp.admin.dto.response.PageResponse;
import com.example.trekkingapp.admin.specification.AdminBookingSpecification;
import com.example.trekkingapp.booking.Booking;
import com.example.trekkingapp.booking.BookingRepository;
import com.example.trekkingapp.booking.BookingStatus;
import com.example.trekkingapp.booking.BookingStatusManager;
import com.example.trekkingapp.booking.PaymentStatus;
import com.example.trekkingapp.common.ConflictException;
import com.example.trekkingapp.common.ResourceNotFoundException;
import com.example.trekkingapp.common.ValidationException;
import com.example.trekkingapp.payment.Payment;
import com.example.trekkingapp.payment.PaymentRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Service
public class AdminBookingService {

    private static final Set<String> SORT_FIELDS = Set.of("bookedAt", "updatedAt", "totalPrice", "bookingStatus", "paymentStatus");

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final BookingStatusManager bookingStatusManager;
    private final AdminAuditService adminAuditService;

    public AdminBookingService(
            BookingRepository bookingRepository,
            PaymentRepository paymentRepository,
            BookingStatusManager bookingStatusManager,
            AdminAuditService adminAuditService
    ) {
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.bookingStatusManager = bookingStatusManager;
        this.adminAuditService = adminAuditService;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminBookingResponse> findBookings(
            int page,
            int size,
            String sort,
            String search,
            String status,
            String paymentStatus,
            LocalDate dateFrom,
            LocalDate dateTo,
            Long tourId,
            Long tourProviderId,
            Long userId
    ) {
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new ValidationException("dateFrom must be before or equal to dateTo");
        }
        Pageable pageable = AdminSortUtils.pageable(page, size, sort, "bookedAt", SORT_FIELDS);
        Specification<Booking> specification = Specification
                .where(AdminBookingSpecification.search(search))
                .and(AdminBookingSpecification.bookingStatus(status))
                .and(AdminBookingSpecification.paymentStatus(paymentStatus))
                .and(AdminBookingSpecification.dateFrom(dateFrom == null ? null : dateFrom.atStartOfDay()))
                .and(AdminBookingSpecification.dateTo(dateTo == null ? null : dateTo.plusDays(1).atStartOfDay().minusNanos(1)))
                .and(AdminBookingSpecification.tourId(tourId))
                .and(AdminBookingSpecification.providerId(tourProviderId))
                .and(AdminBookingSpecification.userId(userId));
        return PageResponse.from(bookingRepository.findAll(specification, pageable).map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public AdminBookingResponse getBooking(Long id) {
        return toResponse(findBooking(id));
    }

    @Transactional
    public AdminBookingResponse cancelBooking(Long id, AdminActionReasonRequest request) {
        Booking booking = bookingRepository.findWithLockByBookingId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (booking.getBookingStatus() == BookingStatus.COMPLETED) {
            throw new ConflictException("Completed booking cannot be cancelled");
        }
        if (booking.getBookingStatus() == BookingStatus.CANCELLED) {
            throw new ConflictException("Booking already cancelled");
        }

        BookingStatus oldStatus = booking.getBookingStatus();
        PaymentStatus oldPaymentStatus = booking.getPaymentStatus();
        PaymentStatus nextPaymentStatus = booking.getPaymentStatus() == PaymentStatus.PAID ? PaymentStatus.PAID : PaymentStatus.CANCELLED;
        bookingStatusManager.cancelBooking(booking, nextPaymentStatus);
        bookingRepository.save(booking);
        adminAuditService.log("CANCEL_BOOKING", "BOOKING", booking.getBookingId(), oldStatus + "|" + oldPaymentStatus, booking.getBookingStatus() + "|" + booking.getPaymentStatus(), request.reason());
        return toResponse(booking);
    }

    @Transactional
    public AdminBookingResponse refundBooking(Long id, AdminActionReasonRequest request) {
        Booking booking = bookingRepository.findWithLockByBookingId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        Payment payment = paymentRepository.findByBooking_BookingId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        if (payment.getStatus() != PaymentStatus.PAID) {
            throw new ConflictException("Only paid booking can be refunded");
        }
        if (payment.getRefundStatus() != null && !payment.getRefundStatus().isBlank()) {
            throw new ConflictException("Refund has already been requested");
        }

        if (booking.getBookingStatus() != BookingStatus.CANCELLED) {
            bookingStatusManager.cancelBooking(booking, PaymentStatus.PAID);
            bookingRepository.save(booking);
        }

        payment.setRefundStatus("PENDING_MANUAL");
        payment.setRefundReason(request.reason());
        payment.setRefundRequestedAt(LocalDateTime.now());
        paymentRepository.save(payment);
        adminAuditService.log("REFUND_BOOKING", "BOOKING", booking.getBookingId(), "PAID", "PENDING_MANUAL", request.reason());
        return toResponse(booking);
    }

    private Booking findBooking(Long id) {
        return bookingRepository.findByBookingId(id).orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
    }

    private AdminBookingResponse toResponse(Booking booking) {
        Payment payment = booking.getPayment();
        return new AdminBookingResponse(
                booking.getBookingId(),
                booking.getTrekker().getFullName(),
                booking.getTour().getTitle(),
                booking.getTour().getProvider().getCompanyName(),
                booking.getSchedule() == null ? null : booking.getSchedule().getStartDateTime() + " - " + booking.getSchedule().getEndDateTime(),
                booking.getBookedAt(),
                booking.getNumberOfPeople(),
                booking.getTotalPrice(),
                booking.getPaymentStatus().name(),
                booking.getBookingStatus().name(),
                booking.getBookedAt(),
                null,
                payment == null ? null : payment.getRefundStatus()
        );
    }
}
