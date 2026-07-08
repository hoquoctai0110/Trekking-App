package com.example.trekkingapp.payment;

import com.example.trekkingapp.auth.CurrentUserService;
import com.example.trekkingapp.booking.Booking;
import com.example.trekkingapp.booking.BookingStatus;
import com.example.trekkingapp.booking.BookingStatusManager;
import com.example.trekkingapp.booking.BookingRepository;
import com.example.trekkingapp.booking.PaymentStatus;
import com.example.trekkingapp.tourprovider.TourProvider;
import com.example.trekkingapp.tourprovider.TourProviderRepository;
import com.example.trekkingapp.user.User;
import com.example.trekkingapp.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Locale;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final String CURRENCY_VND = "VND";

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final BookingStatusManager bookingStatusManager;
    private final PayOSClient payOSClient;
    private final PayOSProperties payOSProperties;
    private final PayOSSignatureService payOSSignatureService;
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final TourProviderRepository tourProviderRepository;

    public PaymentService(
            PaymentRepository paymentRepository,
            BookingRepository bookingRepository,
            BookingStatusManager bookingStatusManager,
            PayOSClient payOSClient,
            PayOSProperties payOSProperties,
            PayOSSignatureService payOSSignatureService,
            CurrentUserService currentUserService,
            UserRepository userRepository,
            TourProviderRepository tourProviderRepository
    ) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.bookingStatusManager = bookingStatusManager;
        this.payOSClient = payOSClient;
        this.payOSProperties = payOSProperties;
        this.payOSSignatureService = payOSSignatureService;
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
        this.tourProviderRepository = tourProviderRepository;
    }

    @Transactional
    public CreatePaymentResult initializePayment(Booking booking) {
        validatePayableBooking(booking);
        validateConfiguration();

        Long orderCode = generateOrderCode(booking.getBookingId());
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setOrderCode(orderCode);
        payment.setAmount(booking.getTotalPrice());
        payment.setCurrency(CURRENCY_VND);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setPaymentMethod(PaymentMethod.PAYOS);

        int amount = booking.getTotalPrice().intValueExact();
        int itemPrice = booking.getTour().getPrice().intValueExact();
        PayOSCreatePaymentRequest unsignedRequest = new PayOSCreatePaymentRequest(
                orderCode,
                amount,
                buildDescription(booking),
                booking.getTrekker().getFullName(),
                booking.getTrekker().getEmail(),
                booking.getTrekker().getPhone(),
                payOSProperties.getReturnUrl(),
                payOSProperties.getCancelUrl(),
                List.of(new PayOSPaymentItem(booking.getTour().getTitle(), booking.getNumberOfPeople(), itemPrice)),
                null
        );

        PayOSCreatePaymentResponse response = payOSClient.createPaymentLink(unsignedRequest);
        if (response == null || response.data() == null || response.data().checkoutUrl() == null) {
            throw new IllegalStateException("PayOS did not return a checkout URL");
        }

        payment.setPayosPaymentLinkId(response.data().paymentLinkId());
        paymentRepository.save(payment);
        log.info("payment_link_created bookingId={} orderCode={} paymentLinkId={}",
                booking.getBookingId(), orderCode, response.data().paymentLinkId());

        return new CreatePaymentResult(orderCode, response.data().checkoutUrl());
    }

    @Transactional
    public void handlePayOSWebhook(PayOSWebhookRequest request) {
        validateConfiguration();
        boolean validSignature = payOSSignatureService.verify(
                payOSSignatureService.buildWebhookFields(request.data()),
                payOSProperties.getChecksumKey(),
                request.signature()
        );

        if (!validSignature) {
            throw new IllegalArgumentException("Invalid PayOS signature");
        }

        Payment payment = paymentRepository.findByOrderCode(request.data().orderCode())
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
        Booking booking = payment.getBooking();

        if (payment.getAmount().compareTo(request.data().amount()) != 0) {
            throw new IllegalArgumentException("Webhook amount does not match booking amount");
        }

        PaymentStatus incomingStatus = resolveWebhookStatus(request);
        if (payment.getStatus() == incomingStatus
                && isTerminal(payment.getStatus())) {
            if (incomingStatus == PaymentStatus.PAID) {
                reconcileSuccessfulPayment(payment, booking, firstNonBlank(request.data().reference(), request.data().paymentLinkId()));
            }
            log.info("payment_webhook_duplicate orderCode={} status={}", payment.getOrderCode(), payment.getStatus());
            return;
        }

        if (payment.getStatus() == PaymentStatus.PAID) {
            reconcileSuccessfulPayment(payment, booking, firstNonBlank(request.data().reference(), request.data().paymentLinkId()));
            log.info("payment_webhook_ignored_already_paid orderCode={}", payment.getOrderCode());
            return;
        }

        if (booking.getBookingStatus() == BookingStatus.CANCELLED || booking.getBookingStatus() == BookingStatus.COMPLETED) {
            log.info("payment_webhook_ignored_booking_not_payable orderCode={} bookingStatus={}",
                    payment.getOrderCode(), booking.getBookingStatus());
            return;
        }

        payment.setTransactionId(firstNonBlank(request.data().reference(), request.data().paymentLinkId()));
        payment.setPayosPaymentLinkId(firstNonBlank(request.data().paymentLinkId(), payment.getPayosPaymentLinkId()));
        payment.setStatus(incomingStatus);

        if (incomingStatus == PaymentStatus.PAID) {
            reconcileSuccessfulPayment(payment, booking, payment.getTransactionId());
        } else if (incomingStatus == PaymentStatus.CANCELLED
                || incomingStatus == PaymentStatus.FAILED
                || incomingStatus == PaymentStatus.EXPIRED) {
            bookingStatusManager.cancelBooking(booking, incomingStatus);
            bookingRepository.save(booking);
            paymentRepository.save(payment);
        }
        log.info("payment_webhook_processed bookingId={} orderCode={} paymentStatus={} bookingStatus={}",
                booking.getBookingId(), payment.getOrderCode(), payment.getStatus(), booking.getBookingStatus());
    }

    @Transactional
    public PaymentReturnResponse handlePaymentReturn(Map<String, String> params) {
        log.info("[PaymentReturn] params={}", params);
        Payment payment = findPaymentFromParams(params);
        Booking booking = payment.getBooking();
        boolean successfulReturn = isSuccessfulReturn(params) || payment.getStatus() == PaymentStatus.PAID;
        boolean bookingUpdated = false;

        log.info("[PaymentReturn] orderCode={}", payment.getOrderCode());
        log.info("[PaymentReturn] status={}", successfulReturn ? PaymentStatus.PAID : payment.getStatus());

        if (successfulReturn) {
            bookingUpdated = reconcileSuccessfulPayment(payment, booking, params.get("id"));
        }

        log.info("[PaymentReturn] booking updated={}", bookingUpdated);

        return buildPaymentReturnResponse(
                payment,
                payment.getStatus() == PaymentStatus.PAID
                        && booking.getPaymentStatus() == PaymentStatus.PAID
                        && booking.getBookingStatus() == BookingStatus.CONFIRMED,
                false
        );
    }

    @Transactional
    public PaymentReturnResponse handlePaymentCancel(Map<String, String> params) {
        log.info("[PaymentCancel] params={}", params);
        Payment payment = findPaymentFromParams(params);
        Booking booking = payment.getBooking();

        if (payment.getStatus() != PaymentStatus.PAID && payment.getStatus() != PaymentStatus.CANCELLED) {
            payment.setTransactionId(firstNonBlank(params.get("id"), payment.getTransactionId()));
            payment.setStatus(PaymentStatus.CANCELLED);
            paymentRepository.save(payment);
        }

        return buildPaymentReturnResponse(payment, payment.getStatus() == PaymentStatus.PAID, true);
    }

    @Transactional
    public PaymentSyncResponse syncPaymentStatus(Long orderCode) {
        log.info("[PaymentSync] orderCode={}", orderCode);
        validateConfiguration();

        Payment payment = paymentRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
        Booking booking = payment.getBooking();
        validateCanView(booking);

        log.info("[PaymentSync] local before={}", formatLocalStatus(payment, booking));
        PayOSGetPaymentLinkResponse remoteResponse = payOSClient.getPaymentLinkInformation(orderCode);
        if (remoteResponse == null || remoteResponse.data() == null) {
            throw new IllegalStateException("PayOS did not return payment data");
        }

        String remoteStatus = normalize(remoteResponse.data().status());
        log.info("[PaymentSync] remoteStatus={}", remoteStatus);

        if (remoteResponse.data().amount() != null
                && payment.getAmount().compareTo(remoteResponse.data().amount()) != 0) {
            throw new IllegalArgumentException("PayOS amount does not match booking amount");
        }

        String resolvedPaymentLinkId = firstNonBlank(remoteResponse.data().id(), payment.getPayosPaymentLinkId());
        boolean paymentLinkUpdated = resolvedPaymentLinkId != null
                && !resolvedPaymentLinkId.equals(payment.getPayosPaymentLinkId());
        if (paymentLinkUpdated) {
            payment.setPayosPaymentLinkId(resolvedPaymentLinkId);
        }

        switch (remoteStatus) {
            case "PAID" -> {
                boolean changed = reconcileSuccessfulPayment(payment, booking, remoteResponse.data().id());
                if (!changed && paymentLinkUpdated) {
                    paymentRepository.save(payment);
                }
            }
            case "CANCELLED", "EXPIRED", "FAILED" -> applyRemoteTerminalStatus(payment, booking, remoteStatus);
            case "PENDING" -> {
                // Keep the local pending state until PayOS reports a terminal result.
                if (paymentLinkUpdated) {
                    paymentRepository.save(payment);
                }
            }
            default -> {
                log.info("[PaymentSync] remoteStatus unsupported={}", remoteStatus);
                if (paymentLinkUpdated) {
                    paymentRepository.save(payment);
                }
            }
        }

        log.info("[PaymentSync] local after={}", formatLocalStatus(payment, booking));
        return new PaymentSyncResponse(
                booking.getBookingId(),
                payment.getOrderCode(),
                payment.getStatus().name(),
                booking.getBookingStatus().name(),
                remoteStatus,
                payment.getAmount()
        );
    }

    @Transactional(readOnly = true)
    public PaymentStatusResponse getPaymentStatus(Long orderCode) {
        Payment payment = paymentRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
        Booking booking = payment.getBooking();
        validateCanView(booking);
        return new PaymentStatusResponse(
                booking.getBookingId(),
                payment.getStatus().name(),
                booking.getBookingStatus().name(),
                payment.getAmount()
        );
    }

    private void validatePayableBooking(Booking booking) {
        if (booking.getBookingStatus() == BookingStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot pay a cancelled booking");
        }
        if (booking.getBookingStatus() == BookingStatus.COMPLETED) {
            throw new IllegalArgumentException("Cannot pay a completed booking");
        }
        paymentRepository.findByBooking_BookingId(booking.getBookingId()).ifPresent(existingPayment -> {
            if (existingPayment.getStatus() == PaymentStatus.PAID) {
                throw new IllegalArgumentException("Booking has already been paid");
            }
        });
    }

    private void validateConfiguration() {
        requireConfigured(payOSProperties.getClientId(), "PAYOS client ID is not configured");
        requireConfigured(payOSProperties.getApiKey(), "PAYOS API key is not configured");
        requireConfigured(payOSProperties.getChecksumKey(), "PAYOS checksum key is not configured");
        requireConfigured(payOSProperties.getReturnUrl(), "PAYOS return URL is not configured");
        requireConfigured(payOSProperties.getCancelUrl(), "PAYOS cancel URL is not configured");
    }

    private void requireConfigured(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(message);
        }
    }

    private Long generateOrderCode(Long bookingId) {
        long candidate = System.currentTimeMillis() * 1000 + (bookingId % 1000);
        while (paymentRepository.existsByOrderCode(candidate)) {
            candidate++;
        }
        return candidate;
    }

    private String buildDescription(Booking booking) {
        return "CTBOOK" + booking.getBookingId();
    }

    private Payment findPaymentFromParams(Map<String, String> params) {
        String orderCodeValue = firstNonBlank(params.get("orderCode"), params.get("providerOrderCode"));
        if (orderCodeValue == null || orderCodeValue.isBlank()) {
            throw new IllegalArgumentException("Missing orderCode");
        }

        Long orderCode;
        try {
            orderCode = Long.valueOf(orderCodeValue.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid orderCode");
        }

        return paymentRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
    }

    private PaymentStatus resolveWebhookStatus(PayOSWebhookRequest request) {
        if (Boolean.TRUE.equals(request.success()) && "00".equals(request.code())) {
            return PaymentStatus.PAID;
        }

        String normalizedCode = request.code() == null ? "" : request.code().trim().toUpperCase();
        return switch (normalizedCode) {
            case "EXPIRED" -> PaymentStatus.EXPIRED;
            case "CANCELLED" -> PaymentStatus.CANCELLED;
            default -> PaymentStatus.FAILED;
        };
    }

    private boolean isTerminal(PaymentStatus status) {
        return status == PaymentStatus.PAID
                || status == PaymentStatus.CANCELLED
                || status == PaymentStatus.FAILED
                || status == PaymentStatus.EXPIRED;
    }

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        return fallback;
    }

    private boolean isSuccessfulReturn(Map<String, String> params) {
        if (isTruthy(params.get("cancel"))) {
            return false;
        }

        String status = normalize(params.get("status"));
        String code = normalize(params.get("code"));
        return "PAID".equals(status)
                || "SUCCESS".equals(status)
                || "SUCCEEDED".equals(status)
                || "00".equals(code);
    }

    private boolean isTruthy(String value) {
        String normalized = normalize(value);
        return "TRUE".equals(normalized) || "1".equals(normalized);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private void applyRemoteTerminalStatus(Payment payment, Booking booking, String remoteStatus) {
        if (payment.getStatus() == PaymentStatus.PAID) {
            reconcileSuccessfulPayment(payment, booking, firstNonBlank(payment.getTransactionId(), payment.getPayosPaymentLinkId()));
            return;
        }

        PaymentStatus resolvedStatus = switch (remoteStatus) {
            case "CANCELLED" -> PaymentStatus.CANCELLED;
            case "EXPIRED" -> PaymentStatus.EXPIRED;
            default -> PaymentStatus.FAILED;
        };

        payment.setStatus(resolvedStatus);
        if (booking.getBookingStatus() != BookingStatus.CANCELLED
                && booking.getBookingStatus() != BookingStatus.COMPLETED) {
            bookingStatusManager.cancelBooking(booking, resolvedStatus);
            bookingRepository.save(booking);
        }
        paymentRepository.save(payment);
    }

    private String formatLocalStatus(Payment payment, Booking booking) {
        return "payment=" + payment.getStatus()
                + ",bookingPayment=" + booking.getPaymentStatus()
                + ",booking=" + booking.getBookingStatus();
    }

    private boolean reconcileSuccessfulPayment(Payment payment, Booking booking, String transactionId) {
        PaymentStatus paymentStatusBefore = payment.getStatus();
        PaymentStatus bookingPaymentStatusBefore = booking.getPaymentStatus();
        BookingStatus bookingStatusBefore = booking.getBookingStatus();

        boolean changed = false;
        if (payment.getStatus() != PaymentStatus.PAID) {
            payment.setStatus(PaymentStatus.PAID);
            changed = true;
        }

        String resolvedTransactionId = firstNonBlank(transactionId, payment.getTransactionId());
        if (resolvedTransactionId != null && !resolvedTransactionId.equals(payment.getTransactionId())) {
            payment.setTransactionId(resolvedTransactionId);
            changed = true;
        }

        if (bookingStatusManager.synchronizePaidBooking(booking)) {
            changed = true;
        }

        log.info("[PaymentSuccess] orderCode={}", payment.getOrderCode());
        log.info("[PaymentSuccess] bookingId={}", booking.getBookingId());
        log.info("[PaymentSuccess] paymentStatus before={}", paymentStatusBefore);
        log.info("[PaymentSuccess] bookingStatus before={}", bookingStatusBefore);
        log.info("[PaymentSuccess] booking.paymentStatus before={}", bookingPaymentStatusBefore);
        log.info("[PaymentSuccess] paymentStatus after={}", payment.getStatus());
        log.info("[PaymentSuccess] bookingStatus after={}", booking.getBookingStatus());
        log.info("[PaymentSuccess] booking.paymentStatus after={}", booking.getPaymentStatus());

        if (changed) {
            bookingRepository.save(booking);
            paymentRepository.save(payment);
        }

        return changed;
    }

    private PaymentReturnResponse buildPaymentReturnResponse(Payment payment, boolean success, boolean cancelFlow) {
        Booking booking = payment.getBooking();
        return new PaymentReturnResponse(
                payment.getId(),
                booking.getBookingId(),
                payment.getStatus().name(),
                booking.getBookingStatus().name(),
                resolveReturnPaymentStatus(booking, cancelFlow),
                String.valueOf(payment.getOrderCode()),
                success
        );
    }

    private String resolveReturnPaymentStatus(Booking booking, boolean cancelFlow) {
        if (cancelFlow && booking.getPaymentStatus() != PaymentStatus.PAID) {
            return "UNPAID";
        }
        return booking.getPaymentStatus().name();
    }

    private void validateCanView(Booking booking) {
        User currentUser = userRepository.findById(currentUserService.getCurrentUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Current user not found"));

        if (hasRole("ROLE_ADMIN")) {
            return;
        }

        if (booking.getTrekker().getUserId().equals(currentUser.getUserId())) {
            return;
        }

        if (hasRole("ROLE_TOUR_PROVIDER")) {
            TourProvider provider = tourProviderRepository.findByUser_UserId(currentUser.getUserId()).orElse(null);
            if (provider != null && booking.getTour().getProvider().getProviderId().equals(provider.getProviderId())) {
                return;
            }
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to view this payment");
    }

    private boolean hasRole(String authority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> authority.equals(grantedAuthority.getAuthority()));
    }
}
