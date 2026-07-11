package com.example.trekkingapp.admin.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminBookingResponse(
        Long id,
        String user,
        String tour,
        String tourProvider,
        String schedule,
        LocalDateTime bookingDate,
        Integer participants,
        BigDecimal totalAmount,
        String paymentStatus,
        String bookingStatus,
        LocalDateTime createdAt,
        String notes,
        String refundStatus
) {
}
