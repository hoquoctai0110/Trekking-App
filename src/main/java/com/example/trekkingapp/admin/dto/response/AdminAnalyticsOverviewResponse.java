package com.example.trekkingapp.admin.dto.response;

import java.math.BigDecimal;

public record AdminAnalyticsOverviewResponse(
        BigDecimal totalRevenue,
        long totalBookings,
        long completedBookings,
        long cancelledBookings,
        long newUsers,
        long newTourProviders,
        long newTours,
        BigDecimal averageBookingValue,
        Double growthComparedToPreviousPeriod
) {
}
