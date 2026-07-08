package com.example.trekkingapp.booking;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BookingResponse(
        Long bookingId,
        Long tourId,
        Long scheduleId,
        String tourTitle,
        Long trekkerId,
        String trekkerName,
        String trekkerEmail,
        LocalDateTime scheduleStartDateTime,
        LocalDateTime scheduleEndDateTime,
        Integer numberOfPeople,
        BigDecimal totalPrice,
        String bookingStatus,
        String paymentStatus,
        LocalDateTime createdAt,
        LocalDateTime bookedAt,
        LocalDateTime updatedAt,
        Long orderCode,
        String checkoutUrl
) {
}
