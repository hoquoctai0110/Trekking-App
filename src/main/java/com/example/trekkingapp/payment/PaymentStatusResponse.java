package com.example.trekkingapp.payment;

import java.math.BigDecimal;

public record PaymentStatusResponse(
        Long bookingId,
        String paymentStatus,
        String bookingStatus,
        BigDecimal amount
) {
}
