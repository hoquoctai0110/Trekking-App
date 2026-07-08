package com.example.trekkingapp.payment;

public record PaymentReturnResponse(
        Long paymentId,
        Long bookingId,
        String status,
        String bookingStatus,
        String paymentStatus,
        String orderCode,
        boolean success
) {
}
