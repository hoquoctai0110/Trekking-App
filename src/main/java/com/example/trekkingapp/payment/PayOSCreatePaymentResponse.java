package com.example.trekkingapp.payment;

public record PayOSCreatePaymentResponse(
        String code,
        String desc,
        PayOSCreatePaymentData data
) {
    public record PayOSCreatePaymentData(
            String paymentLinkId,
            String checkoutUrl
    ) {
    }
}
