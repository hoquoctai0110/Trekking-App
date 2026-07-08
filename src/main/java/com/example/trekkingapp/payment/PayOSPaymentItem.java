package com.example.trekkingapp.payment;

public record PayOSPaymentItem(
        String name,
        Integer quantity,
        Integer price
) {
}
