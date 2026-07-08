package com.example.trekkingapp.payment;

public record CreatePaymentResult(
        Long orderCode,
        String checkoutUrl
) {
}
