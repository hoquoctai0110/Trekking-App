package com.example.trekkingapp.admin.dto.response;

public record AdminBookingStatusSummaryResponse(
        long pending,
        long confirmed,
        long inProgress,
        long completed,
        long cancelled
) {
}
