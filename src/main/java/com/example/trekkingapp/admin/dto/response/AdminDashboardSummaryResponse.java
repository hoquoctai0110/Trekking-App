package com.example.trekkingapp.admin.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record AdminDashboardSummaryResponse(
        long totalUsers,
        long activeUsers,
        long totalTourProviders,
        long totalTours,
        long publishedTours,
        long pendingTours,
        long activeBookings,
        long completedBookings,
        BigDecimal monthlyRevenue,
        long pendingApprovals,
        long activeSosCount,
        List<TrendPoint> userTrend,
        List<TrendPoint> revenueTrend,
        List<TrendPoint> bookingTrend
) {
    public record TrendPoint(String label, Number value) {
    }
}
