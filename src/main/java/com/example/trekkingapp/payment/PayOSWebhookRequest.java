package com.example.trekkingapp.payment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PayOSWebhookRequest(
        String code,
        String desc,
        Boolean success,
        @NotBlank(message = "signature is required")
        String signature,
        @NotNull(message = "data is required")
        @Valid
        PayOSWebhookData data
) {
    public record PayOSWebhookData(
            @NotNull(message = "orderCode is required")
            Long orderCode,
            @NotNull(message = "amount is required")
            BigDecimal amount,
            String description,
            String paymentLinkId,
            String reference,
            String transactionDateTime,
            String currency,
            String code,
            String desc,
            String counterAccountBankId,
            String counterAccountBankName,
            String counterAccountName,
            String counterAccountNumber,
            String virtualAccountName,
            String virtualAccountNumber
    ) {
    }
}
