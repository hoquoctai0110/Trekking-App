package com.example.trekkingapp.payment;

import java.util.List;

public record PayOSCreatePaymentRequest(
        Long orderCode,
        Integer amount,
        String description,
        String buyerName,
        String buyerEmail,
        String buyerPhone,
        String returnUrl,
        String cancelUrl,
        List<PayOSPaymentItem> items,
        String signature
) {
}
