package com.example.trekkingapp.payment;

import java.math.BigDecimal;

public record PaymentSyncResponse(
        Long bookingId,
        Long orderCode,
        String localPaymentStatus,
        String localBookingStatus,
        String remoteStatus,
        BigDecimal amount
) {
}
