package com.example.trekkingapp.admin.dto.response;

import java.math.BigDecimal;

public record AdminRevenueChartItemResponse(
        String label,
        BigDecimal revenue,
        long bookings,
        long newUsers
) {
}
