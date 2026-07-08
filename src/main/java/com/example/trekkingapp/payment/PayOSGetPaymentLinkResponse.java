package com.example.trekkingapp.payment;

import java.math.BigDecimal;

public record PayOSGetPaymentLinkResponse(
        String code,
        String desc,
        PayOSGetPaymentLinkData data,
        String signature
) {
    public record PayOSGetPaymentLinkData(
            String id,
            Long orderCode,
            BigDecimal amount,
            BigDecimal amountPaid,
            BigDecimal amountRemaining,
            String status,
            String createdAt
    ) {
    }
}
