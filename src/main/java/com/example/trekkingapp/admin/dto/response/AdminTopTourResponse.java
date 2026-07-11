package com.example.trekkingapp.admin.dto.response;

import java.math.BigDecimal;

public record AdminTopTourResponse(
        Long tourId,
        String name,
        long bookingCount,
        Double rating,
        BigDecimal revenue
) {
}
