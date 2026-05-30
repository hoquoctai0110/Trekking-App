package com.example.trekkingapp.booking;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BookingResponse(
        Long bookingId,
        Long tourId,
        String tourTitle,
        Long trekkerId,
        String trekkerEmail,
        Integer numberOfPeople,
        BigDecimal totalPrice,
        String bookingStatus,
        String paymentStatus,
        LocalDateTime bookedAt,
        LocalDateTime updatedAt
) {
}
